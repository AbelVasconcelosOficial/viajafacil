package com.viajafacil;

import com.viajafacil.exception.RegraNegocioException;
import com.viajafacil.model.*;
import com.viajafacil.service.ResumoDevolucao;
import org.junit.Before;
import org.junit.Test;

import java.time.LocalDate;

import static org.junit.Assert.*;

public class RegrasDeNegocioTest {
    private Cenario c;
    private final LocalDate hoje = Cenario.HOJE;

    @Before
    public void preparar() throws Exception { c = new Cenario(); }

    private void rejeita(Runnable operacao) {
        try { operacao.run(); } catch (RegraNegocioException e) { return; }
        fail("A operação inválida foi aceita");
    }

    @Test
    public void categoriasTemDiariaDoEnunciado() {
        assertEquals(120, new VeiculoEconomico("AAA1111", "x", 0, 0).getDiaria(), 0.001);
        assertEquals(220, new VeiculoSUV("AAA1111", "x", 0, 0).getDiaria(), 0.001);
        assertEquals(350, new VeiculoLuxo("AAA1111", "x", 0, 0).getDiaria(), 0.001);
    }

    @Test
    public void cadastroValidaEntradas() {
        c.cadastro.cadastrarCliente("Ana", "111.111.111-11", TipoCliente.REGULAR);
        rejeita(() -> c.cadastro.cadastrarCliente("Outra", "11111111111", TipoCliente.REGULAR));
        rejeita(() -> c.cadastro.cadastrarCliente("", "22222222222", TipoCliente.REGULAR));
        rejeita(() -> c.cadastro.cadastrarCliente("Sem CPF", "123", TipoCliente.REGULAR));
        c.cadastro.cadastrarVeiculo("ABC-1234", "Argo", Categoria.ECONOMICO, 0, 0);
        rejeita(() -> c.cadastro.cadastrarVeiculo("ABC1234", "Outro", Categoria.SUV, 0, 0));
        rejeita(() -> c.cadastro.cadastrarVeiculo("AAA1234", "Outro", Categoria.SUV, Double.NaN, 0));
        rejeita(() -> c.cadastro.cadastrarVeiculo("AAA1234", "Outro", Categoria.SUV, 100, 500));
        rejeita(() -> c.cadastro.cadastrarVeiculo("123", "Outro", Categoria.SUV, 0, 0));
    }

    @Test
    public void veiculoCadastradoComDezMilKmDesdeRevisaoJaNasceEmManutencao() {
        Veiculo v = c.cadastro.cadastrarVeiculo("ABC1234", "Argo", Categoria.ECONOMICO, 10000, 0);
        assertEquals(StatusVeiculo.MANUTENCAO, v.getStatus());
    }

    @Test
    public void luxoEhExclusivoParaVipEVipTemQuinzePorCentro() {
        c.cadastro.cadastrarCliente("Ana", "11111111111", TipoCliente.REGULAR);
        c.cadastro.cadastrarCliente("Bia", "22222222222", TipoCliente.VIP);
        c.cadastro.cadastrarVeiculo("DEF1G23", "Sedã", Categoria.LUXO, 0, 0);
        rejeita(() -> c.reservas.orcamento("11111111111", Categoria.LUXO, hoje, hoje.plusDays(1), false));
        Reserva r = c.reservas.reservar("22222222222", Categoria.LUXO, hoje, hoje.plusDays(2), false);
        assertEquals(595, r.getValorTotal(), 0.001);   // 2 x 350 x 0,85
        assertEquals(178.5, r.getValorSinal(), 0.001); // 30%
        assertEquals(178.5, c.relatorios.receitas(), 0.001);
    }

    @Test
    public void datasInvalidasSaoRejeitadas() {
        c.cadastro.cadastrarCliente("Bia", "22222222222", TipoCliente.VIP);
        c.cadastro.cadastrarVeiculo("DEF1G23", "Sedã", Categoria.LUXO, 0, 0);
        rejeita(() -> c.reservas.orcamento("22222222222", Categoria.LUXO, hoje, hoje, false));
        rejeita(() -> c.reservas.orcamento("22222222222", Categoria.LUXO, hoje.minusDays(1), hoje.plusDays(1), false));
        rejeita(() -> c.reservas.orcamento("22222222222", Categoria.LUXO, null, hoje.plusDays(1), false));
        rejeita(() -> c.reservas.orcamento("99999999999", Categoria.LUXO, hoje, hoje.plusDays(1), false));
    }

