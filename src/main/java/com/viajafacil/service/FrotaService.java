package com.viajafacil.service;

import com.viajafacil.model.*;
import com.viajafacil.util.Dinheiro;
import com.viajafacil.util.Relogio;

import java.time.LocalDate;

import static com.viajafacil.exception.RegraNegocioException.exigir;

/** Disponibilidade, status e manutenção dos veículos. */
public class FrotaService {
    private final BancoDados bd;
    private final Relogio relogio;

    public FrotaService(BancoDados bd, Relogio relogio) {
        this.bd = bd;
        this.relogio = relogio;
    }

    /** O fim do período é exclusivo: uma reserva de 28 a 29 ocupa uma diária e outra pode começar no dia 29. */
    public boolean disponivel(Veiculo v, LocalDate inicio, LocalDate fim) {
        if (v.getStatus() == StatusVeiculo.MANUTENCAO || v.precisaRevisao()) return false;
        for (Reserva r : bd.reservas()) {
            if (r.getVeiculoPlaca().equals(v.getPlaca()) && r.ocupaVeiculo()
                    && inicio.isBefore(r.getFim()) && r.getInicio().isBefore(fim)) return false;
        }
        // Veículo atrasado e ainda não devolvido não pode ser prometido novamente.
        return bd.locacoes().stream().filter(Locacao::isAberta).noneMatch(l -> {
            Reserva r = bd.reservaObrigatoria(l.getReservaId());
            return r.getVeiculoPlaca().equals(v.getPlaca()) && !r.getFim().isAfter(inicio)
                    && !relogio.hoje().isBefore(r.getFim());
        });
    }

    public void atualizarStatus(Veiculo v) {
        if (v.getStatus() == StatusVeiculo.MANUTENCAO) return;
        if (bd.locacaoAbertaDoVeiculo(v.getPlaca()).isPresent()) v.setStatus(StatusVeiculo.ALUGADO);
        else if (bd.reservas().stream().anyMatch(r -> r.getVeiculoPlaca().equals(v.getPlaca())
                && r.getStatus() == StatusReserva.CONFIRMADA)) v.setStatus(StatusVeiculo.RESERVADO);
        else v.setStatus(StatusVeiculo.DISPONIVEL);
    }

    public void iniciarManutencao(String placa) {
        Veiculo v = bd.veiculoObrigatorio(placa);
        exigir(v.getStatus() != StatusVeiculo.MANUTENCAO, "Este veículo já está em manutenção.");
        exigir(bd.reservas().stream().noneMatch(r -> r.getVeiculoPlaca().equals(v.getPlaca()) && r.ocupaVeiculo()),
                "Cancele as reservas e finalize as locações deste veículo antes da manutenção.");
        v.setStatus(StatusVeiculo.MANUTENCAO);
        bd.salvar();
    }

    public void concluirManutencao(String placa, double custo) {
        Veiculo v = bd.veiculoObrigatorio(placa);
        exigir(v.getStatus() == StatusVeiculo.MANUTENCAO, "Selecione um veículo em manutenção.");
        exigir(Double.isFinite(custo) && custo >= 0, "O custo deve ser um número maior ou igual a zero.");
        bd.manutencoes().add(new Manutencao(v.getPlaca(), relogio.hoje(), Dinheiro.arredondar(custo)));
        v.registrarRevisao();
        v.setStatus(StatusVeiculo.DISPONIVEL);
        atualizarStatus(v);
        bd.salvar();
    }
}
