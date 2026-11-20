package com.viajafacil.model;

public final class ClienteFactory {
    private ClienteFactory() { }

    public static Cliente criar(TipoCliente tipo, String nome, String cpf, int pontos) {
        return criar(tipo, nome, cpf, pontos, null);
    }

    public static Cliente criar(TipoCliente tipo, String nome, String cpf, int pontos, Endereco endereco) {
        return switch (tipo) {
            case REGULAR -> new ClienteRegular(nome, cpf, pontos, endereco);
            case VIP -> new ClienteVIP(nome, cpf, pontos, endereco);
        };
    }
}
