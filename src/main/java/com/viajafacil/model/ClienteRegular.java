package com.viajafacil.model;

public class ClienteRegular extends Cliente {
    public ClienteRegular(String nome, String cpf, int pontos) { super(nome, cpf, pontos); }
    public ClienteRegular(String nome, String cpf, int pontos, Endereco endereco) { super(nome, cpf, pontos, endereco); }

    @Override public TipoCliente getTipo() { return TipoCliente.REGULAR; }

    @Override public double getDesconto() { return 0.0; }
}
