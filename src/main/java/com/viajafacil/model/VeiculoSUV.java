package com.viajafacil.model;

public class VeiculoSUV extends Veiculo {
    public VeiculoSUV(String placa, String modelo, double kmAtual, double kmUltimaRevisao) {
        super(placa, modelo, kmAtual, kmUltimaRevisao);
    }
    public VeiculoSUV(String placa, String modelo, double kmAtual, double kmUltimaRevisao,
                      String marca, String anoModelo, String valorFipe, String referenciaFipe) {
        super(placa, modelo, kmAtual, kmUltimaRevisao, marca, anoModelo, valorFipe, referenciaFipe);
    }

    @Override public Categoria getCategoria() { return Categoria.SUV; }

    @Override public double getDiaria() { return 220.00; }
}
