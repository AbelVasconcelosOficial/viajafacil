package com.viajafacil.util;

import java.time.LocalDate;

/**
 * Fonte única da "data de hoje" do sistema. Permite simular outra data na demonstração
 * (por exemplo, para mostrar a multa de atraso sem mexer no relógio do computador).
 */
public class Relogio {
    private LocalDate fixa;

    public synchronized LocalDate hoje() {
        return fixa != null ? fixa : LocalDate.now();
    }

    public synchronized void definir(LocalDate data) {
        this.fixa = data;
    }

    public synchronized boolean simulado() {
        return fixa != null;
    }
}
