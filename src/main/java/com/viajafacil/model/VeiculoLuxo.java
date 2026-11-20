package com.viajafacil.model;

/** Categoria exclusiva para clientes VIP: a regra mora na própria classe, não em ifs espalhados. */
public class VeiculoLuxo extends Veiculo {
    public VeiculoLuxo(String placa, String modelo, double kmAtual, double kmUltimaRevisao) {
        super(placa, modelo, kmAtual, kmUltimaRevisao);
    }
    public VeiculoLuxo(String placa, String modelo, double kmAtual, double kmUltimaRevisao,
                       String marca, String anoModelo, String valorFipe, String referenciaFipe) {
        super(placa, modelo, kmAtual, kmUltimaRevisao, marca, anoModelo, valorFipe, referenciaFipe);
    }

    @Override public Categoria getCategoria() { return Categoria.LUXO; }

    @Override public double getDiaria() { return 350.00; }

    @Override public boolean aceitaCliente(Cliente cliente) { return cliente.isVip(); }

    @Override public String getRestricao() { return "Veículos de Luxo são exclusivos para clientes VIP."; }
}
