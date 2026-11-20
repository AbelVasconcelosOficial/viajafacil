package com.viajafacil.service;

/** Valores calculados na devolução (usado tanto para simular quanto para concluir). */
public record ResumoDevolucao(int reservaId, double totalContratado, int diasAtraso, double taxaAtraso,
                              double sinalPago, double saldo, int pontosGanhos) { }
