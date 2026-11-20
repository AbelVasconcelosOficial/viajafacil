package com.viajafacil;

import com.viajafacil.model.*;
import com.viajafacil.service.BancoDados;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.*;

public class PersistenciaTest {
    private final java.time.LocalDate hoje = Cenario.HOJE;

    @Test
    public void pastaVaziaOuArquivosAusentesNaoQuebram() throws Exception {
        Path pasta = Files.createTempDirectory("viajafacil-vazio-").resolve("naoExisteAinda");
        BancoDados bd = new BancoDados(pasta);
        assertTrue(bd.veiculos().isEmpty());
        assertTrue(bd.clientes().isEmpty());
    }

    @Test
    public void salvaERecarregaPreservandoAsSubclassesEOsRelacionamentos() throws Exception {
        Cenario a = new Cenario();
        a.cadastro.cadastrarCliente("Ana", "11111111111", TipoCliente.REGULAR);
        a.cadastro.cadastrarCliente("Bia", "22222222222", TipoCliente.VIP);
        a.cadastro.cadastrarVeiculo("ABC1234", "Argo", Categoria.ECONOMICO, 100, 0);
        a.cadastro.cadastrarVeiculo("SUV1A23", "Compass", Categoria.SUV, 0, 0);
        a.cadastro.cadastrarVeiculo("DEF1G23", "Sedã", Categoria.LUXO, 0, 0);
        Reserva r = a.reservas.reservar("22222222222", Categoria.LUXO, hoje, hoje.plusDays(2), false);
        a.reservas.retirar(r.getId(), hoje);
        a.devolucoes.devolver(r.getId(), hoje, 500);
        a.frota.iniciarManutencao("ABC1234");
        a.frota.concluirManutencao("ABC1234", 250);

        Cenario b = new Cenario(a.pasta); // novo "processo" lendo a mesma pasta
        assertEquals(3, b.bd.veiculos().size());
        assertTrue(b.bd.veiculoObrigatorio("DEF1G23") instanceof VeiculoLuxo);
        assertTrue(b.bd.veiculoObrigatorio("SUV1A23") instanceof VeiculoSUV);
        assertTrue(b.bd.veiculoObrigatorio("ABC1234") instanceof VeiculoEconomico);
        assertEquals(500, b.bd.veiculoObrigatorio("DEF1G23").getKmAtual(), 0.001);
        assertTrue(b.bd.clienteObrigatorio("22222222222") instanceof ClienteVIP);
        assertTrue(b.bd.clienteObrigatorio("11111111111") instanceof ClienteRegular);
        assertEquals(20, b.bd.clienteObrigatorio("22222222222").getPontos());
        assertEquals(StatusReserva.CONCLUIDA, b.bd.reservaObrigatoria(r.getId()).getStatus());
        assertEquals(hoje, b.bd.reservaObrigatoria(r.getId()).getInicio());
        assertEquals(a.relatorios.receitas(), b.relatorios.receitas(), 0.001);
        assertEquals(250, b.relatorios.custosManutencao(), 0.001);
    }

    @Test
    public void arquivoCorrompidoEPreservadoEOSistemaIniciaVazio() throws Exception {
        Cenario a = new Cenario();
        a.cadastro.cadastrarCliente("Ana", "11111111111", TipoCliente.REGULAR);
        Files.writeString(a.pasta.resolve("clientes.json"), "isto {não é json");

        BancoDados bd = new BancoDados(a.pasta);
        assertTrue(bd.clientes().isEmpty());
        try (var arquivos = Files.list(a.pasta)) {
            assertTrue(arquivos.anyMatch(p -> p.getFileName().toString().startsWith("clientes.json.corrompido-")));
        }
    }

    @Test
    public void idsDeReservaContinuamDepoisDeRecarregar() throws Exception {
        Cenario a = new Cenario();
        a.cadastro.cadastrarCliente("Ana", "11111111111", TipoCliente.REGULAR);
        a.cadastro.cadastrarVeiculo("ABC1234", "Argo", Categoria.ECONOMICO, 0, 0);
        a.reservas.reservar("11111111111", Categoria.ECONOMICO, hoje, hoje.plusDays(1), false);
        Cenario b = new Cenario(a.pasta);
        Reserva nova = b.reservas.reservar("11111111111", Categoria.ECONOMICO, hoje.plusDays(1), hoje.plusDays(2), false);
        assertEquals(2, nova.getId());
    }
}
