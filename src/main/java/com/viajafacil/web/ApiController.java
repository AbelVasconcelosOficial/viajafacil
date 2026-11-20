package com.viajafacil.web;

import com.viajafacil.exception.RegraNegocioException;
import com.viajafacil.model.*;
import com.viajafacil.relatorio.GeradorRelatorio;
import com.viajafacil.relatorio.RelatorioPdf;
import com.viajafacil.relatorio.RelatorioHtml;
import com.viajafacil.relatorio.RelatorioTxt;
import com.viajafacil.service.*;
import com.viajafacil.util.Relogio;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** Liga as rotas HTTP aos serviços. Não tem regra de negócio: só converte entrada e saída. */
public class ApiController {
    // ---- corpos de requisição ----
    public record VeiculoReq(String placa, String modelo, Categoria categoria, Double kmAtual, Double kmUltimaRevisao,
                             String marca, String anoModelo, String valorFipe, String referenciaFipe) { }
    public record ModeloReq(String modelo) { }
    public record ClienteReq(String nome, String cpf, TipoCliente tipo, Endereco endereco) { }
    public record ReservaReq(String cpf, Categoria categoria, LocalDate inicio, LocalDate fim, Boolean usarPontos) { }
    public record DataKmReq(LocalDate data, Double km) { }
    public record DataReq(LocalDate data) { }
    public record CustoReq(Double custo) { }

    // ---- visões de resposta ----
    public record VeiculoView(String placa, String modelo, Categoria categoria, String categoriaRotulo, double diaria,
                              double kmAtual, double kmUltimaRevisao, StatusVeiculo status, String marca,
                              String anoModelo, String valorFipe, String referenciaFipe) { }
    public record ClienteView(String nome, String cpf, TipoCliente tipo, int pontos, Endereco endereco) { }
    public record ReservaView(int id, String cpf, String cliente, String placa, String modelo, Categoria categoria,
                              LocalDate inicio, LocalDate fim, StatusReserva status, double total, double sinal,
                              boolean usouPontos, int diasAtraso, double taxaAtrasoAcumulada) { }
    public record VeiculoDisponivelView(String placa, String modelo, String categoriaRotulo, double diaria) { }

    private final BancoDados bd;
    private final Relogio relogio;
    private final CadastroService cadastro;
    private final FrotaService frota;
    private final ReservaService reservas;
    private final DevolucaoService devolucoes;
    private final RelatorioService relatorios;
    private final ViaCepService viaCep = new ViaCepService();
    private final BrasilApiFipeService fipe = new BrasilApiFipeService();

    public ApiController(BancoDados bd, Relogio relogio, CadastroService cadastro, FrotaService frota,
                         ReservaService reservas, DevolucaoService devolucoes, RelatorioService relatorios) {
        this.bd = bd;
        this.relogio = relogio;
        this.cadastro = cadastro;
        this.frota = frota;
        this.reservas = reservas;
        this.devolucoes = devolucoes;
        this.relatorios = relatorios;
    }

    private static double numero(Double valor, String campo) {
        if (valor == null) throw new RegraNegocioException("Informe " + campo + ".");
        return valor;
    }

    private VeiculoView view(Veiculo v) {
        return new VeiculoView(v.getPlaca(), v.getModelo(), v.getCategoria(), v.getCategoria().getRotulo(), v.getDiaria(),
                v.getKmAtual(), v.getKmUltimaRevisao(), v.getStatus(), v.getMarca(), v.getAnoModelo(), v.getValorFipe(), v.getReferenciaFipe());
    }

    private ClienteView view(Cliente c) { return new ClienteView(c.getNome(), c.getCpf(), c.getTipo(), c.getPontos(), c.getEndereco()); }

