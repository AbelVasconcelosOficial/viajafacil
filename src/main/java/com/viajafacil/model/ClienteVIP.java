package com.viajafacil.model;

public class ClienteVIP extends Cliente {
    public ClienteVIP(String nome, String cpf, int pontos) { super(nome, cpf, pontos); }
    public ClienteVIP(String nome, String cpf, int pontos, Endereco endereco) { super(nome, cpf, pontos, endereco); }

    @Override public TipoCliente getTipo() { return TipoCliente.VIP; }

    @Override public double getDesconto() { return 0.15; }
}
