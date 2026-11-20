package com.viajafacil.model;

/**
 * ABSTRAÇÃO: um veículo genérico da frota. Cada categoria é uma subclasse que define a própria
 * diária e suas restrições (POLIMORFISMO), então o restante do sistema trata todos como {@code Veiculo}.
 * ENCAPSULAMENTO: o estado só muda por métodos que protegem as regras (ex.: revisão a cada 10.000 km).
 */
public abstract class Veiculo {
    public static final double LIMITE_REVISAO_KM = 10_000;

    private final String placa;
    private String modelo;
    private String marca;
    private String anoModelo;
    private String valorFipe;
    private String referenciaFipe;
    private double kmAtual;
    private double kmUltimaRevisao;
    private StatusVeiculo status;
    private boolean removido;

    protected Veiculo(String placa, String modelo, double kmAtual, double kmUltimaRevisao) {
        this(placa, modelo, kmAtual, kmUltimaRevisao, null, null, null, null);
    }

    protected Veiculo(String placa, String modelo, double kmAtual, double kmUltimaRevisao,
                      String marca, String anoModelo, String valorFipe, String referenciaFipe) {
        this.placa = placa;
        this.modelo = modelo;
        this.marca = marca;
        this.anoModelo = anoModelo;
        this.valorFipe = valorFipe;
        this.referenciaFipe = referenciaFipe;
        this.kmAtual = kmAtual;
        this.kmUltimaRevisao = kmUltimaRevisao;
        this.status = precisaRevisao() ? StatusVeiculo.MANUTENCAO : StatusVeiculo.DISPONIVEL;
    }

    public abstract Categoria getCategoria();

    /** Valor da diária desta categoria. */
    public abstract double getDiaria();

    /** Por padrão qualquer cliente pode alugar; categorias restritas sobrescrevem. */
    public boolean aceitaCliente(Cliente cliente) { return true; }

    /** Mensagem exibida quando o cliente não pode alugar esta categoria. */
    public String getRestricao() { return null; }

    /** Regra: ao atingir 10.000 km desde a última revisão, o veículo precisa ir para manutenção. */
    public final boolean precisaRevisao() { return kmAtual - kmUltimaRevisao >= LIMITE_REVISAO_KM; }

    public void registrarQuilometragem(double km) { this.kmAtual = km; }

    public void registrarRevisao() { this.kmUltimaRevisao = kmAtual; }

    public void setModelo(String modelo) {
        if (!java.util.Objects.equals(this.modelo, modelo)) {
            marca = null; anoModelo = null; valorFipe = null; referenciaFipe = null;
        }
        this.modelo = modelo;
    }

    public void setStatus(StatusVeiculo status) { this.status = status; }
    public void setRemovido(boolean removido) { this.removido = removido; }

    public String getPlaca() { return placa; }
    public String getModelo() { return modelo; }
    public String getMarca() { return marca; }
    public String getAnoModelo() { return anoModelo; }
    public String getValorFipe() { return valorFipe; }
    public String getReferenciaFipe() { return referenciaFipe; }
    public double getKmAtual() { return kmAtual; }
    public double getKmUltimaRevisao() { return kmUltimaRevisao; }
    public StatusVeiculo getStatus() { return status; }
    public boolean isRemovido() { return removido; }
}
