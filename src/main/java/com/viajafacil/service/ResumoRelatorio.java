package com.viajafacil.service;

import java.time.LocalDate;
import java.util.List;

/** Dados do relatório de aluguéis, receitas e custos de manutenção. */
public record ResumoRelatorio(LocalDate data, List<VeiculoAlugado> alugados, double receitas,
                              double custosManutencao, double resultado,
                              int totalVeiculos, int totalClientes, int totalReservas) {

    public record VeiculoAlugado(int reservaId, String placa, String modelo, String cliente,
                                 LocalDate devolucaoPrevista, boolean emAtraso) { }
}
