package com.viajafacil.model;

import java.time.LocalDate;

/** Movimento financeiro: sinal de 30%, saldo na devolução ou reembolso de cancelamento. */
public class Pagamento {
    private final int reservaId;
    private final TipoPagamento tipo;
    private final double valor;
    private final LocalDate data;

    public Pagamento(int reservaId, TipoPagamento tipo, double valor, LocalDate data) {
        this.reservaId = reservaId;
        this.tipo = tipo;
        this.valor = valor;
        this.data = data;
    }

    /** Receita líquida: reembolsos entram negativos. */
    public double valorLiquido() { return tipo == TipoPagamento.REEMBOLSO ? -valor : valor; }

    public int getReservaId() { return reservaId; }
    public TipoPagamento getTipo() { return tipo; }
    public double getValor() { return valor; }
    public LocalDate getData() { return data; }
}
