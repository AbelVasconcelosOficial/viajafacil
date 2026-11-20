package com.viajafacil;

import com.viajafacil.service.*;
import com.viajafacil.util.Relogio;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

/** Monta os serviços com uma pasta temporária e uma data fixa, para os testes serem repetíveis. */
class Cenario {
    static final LocalDate HOJE = LocalDate.of(2026, 11, 18);

    final Path pasta;
    final Relogio relogio = new Relogio();
    final BancoDados bd;
    final CadastroService cadastro;
    final FrotaService frota;
    final ReservaService reservas;
    final DevolucaoService devolucoes;
    final RelatorioService relatorios;

    Cenario() throws IOException { this(Files.createTempDirectory("viajafacil-teste-")); }

    Cenario(Path pasta) {
        this.pasta = pasta;
        relogio.definir(HOJE);
        bd = new BancoDados(pasta);
        cadastro = new CadastroService(bd);
        frota = new FrotaService(bd, relogio);
        reservas = new ReservaService(bd, frota, relogio);
        devolucoes = new DevolucaoService(bd, frota, relogio);
        relatorios = new RelatorioService(bd, relogio);
    }
}
