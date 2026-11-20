package com.viajafacil.relatorio;

import com.viajafacil.service.ResumoRelatorio;
import com.viajafacil.util.Dinheiro;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;

public class RelatorioTxt implements GeradorRelatorio {
    static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Override
    public void gerar(ResumoRelatorio r, OutputStream saida) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("VIAJAFÁCIL - RELATÓRIO DE ALUGUÉIS\n");
        sb.append("Data: ").append(r.data().format(DATA)).append("\n\n");
        sb.append("Veículos cadastrados: ").append(r.totalVeiculos()).append("\n");
        sb.append("Clientes cadastrados: ").append(r.totalClientes()).append("\n");
        sb.append("Reservas registradas: ").append(r.totalReservas()).append("\n\n");
        sb.append("VEÍCULOS ALUGADOS AGORA\n");
        if (r.alugados().isEmpty()) sb.append("Nenhum veículo alugado no momento.\n");
        for (ResumoRelatorio.VeiculoAlugado a : r.alugados()) {
            sb.append(String.format("#%d | %s | %s | %s | devolução prevista %s%s%n", a.reservaId(), a.placa(), a.modelo(),
                    a.cliente(), a.devolucaoPrevista().format(DATA), a.emAtraso() ? " | EM ATRASO" : ""));
        }
        sb.append("\nFINANCEIRO\n");
        sb.append("Receitas recebidas: ").append(Dinheiro.formatar(r.receitas())).append("\n");
        sb.append("Custos de manutenção: ").append(Dinheiro.formatar(r.custosManutencao())).append("\n");
        sb.append("Resultado: ").append(Dinheiro.formatar(r.resultado())).append("\n");
        saida.write(sb.toString().getBytes(StandardCharsets.UTF_8));
    }

    @Override public String contentType() { return "text/plain; charset=utf-8"; }

    @Override public String extensao() { return "txt"; }
}
