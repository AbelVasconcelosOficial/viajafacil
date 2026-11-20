package com.viajafacil.model;

public class VeiculoEconomico extends Veiculo {
    public VeiculoEconomico(String placa, String modelo, double kmAtual, double kmUltimaRevisao) {
        super(placa, modelo, kmAtual, kmUltimaRevisao);
    }
    public VeiculoEconomico(String placa, String modelo, double kmAtual, double kmUltimaRevisao,
                            String marca, String anoModelo, String valorFipe, String referenciaFipe) {
        super(placa, modelo, kmAtual, kmUltimaRevisao, marca, anoModelo, valorFipe, referenciaFipe);
    }

    @Override public Categoria getCategoria() { return Categoria.ECONOMICO; }

    @Override public double getDiaria() { return 120.00; }
}
