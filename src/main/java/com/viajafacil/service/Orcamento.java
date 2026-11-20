package com.viajafacil.service;

import com.viajafacil.model.Categoria;

/** Resultado de uma simulação de reserva (antes de confirmar o sinal). */
public record Orcamento(String placa, String modelo, Categoria categoria, int dias, int diasGratis,
                        double diaria, double descontoVip, double total, double sinal, int pontosDoCliente) { }
