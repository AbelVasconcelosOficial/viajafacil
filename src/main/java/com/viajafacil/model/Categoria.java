package com.viajafacil.model;

public enum Categoria {
    ECONOMICO("Econômico"), SUV("SUV"), LUXO("Luxo");

    private final String rotulo;

    Categoria(String rotulo) { this.rotulo = rotulo; }

    public String getRotulo() { return rotulo; }
}
