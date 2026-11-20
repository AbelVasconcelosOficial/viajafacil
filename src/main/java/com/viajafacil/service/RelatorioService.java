package com.viajafacil.service;

import com.viajafacil.model.*;
import com.viajafacil.util.Dinheiro;
import com.viajafacil.util.Relogio;

import java.util.List;

public class RelatorioService {
    private final BancoDados bd;
    private final Relogio relogio;

    public RelatorioService(BancoDados bd, Relogio relogio) {
        this.bd = bd;
        this.relogio = relogio;
    }

    /** Receitas = sinais + saldos - reembolsos. Custos de manutenção ficam separados. */
    public double receitas() {
        return Dinheiro.arredondar(bd.pagamentos().stream().mapToDouble(Pagamento::valorLiquido).sum());
    }

    public double custosManutencao() {
        return Dinheiro.arredondar(bd.manutencoes().stream().mapToDouble(Manutencao::getCusto).sum());
    }

    public ResumoRelatorio gerarResumo() {
        List<ResumoRelatorio.VeiculoAlugado> alugados = bd.locacoes().stream()
                .filter(Locacao::isAberta)
                .map(l -> {
                    Reserva r = bd.reservaObrigatoria(l.getReservaId());
                    Veiculo v = bd.veiculoObrigatorio(r.getVeiculoPlaca());
                    Cliente c = bd.clienteObrigatorio(r.getClienteCpf());
                    return new ResumoRelatorio.VeiculoAlugado(r.getId(), v.getPlaca(), v.getModelo(), c.getNome(),
                            r.getFim(), relogio.hoje().isAfter(r.getFim()));
                }).toList();
        double receitas = receitas();
        double custos = custosManutencao();
        return new ResumoRelatorio(relogio.hoje(), alugados, receitas, custos, Dinheiro.arredondar(receitas - custos),
                (int) bd.veiculos().stream().filter(v -> !v.isRemovido()).count(),
                (int) bd.clientes().stream().filter(c -> !c.isRemovido()).count(), bd.reservas().size());
    }
}
