package com.viajafacil.model;

import java.time.LocalDate;

/** Revisão concluída de um veículo e o seu custo. */
public class Manutencao {
    private final String placa;
    private final LocalDate data;
    private final double custo;

    public Manutencao(String placa, LocalDate data, double custo) {
        this.placa = placa;
        this.data = data;
        this.custo = custo;
    }

    public String getPlaca() { return placa; }
    public LocalDate getData() { return data; }
    public double getCusto() { return custo; }
}
