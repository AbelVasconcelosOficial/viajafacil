package com.viajafacil.service;

import com.viajafacil.exception.RegraNegocioException;
import com.viajafacil.model.*;
import com.viajafacil.repository.*;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/** Dados em memória + persistência. Os serviços compartilham uma única instância. */
public class BancoDados {
    private final Repositorio<Veiculo> repVeiculos;
    private final Repositorio<Cliente> repClientes;
    private final Repositorio<Reserva> repReservas;
    private final Repositorio<Locacao> repLocacoes;
    private final Repositorio<Pagamento> repPagamentos;
    private final Repositorio<Manutencao> repManutencoes;

    private final List<Veiculo> veiculos;
    private final List<Cliente> clientes;
    private final List<Reserva> reservas;
    private final List<Locacao> locacoes;
    private final List<Pagamento> pagamentos;
    private final List<Manutencao> manutencoes;

    public BancoDados(Path pasta) {
        repVeiculos = new VeiculoRepositorio(pasta.resolve("veiculos.json"));
        repClientes = new ClienteRepositorio(pasta.resolve("clientes.json"));
        repReservas = new JsonStorage<>(pasta.resolve("reservas.json"), Reserva.class);
        repLocacoes = new JsonStorage<>(pasta.resolve("locacoes.json"), Locacao.class);
        repPagamentos = new JsonStorage<>(pasta.resolve("pagamentos.json"), Pagamento.class);
        repManutencoes = new JsonStorage<>(pasta.resolve("manutencoes.json"), Manutencao.class);
        veiculos = repVeiculos.carregar();
        clientes = repClientes.carregar();
        reservas = repReservas.carregar();
        locacoes = repLocacoes.carregar();
        pagamentos = repPagamentos.carregar();
        manutencoes = repManutencoes.carregar();
    }

    public void salvar() {
        repVeiculos.salvar(veiculos);
        repClientes.salvar(clientes);
        repReservas.salvar(reservas);
        repLocacoes.salvar(locacoes);
        repPagamentos.salvar(pagamentos);
        repManutencoes.salvar(manutencoes);
    }

    /** Remove todos os registros da demonstração e persiste o estado vazio. */
    public void limparTudo() {
        veiculos.clear();
        clientes.clear();
        reservas.clear();
        locacoes.clear();
        pagamentos.clear();
        manutencoes.clear();
        salvar();
    }

    public List<Veiculo> veiculos() { return veiculos; }
    public List<Cliente> clientes() { return clientes; }
    public List<Reserva> reservas() { return reservas; }
    public List<Locacao> locacoes() { return locacoes; }
    public List<Pagamento> pagamentos() { return pagamentos; }
    public List<Manutencao> manutencoes() { return manutencoes; }

    public int proximoIdReserva() {
        return reservas.stream().mapToInt(Reserva::getId).max().orElse(0) + 1;
    }

    public static String limparCpf(String cpf) { return cpf == null ? "" : cpf.replaceAll("[.\\-\\s]", ""); }

    public static String limparPlaca(String placa) {
        return placa == null ? "" : placa.replaceAll("[\\s-]", "").toUpperCase(java.util.Locale.ROOT);
    }

    public Optional<Cliente> buscarCliente(String cpf) {
        String limpo = limparCpf(cpf);
        return clientes.stream().filter(c -> !c.isRemovido() && c.getCpf().equals(limpo)).findFirst();
    }

    public boolean cpfClienteCadastrado(String cpf) {
        String limpo = limparCpf(cpf);
        return clientes.stream().anyMatch(c -> c.getCpf().equals(limpo));
    }

    public Cliente clienteObrigatorio(String cpf) {
        String limpo = limparCpf(cpf);
        return clientes.stream().filter(c -> c.getCpf().equals(limpo)).findFirst()
                .orElseThrow(() -> new RegraNegocioException("Cliente não encontrado. Cadastre-o primeiro."));
    }

    public Cliente clienteAtivoObrigatorio(String cpf) {
        return buscarCliente(cpf).orElseThrow(() -> new RegraNegocioException("Cliente não encontrado ou arquivado. Cadastre um cliente ativo."));
    }

    public Optional<Veiculo> buscarVeiculo(String placa) {
        String limpa = limparPlaca(placa);
        return veiculos.stream().filter(v -> !v.isRemovido() && v.getPlaca().equals(limpa)).findFirst();
    }

    public boolean placaVeiculoCadastrada(String placa) {
        String limpa = limparPlaca(placa);
        return veiculos.stream().anyMatch(v -> v.getPlaca().equals(limpa));
    }

    public Veiculo veiculoObrigatorio(String placa) {
        String limpa = limparPlaca(placa);
        return veiculos.stream().filter(v -> v.getPlaca().equals(limpa)).findFirst()
                .orElseThrow(() -> new RegraNegocioException("Veículo não encontrado."));
    }

    public Reserva reservaObrigatoria(int id) {
        return reservas.stream().filter(r -> r.getId() == id).findFirst()
                .orElseThrow(() -> new RegraNegocioException("Reserva não encontrada."));
    }

    public Optional<Locacao> locacaoAbertaDaReserva(int reservaId) {
        return locacoes.stream().filter(l -> l.getReservaId() == reservaId && l.isAberta()).findFirst();
    }

    public Optional<Locacao> locacaoAbertaDoVeiculo(String placa) {
        return locacoes.stream()
                .filter(l -> l.isAberta() && reservaObrigatoria(l.getReservaId()).getVeiculoPlaca().equals(placa))
                .findFirst();
    }
}
