package com.viajafacil.model;

import com.viajafacil.exception.RegraNegocioException;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/** Reserva de um veículo por um cliente. Guarda apenas CPF e placa, o que facilita salvar em JSON. */
public class Reserva {
    private final int id;
    private final String clienteCpf;
    private final String veiculoPlaca;
    private final LocalDate inicio;
    private final LocalDate fim;
    private final boolean usouPontos;
    private final double valorTotal;
    private final double valorSinal;
    private StatusReserva status = StatusReserva.CONFIRMADA;

    public Reserva(int id, String clienteCpf, String veiculoPlaca, LocalDate inicio, LocalDate fim,
                   boolean usouPontos, double valorTotal, double valorSinal) {
        this.id = id;
        this.clienteCpf = clienteCpf;
        this.veiculoPlaca = veiculoPlaca;
        this.inicio = inicio;
        this.fim = fim;
        this.usouPontos = usouPontos;
        this.valorTotal = valorTotal;
        this.valorSinal = valorSinal;
    }

    public int getDias() { return (int) ChronoUnit.DAYS.between(inicio, fim); }

    /** Reservas confirmadas ou em locação ocupam o veículo. */
    public boolean ocupaVeiculo() { return status == StatusReserva.CONFIRMADA || status == StatusReserva.EM_LOCACAO; }

    public void iniciarLocacao() {
        RegraNegocioException.exigir(status == StatusReserva.CONFIRMADA, "Selecione uma reserva confirmada.");
        status = StatusReserva.EM_LOCACAO;
    }

    public void concluir() { status = StatusReserva.CONCLUIDA; }

    public void cancelar() {
        RegraNegocioException.exigir(status != StatusReserva.EM_LOCACAO, "Um veículo já retirado precisa ser devolvido.");
        RegraNegocioException.exigir(status == StatusReserva.CONFIRMADA, "Só é possível cancelar reservas confirmadas.");
        status = StatusReserva.CANCELADA;
    }

    public int getId() { return id; }
    public String getClienteCpf() { return clienteCpf; }
    public String getVeiculoPlaca() { return veiculoPlaca; }
    public LocalDate getInicio() { return inicio; }
    public LocalDate getFim() { return fim; }
    public boolean isUsouPontos() { return usouPontos; }
    public double getValorTotal() { return valorTotal; }
    public double getValorSinal() { return valorSinal; }
    public StatusReserva getStatus() { return status; }
}
