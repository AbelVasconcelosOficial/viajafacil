package com.viajafacil.model;

/** Fábrica: escolhe a subclasse certa a partir da categoria, escondendo o {@code new} do restante do código. */
public final class VeiculoFactory {
    private VeiculoFactory() { }

    public static Veiculo criar(Categoria categoria, String placa, String modelo, double km, double kmRevisao) {
        return criar(categoria, placa, modelo, km, kmRevisao, null, null, null, null);
    }

    public static Veiculo criar(Categoria categoria, String placa, String modelo, double km, double kmRevisao,
                                String marca, String anoModelo, String valorFipe, String referenciaFipe) {
        return switch (categoria) {
            case ECONOMICO -> new VeiculoEconomico(placa, modelo, km, kmRevisao, marca, anoModelo, valorFipe, referenciaFipe);
            case SUV -> new VeiculoSUV(placa, modelo, km, kmRevisao, marca, anoModelo, valorFipe, referenciaFipe);
            case LUXO -> new VeiculoLuxo(placa, modelo, km, kmRevisao, marca, anoModelo, valorFipe, referenciaFipe);
        };
    }
}
