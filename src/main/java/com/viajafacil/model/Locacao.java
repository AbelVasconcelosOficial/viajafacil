package com.viajafacil.model;

import java.time.LocalDate;

/** Retirada e devolução de um veículo para uma reserva. Fica "aberta" até a devolução. */
public class Locacao {
    private final int reservaId;
    private final LocalDate dataRetirada;
    private final double kmInicial;
    private LocalDate dataDevolucao;
    private double kmFinal;
    private double valorAtraso;
    private double saldoPago;

    public Locacao(int reservaId, LocalDate dataRetirada, double kmInicial) {
        this.reservaId = reservaId;
        this.dataRetirada = dataRetirada;
        this.kmInicial = kmInicial;
    }

    public boolean isAberta() { return dataDevolucao == null; }

    public void registrarDevolucao(LocalDate data, double kmFinal, double valorAtraso, double saldoPago) {
        this.dataDevolucao = data;
        this.kmFinal = kmFinal;
        this.valorAtraso = valorAtraso;
        this.saldoPago = saldoPago;
    }

    public int getReservaId() { return reservaId; }
    public LocalDate getDataRetirada() { return dataRetirada; }
    public double getKmInicial() { return kmInicial; }
    public LocalDate getDataDevolucao() { return dataDevolucao; }
    public double getKmFinal() { return kmFinal; }
    public double getValorAtraso() { return valorAtraso; }
    public double getSaldoPago() { return saldoPago; }
}