    @Test
    public void cancelarDevolveSinalEPontos() {
        Cliente cli = c.cadastro.cadastrarCliente("Ana", "11111111111", TipoCliente.REGULAR);
        c.cadastro.cadastrarVeiculo("ABC1234", "Argo", Categoria.ECONOMICO, 0, 0);
        cli.adicionarPontos(200);
        Reserva r = c.reservas.reservar(cli.getCpf(), Categoria.ECONOMICO, hoje, hoje.plusDays(1), true);
        assertEquals(0, r.getValorTotal(), 0.001);
        assertEquals(0, cli.getPontos());
        c.reservas.cancelar(r.getId());
        assertEquals(200, cli.getPontos());
        assertEquals(0, c.relatorios.receitas(), 0.001);
        rejeita(() -> c.reservas.cancelar(r.getId()));
    }

    @Test
    public void reservasNaoSePodemSobreporMasPodemSerSeguidas() {
        c.cadastro.cadastrarCliente("Ana", "11111111111", TipoCliente.REGULAR);
        Veiculo eco = c.cadastro.cadastrarVeiculo("ABC1234", "Argo", Categoria.ECONOMICO, 0, 0);
        c.reservas.reservar("11111111111", Categoria.ECONOMICO, hoje, hoje.plusDays(2), false);
        rejeita(() -> c.reservas.reservar("11111111111", Categoria.ECONOMICO, hoje.plusDays(1), hoje.plusDays(3), false));
        Reserva seguinte = c.reservas.reservar("11111111111", Categoria.ECONOMICO, hoje.plusDays(2), hoje.plusDays(4), false);
        assertEquals(eco.getPlaca(), seguinte.getVeiculoPlaca());
        rejeita(() -> c.reservas.retirar(seguinte.getId(), hoje)); // antes do período
    }

    @Test
    public void fluxoCompletoComManutencaoPorQuilometragemEPontos() {
        Cliente ana = c.cadastro.cadastrarCliente("Ana", "11111111111", TipoCliente.REGULAR);
        Veiculo eco = c.cadastro.cadastrarVeiculo("ABC1234", "Argo", Categoria.ECONOMICO, 9000, 0);
        Reserva r = c.reservas.reservar(ana.getCpf(), Categoria.ECONOMICO, hoje, hoje.plusDays(2), false);
        assertEquals(240, r.getValorTotal(), 0.001);
        assertEquals(72, r.getValorSinal(), 0.001);
        assertEquals(StatusVeiculo.RESERVADO, eco.getStatus());

        c.reservas.retirar(r.getId(), hoje);
        assertEquals(StatusVeiculo.ALUGADO, eco.getStatus());
        rejeita(() -> c.reservas.retirar(r.getId(), hoje));
        rejeita(() -> c.reservas.cancelar(r.getId()));
        rejeita(() -> c.devolucoes.simular(r.getId(), hoje, 8000));              // km menor que o inicial
        rejeita(() -> c.devolucoes.simular(r.getId(), hoje.plusDays(1), 9000));  // data no futuro

        ResumoDevolucao resumo = c.devolucoes.devolver(r.getId(), hoje, 10000);
        assertEquals(168, resumo.saldo(), 0.001);
        assertEquals(20, ana.getPontos());                       // 2 diárias x 10 pontos
        assertEquals(StatusVeiculo.MANUTENCAO, eco.getStatus()); // chegou a 10.000 km
        assertEquals(240, c.relatorios.receitas(), 0.001);
        rejeita(() -> c.devolucoes.devolver(r.getId(), hoje, 10000));

        rejeita(() -> c.reservas.reservar(ana.getCpf(), Categoria.ECONOMICO, hoje, hoje.plusDays(1), false)); // bloqueado
        c.frota.concluirManutencao(eco.getPlaca(), 300);
        assertEquals(StatusVeiculo.DISPONIVEL, eco.getStatus());
        assertEquals(300, c.relatorios.custosManutencao(), 0.001);
        assertEquals(-60, c.relatorios.gerarResumo().resultado(), 0.001);
    }

