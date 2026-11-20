package com.viajafacil.service;

import com.viajafacil.model.*;

import static com.viajafacil.exception.RegraNegocioException.exigir;

/** Cadastro, edição e remoção de clientes e veículos. */
public class CadastroService {
    private final BancoDados bd;

    public CadastroService(BancoDados bd) { this.bd = bd; }

    public Cliente cadastrarCliente(String nome, String cpf, TipoCliente tipo) {
        return cadastrarCliente(nome, cpf, tipo, null);
    }

    public Cliente cadastrarCliente(String nome, String cpf, TipoCliente tipo, Endereco endereco) {
        exigir(nome != null && !nome.isBlank(), "Informe o nome do cliente.");
        String cpfLimpo = BancoDados.limparCpf(cpf);
        exigir(cpfLimpo.matches("[0-9]{11}"), "Informe um CPF com 11 dígitos.");
        exigir(tipo != null, "Informe o tipo do cliente (Regular ou VIP).");
        exigir(!bd.cpfClienteCadastrado(cpfLimpo), "Este CPF já está cadastrado.");
        Cliente cliente = ClienteFactory.criar(tipo, nome.trim(), cpfLimpo, 0, endereco);
        bd.clientes().add(cliente);
        bd.salvar();
        return cliente;
    }

    /** Trocar entre Regular e VIP cria o objeto da outra subclasse mantendo os pontos. */
    public Cliente editarCliente(String cpf, String nome, TipoCliente tipo) {
        Cliente atual = bd.clienteAtivoObrigatorio(cpf);
        exigir(nome != null && !nome.isBlank(), "Informe o nome do cliente.");
        exigir(tipo != null, "Informe o tipo do cliente (Regular ou VIP).");
        Cliente novo = ClienteFactory.criar(tipo, nome.trim(), atual.getCpf(), atual.getPontos(), atual.getEndereco());
        bd.clientes().set(bd.clientes().indexOf(atual), novo);
        bd.salvar();
        return novo;
    }

    public void removerCliente(String cpf) {
        Cliente cliente = bd.clienteObrigatorio(cpf);
        exigir(!cliente.isRemovido(), "Este cliente já foi removido da lista.");
        exigir(bd.reservas().stream().noneMatch(r -> r.getClienteCpf().equals(cliente.getCpf()) && r.ocupaVeiculo()),
                "Não é possível remover um cliente com reserva confirmada ou locação em andamento.");
        cliente.setRemovido(true);
        bd.salvar();
    }

    public Cliente reativarCliente(String cpf) {
        Cliente cliente = bd.clienteObrigatorio(cpf);
        exigir(cliente.isRemovido(), "Este cliente já está ativo.");
        cliente.setRemovido(false);
        bd.salvar();
        return cliente;
    }

    public Veiculo cadastrarVeiculo(String placa, String modelo, Categoria categoria, double km, double kmRevisao) {
        return cadastrarVeiculo(placa, modelo, categoria, km, kmRevisao, null, null, null, null);
    }

    public Veiculo cadastrarVeiculo(String placa, String modelo, Categoria categoria, double km, double kmRevisao,
                                     String marca, String anoModelo, String valorFipe, String referenciaFipe) {
        String placaLimpa = BancoDados.limparPlaca(placa);
        exigir(placaLimpa.matches("[A-Z]{3}[0-9][A-Z0-9][0-9]{2}"), "Informe uma placa válida (ABC1234 ou ABC1D23).");
        exigir(modelo != null && !modelo.isBlank(), "Informe o modelo.");
        exigir(categoria != null, "Selecione a categoria.");
        exigir(Double.isFinite(km) && km >= 0, "Quilometragem deve ser um número maior ou igual a zero.");
        exigir(Double.isFinite(kmRevisao) && kmRevisao >= 0, "Quilometragem da revisão deve ser um número maior ou igual a zero.");
        exigir(kmRevisao <= km, "A última revisão não pode ter quilometragem maior que a atual.");
        exigir(!bd.placaVeiculoCadastrada(placaLimpa), "Esta placa já está cadastrada.");
        Veiculo veiculo = VeiculoFactory.criar(categoria, placaLimpa, modelo.trim(), km, kmRevisao,
                marca, anoModelo, valorFipe, referenciaFipe);
        bd.veiculos().add(veiculo);
        bd.salvar();
        return veiculo;
    }

    public Veiculo editarVeiculo(String placa, String modelo) {
        Veiculo veiculo = bd.veiculoObrigatorio(placa);
        exigir(modelo != null && !modelo.isBlank(), "Informe o modelo.");
        veiculo.setModelo(modelo.trim());
        bd.salvar();
        return veiculo;
    }

    public void removerVeiculo(String placa) {
        Veiculo veiculo = bd.veiculoObrigatorio(placa);
        exigir(!veiculo.isRemovido(), "Este veículo já foi removido da frota.");
        exigir(bd.reservas().stream().noneMatch(r -> r.getVeiculoPlaca().equals(veiculo.getPlaca()) && r.ocupaVeiculo()),
                "Não é possível remover um veículo com reserva confirmada ou locação em andamento.");
        veiculo.setRemovido(true);
        bd.salvar();
    }

    public Veiculo reativarVeiculo(String placa) {
        Veiculo veiculo = bd.veiculoObrigatorio(placa);
        exigir(veiculo.isRemovido(), "Este veículo já está ativo.");
        veiculo.setRemovido(false);
        bd.salvar();
        return veiculo;
    }
}
