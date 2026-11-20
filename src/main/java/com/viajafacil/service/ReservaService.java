package com.viajafacil.service;

import com.viajafacil.exception.RegraNegocioException;
import com.viajafacil.model.*;
import com.viajafacil.util.Dinheiro;
import com.viajafacil.util.Relogio;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static com.viajafacil.exception.RegraNegocioException.exigir;

/** Orçamento, reserva (com sinal de 30%), cancelamento e retirada. */
public class ReservaService {
    public static final double PERCENTUAL_SINAL = 0.30;

    private final BancoDados bd;
    private final FrotaService frota;
    private final Relogio relogio;

    public ReservaService(BancoDados bd, FrotaService frota, Relogio relogio) {
        this.bd = bd;
        this.frota = frota;
        this.relogio = relogio;
    }

    private record Plano(Cliente cliente, Veiculo veiculo, int dias, int diasGratis, double total, double sinal) { }

    private Plano planejar(String cpf, Categoria categoria, LocalDate inicio, LocalDate fim, boolean usarPontos) {
        Cliente cliente = bd.clienteAtivoObrigatorio(cpf);
        exigir(categoria != null, "Selecione a categoria.");
        exigir(inicio != null && fim != null && fim.isAfter(inicio), "A devolução deve ocorrer depois da retirada.");
        exigir(!inicio.isBefore(relogio.hoje()), "A reserva não pode começar no passado.");
        int dias = (int) ChronoUnit.DAYS.between(inicio, fim);
        exigir(dias <= 3650, "O período máximo é de 10 anos.");
        if (usarPontos) {
            exigir(categoria == Categoria.ECONOMICO, "Pontos só podem ser usados na categoria Econômico.");
            exigir(cliente.getPontos() >= Cliente.PONTOS_DIARIA_GRATIS,
                    "São necessários " + Cliente.PONTOS_DIARIA_GRATIS + " pontos para uma diária grátis.");
        }

        List<Veiculo> daCategoria = bd.veiculos().stream().filter(v -> !v.isRemovido() && v.getCategoria() == categoria).toList();
        List<Veiculo> permitidos = daCategoria.stream().filter(v -> v.aceitaCliente(cliente)).toList();
        if (!daCategoria.isEmpty() && permitidos.isEmpty()) throw new RegraNegocioException(daCategoria.get(0).getRestricao());
        Veiculo veiculo = permitidos.stream().filter(v -> frota.disponivel(v, inicio, fim)).findFirst()
                .orElseThrow(() -> new RegraNegocioException("Nenhum veículo desta categoria está disponível no período."));

        int diasGratis = usarPontos ? 1 : 0;
        double total = Dinheiro.arredondar((dias - diasGratis) * veiculo.getDiaria() * (1 - cliente.getDesconto()));
        return new Plano(cliente, veiculo, dias, diasGratis, total, Dinheiro.arredondar(total * PERCENTUAL_SINAL));
    }

    public Orcamento orcamento(String cpf, Categoria categoria, LocalDate inicio, LocalDate fim, boolean usarPontos) {
        Plano p = planejar(cpf, categoria, inicio, fim, usarPontos);
        return new Orcamento(p.veiculo().getPlaca(), p.veiculo().getModelo(), categoria, p.dias(), p.diasGratis(),
                p.veiculo().getDiaria(), p.cliente().getDesconto(), p.total(), p.sinal(), p.cliente().getPontos());
    }

    /** Lista a frota que atende ao cliente e ao período selecionado, sem criar uma reserva. */
    public List<Veiculo> veiculosDisponiveis(String cpf, Categoria categoria, LocalDate inicio, LocalDate fim) {
        Cliente cliente = bd.clienteAtivoObrigatorio(cpf);
        exigir(categoria != null, "Selecione a categoria.");
        exigir(inicio != null && fim != null && fim.isAfter(inicio), "A devolução deve ocorrer depois da retirada.");
        exigir(!inicio.isBefore(relogio.hoje()), "A reserva não pode começar no passado.");
        exigir(ChronoUnit.DAYS.between(inicio, fim) <= 3650, "O período máximo é de 10 anos.");
        List<Veiculo> daCategoria = bd.veiculos().stream()
                .filter(v -> !v.isRemovido() && v.getCategoria() == categoria).toList();
        List<Veiculo> permitidos = daCategoria.stream().filter(v -> v.aceitaCliente(cliente)).toList();
        if (!daCategoria.isEmpty() && permitidos.isEmpty()) throw new RegraNegocioException(daCategoria.get(0).getRestricao());
        return permitidos.stream().filter(v -> frota.disponivel(v, inicio, fim)).toList();
    }

    /** Criar a reserva equivale a receber o sinal de 30%, que confirma a reserva. */
    public Reserva reservar(String cpf, Categoria categoria, LocalDate inicio, LocalDate fim, boolean usarPontos) {
        Plano p = planejar(cpf, categoria, inicio, fim, usarPontos);
        if (usarPontos) p.cliente().resgatarDiariaGratis();
        Reserva reserva = new Reserva(bd.proximoIdReserva(), p.cliente().getCpf(), p.veiculo().getPlaca(),
                inicio, fim, usarPontos, p.total(), p.sinal());
        bd.reservas().add(reserva);
        bd.pagamentos().add(new Pagamento(reserva.getId(), TipoPagamento.SINAL, p.sinal(), relogio.hoje()));
        frota.atualizarStatus(p.veiculo());
        bd.salvar();
        return reserva;
    }

    /** Cancelar antes da retirada devolve o sinal e os pontos (política escolhida pelo projeto). */
    public void cancelar(int reservaId) {
        Reserva r = bd.reservaObrigatoria(reservaId);
        r.cancelar();
        if (r.isUsouPontos()) bd.clienteObrigatorio(r.getClienteCpf()).devolverDiariaGratis();
        bd.pagamentos().add(new Pagamento(r.getId(), TipoPagamento.REEMBOLSO, r.getValorSinal(), relogio.hoje()));
        frota.atualizarStatus(bd.veiculoObrigatorio(r.getVeiculoPlaca()));
        bd.salvar();
    }

    public Locacao retirar(int reservaId, LocalDate data) {
        Reserva r = bd.reservaObrigatoria(reservaId);
        exigir(r.getStatus() == StatusReserva.CONFIRMADA, "Selecione uma reserva confirmada.");
        LocalDate dia = data != null ? data : relogio.hoje();
        exigir(!dia.isBefore(r.getInicio()) && dia.isBefore(r.getFim()),
                "A retirada só pode ocorrer dentro do período reservado. A multa por atraso é calculada na devolução de um veículo já retirado.");
        Veiculo v = bd.veiculoObrigatorio(r.getVeiculoPlaca());
        exigir(v.getStatus() != StatusVeiculo.MANUTENCAO && !v.precisaRevisao(), "O veículo está bloqueado para manutenção.");
        exigir(bd.locacaoAbertaDoVeiculo(v.getPlaca()).isEmpty(), "Este veículo ainda não foi devolvido pelo outro cliente.");
        Locacao locacao = new Locacao(r.getId(), dia, v.getKmAtual());
        bd.locacoes().add(locacao);
        r.iniciarLocacao();
        frota.atualizarStatus(v);
        bd.salvar();
        return locacao;
    }
}