    @Test
    public void atrasoCobraDiariaMais20PorCentoPorDia() {
        Cliente ana = c.cadastro.cadastrarCliente("Ana", "11111111111", TipoCliente.REGULAR);
        Veiculo eco = c.cadastro.cadastrarVeiculo("ABC1234", "Argo", Categoria.ECONOMICO, 0, 0);
        Reserva r = c.reservas.reservar(ana.getCpf(), Categoria.ECONOMICO, hoje, hoje.plusDays(2), false);
        c.reservas.retirar(r.getId(), hoje);

        // Avança o relógio 4 dias: o carro deveria ter voltado há 2 dias.
        c.relogio.definir(hoje.plusDays(4));
        assertFalse(c.frota.disponivel(eco, hoje.plusDays(4), hoje.plusDays(5))); // atrasado não pode ser reservado
        assertTrue(c.relatorios.gerarResumo().alugados().get(0).emAtraso());

        ResumoDevolucao resumo = c.devolucoes.simular(r.getId(), null, 100);
        assertEquals(2, resumo.diasAtraso());
        assertEquals(288, resumo.taxaAtraso(), 0.001);  // 2 x 120 x 1,2
        assertEquals(456, resumo.saldo(), 0.001);       // 240 + 288 - 72
        c.devolucoes.devolver(r.getId(), null, 100);
        assertEquals(40, ana.getPontos());              // (2 + 2) diárias pagas x 10
        assertEquals(528, c.relatorios.receitas(), 0.001);
    }

    @Test
    public void pontosSoValemParaEconomicoComSaldoSuficiente() {
        Cliente ana = c.cadastro.cadastrarCliente("Ana", "11111111111", TipoCliente.REGULAR);
        c.cadastro.cadastrarVeiculo("ABC1234", "Argo", Categoria.ECONOMICO, 0, 0);
        c.cadastro.cadastrarVeiculo("SUV1A23", "Compass", Categoria.SUV, 0, 0);
        rejeita(() -> c.reservas.reservar(ana.getCpf(), Categoria.ECONOMICO, hoje, hoje.plusDays(2), true)); // 0 pontos
        ana.adicionarPontos(200);
        rejeita(() -> c.reservas.reservar(ana.getCpf(), Categoria.SUV, hoje, hoje.plusDays(2), true));
        assertEquals(200, ana.getPontos());
        Reserva r = c.reservas.reservar(ana.getCpf(), Categoria.ECONOMICO, hoje, hoje.plusDays(2), true);
        assertEquals(120, r.getValorTotal(), 0.001); // 1 diária paga, 1 grátis
        c.reservas.retirar(r.getId(), hoje);
        c.devolucoes.devolver(r.getId(), hoje, 10);
        assertEquals(10, ana.getPontos());           // só a diária paga gera pontos
    }

    @Test
    public void editarClienteTrocaDeSubclasseMantendoPontos() {
        Cliente ana = c.cadastro.cadastrarCliente("Ana", "11111111111", TipoCliente.REGULAR);
        ana.adicionarPontos(50);
        Cliente vip = c.cadastro.editarCliente("11111111111", "Ana Souza", TipoCliente.VIP);
        assertTrue(vip instanceof ClienteVIP);
        assertEquals(50, vip.getPontos());
        assertEquals(0.15, vip.getDesconto(), 0.0001);
        assertEquals(1, c.bd.clientes().size());
    }

    @Test
    public void naoRemoveClienteOuVeiculoComReservas() {
        c.cadastro.cadastrarCliente("Ana", "11111111111", TipoCliente.REGULAR);
        c.cadastro.cadastrarVeiculo("ABC1234", "Argo", Categoria.ECONOMICO, 0, 0);
        c.reservas.reservar("11111111111", Categoria.ECONOMICO, hoje, hoje.plusDays(1), false);
        rejeita(() -> c.cadastro.removerCliente("11111111111"));
        rejeita(() -> c.cadastro.removerVeiculo("ABC1234"));
        c.cadastro.cadastrarCliente("Bia", "22222222222", TipoCliente.REGULAR);
        c.cadastro.removerCliente("22222222222");
        assertEquals(1, c.bd.clientes().size());
    }
}
