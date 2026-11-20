package com.viajafacil.model;

import com.viajafacil.exception.RegraNegocioException;

/**
 * ABSTRAÇÃO: cliente da locadora. O desconto depende do tipo (Regular ou VIP) e é definido pelas
 * subclasses; os pontos de fidelidade são iguais para todos e ficam encapsulados aqui.
 */
public abstract class Cliente {
    public static final int PONTOS_POR_DIARIA = 10;
    public static final int PONTOS_DIARIA_GRATIS = 200;

    private final String cpf;
    private String nome;
    private int pontos;
    private Endereco endereco;
    private boolean removido;

    protected Cliente(String nome, String cpf, int pontos) {
        this(nome, cpf, pontos, null);
    }

    protected Cliente(String nome, String cpf, int pontos, Endereco endereco) {
        this.nome = nome;
        this.cpf = cpf;
        this.pontos = pontos;
        this.endereco = endereco;
    }

    public abstract TipoCliente getTipo();

    /** Percentual de desconto automático em cada locação (0.15 = 15%). */
    public abstract double getDesconto();

    public boolean isVip() { return getTipo() == TipoCliente.VIP; }

    public void adicionarPontos(int quantidade) { this.pontos += quantidade; }

    /** Gasta 200 pontos em uma diária grátis; falha se o saldo não for suficiente. */
    public void resgatarDiariaGratis() {
        RegraNegocioException.exigir(pontos >= PONTOS_DIARIA_GRATIS,
                "São necessários " + PONTOS_DIARIA_GRATIS + " pontos para uma diária grátis.");
        pontos -= PONTOS_DIARIA_GRATIS;
    }

    public void devolverDiariaGratis() { pontos += PONTOS_DIARIA_GRATIS; }

    public void setNome(String nome) { this.nome = nome; }

    public String getNome() { return nome; }
    public String getCpf() { return cpf; }
    public int getPontos() { return pontos; }
    public Endereco getEndereco() { return endereco; }
    public void setEndereco(Endereco endereco) { this.endereco = endereco; }
    public void setRemovido(boolean removido) { this.removido = removido; }
    public boolean isRemovido() { return removido; }
}