    private ReservaView view(Reserva r) {
        Cliente c = bd.clienteObrigatorio(r.getClienteCpf());
        Veiculo v = bd.veiculoObrigatorio(r.getVeiculoPlaca());
        DevolucaoService.AtrasoAtual atraso = devolucoes.atrasoAtual(r.getId());
        return new ReservaView(r.getId(), c.getCpf(), c.getNome(), v.getPlaca(), v.getModelo(), v.getCategoria(),
                r.getInicio(), r.getFim(), r.getStatus(), r.getValorTotal(), r.getValorSinal(), r.isUsouPontos(),
                atraso.dias(), atraso.taxa());
    }

    private Resposta relatorio(GeradorRelatorio gerador) throws Exception {
        ByteArrayOutputStream saida = new ByteArrayOutputStream();
        gerador.gerar(relatorios.gerarResumo(), saida);
        return new Resposta(200, gerador.contentType(), saida.toByteArray());
    }

    public void registrar(Servidor s) {
        s.rota("POST", "/api/dados/resetar", r -> {
            bd.limparTudo();
            relogio.definir(null);
            return Resposta.json(Map.of("ok", true));
        });
        s.rota("GET", "/api/estado", r -> Resposta.json(Map.of("hoje", relogio.hoje(), "simulado", relogio.simulado())));
        s.rota("POST", "/api/relogio", r -> {
            relogio.definir(r.corpo(DataReq.class).data());
            return Resposta.json(Map.of("hoje", relogio.hoje(), "simulado", relogio.simulado()));
        });

        // veículos
        s.rota("GET", "/api/fipe/marcas", r -> Resposta.json(fipe.marcas()));
        s.rota("GET", "/api/fipe/modelos/([0-9]+)", r -> Resposta.json(fipe.modelos(r.param(0))));
        s.rota("GET", "/api/fipe/anos/([0-9]+)/([0-9]+)", r -> Resposta.json(fipe.anos(r.param(0), r.param(1))));
        s.rota("GET", "/api/fipe/preco/([0-9]+)/([0-9]+)/([0-9-]+)", r -> Resposta.json(fipe.detalhe(r.param(0), r.param(1), r.param(2))));
        s.rota("GET", "/api/veiculos", r -> Resposta.json(bd.veiculos().stream().filter(v -> !v.isRemovido()).map(this::view).toList()));
        s.rota("GET", "/api/arquivados/veiculos", r -> Resposta.json(bd.veiculos().stream().filter(Veiculo::isRemovido).map(this::view).toList()));
        s.rota("POST", "/api/veiculos", r -> {
            VeiculoReq b = r.corpo(VeiculoReq.class);
            return Resposta.json(201, view(cadastro.cadastrarVeiculo(b.placa(), b.modelo(), b.categoria(),
                    numero(b.kmAtual(), "a quilometragem atual"), numero(b.kmUltimaRevisao(), "a quilometragem da última revisão"),
                    b.marca(), b.anoModelo(), b.valorFipe(), b.referenciaFipe())));
        });
        s.rota("PUT", "/api/veiculos/([^/]+)", r -> Resposta.json(view(cadastro.editarVeiculo(r.param(0), r.corpo(ModeloReq.class).modelo()))));
        s.rota("DELETE", "/api/veiculos/([^/]+)", r -> { cadastro.removerVeiculo(r.param(0)); return Resposta.json(Map.of("ok", true)); });
        s.rota("POST", "/api/veiculos/([^/]+)/reativar", r -> Resposta.json(view(cadastro.reativarVeiculo(r.param(0)))));
        s.rota("POST", "/api/veiculos/([^/]+)/manutencao/iniciar", r -> {
            frota.iniciarManutencao(r.param(0));
            return Resposta.json(view(bd.veiculoObrigatorio(r.param(0))));
        });
        s.rota("POST", "/api/veiculos/([^/]+)/manutencao/concluir", r -> {
            frota.concluirManutencao(r.param(0), numero(r.corpo(CustoReq.class).custo(), "o custo da manutenção"));
            return Resposta.json(view(bd.veiculoObrigatorio(r.param(0))));
        });

        // clientes
        s.rota("GET", "/api/cep/([^/]+)", r -> Resposta.json(viaCep.buscar(r.param(0))));
        s.rota("GET", "/api/clientes", r -> Resposta.json(bd.clientes().stream().filter(c -> !c.isRemovido()).map(this::view).toList()));
        s.rota("GET", "/api/arquivados/clientes", r -> Resposta.json(bd.clientes().stream().filter(Cliente::isRemovido).map(this::view).toList()));
        s.rota("POST", "/api/clientes", r -> {
            ClienteReq b = r.corpo(ClienteReq.class);
            return Resposta.json(201, view(cadastro.cadastrarCliente(b.nome(), b.cpf(), b.tipo(), b.endereco())));
        });
        s.rota("PUT", "/api/clientes/([^/]+)", r -> {
            ClienteReq b = r.corpo(ClienteReq.class);
            Cliente atualizado = cadastro.editarCliente(r.param(0), b.nome(), b.tipo());
            if (b.endereco() != null) { atualizado.setEndereco(b.endereco()); bd.salvar(); }
            return Resposta.json(view(atualizado));
        });
        s.rota("DELETE", "/api/clientes/([^/]+)", r -> { cadastro.removerCliente(r.param(0)); return Resposta.json(Map.of("ok", true)); });
        s.rota("POST", "/api/clientes/([^/]+)/reativar", r -> Resposta.json(view(cadastro.reativarCliente(r.param(0)))));

        // reservas
        s.rota("GET", "/api/reservas", r -> {
            List<ReservaView> lista = bd.reservas().stream().map(this::view).toList();
            return Resposta.json(lista);
        });
        s.rota("POST", "/api/reservas/orcamento", r -> {
            ReservaReq b = r.corpo(ReservaReq.class);
            return Resposta.json(reservas.orcamento(b.cpf(), b.categoria(), b.inicio(), b.fim(), Boolean.TRUE.equals(b.usarPontos())));
        });
        s.rota("POST", "/api/reservas/disponiveis", r -> {
            ReservaReq b = r.corpo(ReservaReq.class);
            return Resposta.json(reservas.veiculosDisponiveis(b.cpf(), b.categoria(), b.inicio(), b.fim()).stream()
                    .map(v -> new VeiculoDisponivelView(v.getPlaca(), v.getModelo(), v.getCategoria().getRotulo(), v.getDiaria())).toList());
        });
        s.rota("POST", "/api/reservas", r -> {
            ReservaReq b = r.corpo(ReservaReq.class);
            return Resposta.json(201, view(reservas.reservar(b.cpf(), b.categoria(), b.inicio(), b.fim(), Boolean.TRUE.equals(b.usarPontos()))));
        });
        s.rota("POST", "/api/reservas/(\\d+)/cancelar", r -> {
            reservas.cancelar(r.paramInt(0));
            return Resposta.json(view(bd.reservaObrigatoria(r.paramInt(0))));
        });
        s.rota("POST", "/api/reservas/(\\d+)/retirar", r -> {
            reservas.retirar(r.paramInt(0), r.corpo(DataReq.class).data());
            return Resposta.json(view(bd.reservaObrigatoria(r.paramInt(0))));
        });
        s.rota("POST", "/api/reservas/(\\d+)/devolucao/simular", r -> {
            DataKmReq b = r.corpo(DataKmReq.class);
            return Resposta.json(devolucoes.simular(r.paramInt(0), b.data(), numero(b.km(), "a quilometragem final")));
        });
        s.rota("POST", "/api/reservas/(\\d+)/devolver", r -> {
            DataKmReq b = r.corpo(DataKmReq.class);
            return Resposta.json(devolucoes.devolver(r.paramInt(0), b.data(), numero(b.km(), "a quilometragem final")));
        });

        // relatórios
        s.rota("GET", "/api/relatorio", r -> Resposta.json(relatorios.gerarResumo()));
        s.rota("GET", "/api/relatorio/pdf", r -> relatorio(new RelatorioPdf()));
        s.rota("GET", "/api/relatorio/html", r -> relatorio(new RelatorioHtml()));
        s.rota("GET", "/api/relatorio/txt", r -> relatorio(new RelatorioTxt()));
    }
}
