package com.viajafacil.service;

import com.viajafacil.exception.RegraNegocioException;
import com.viajafacil.model.*;
import com.viajafacil.util.Dinheiro;
import com.viajafacil.util.Relogio;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import static com.viajafacil.exception.RegraNegocioException.exigir;

/** Devolução: taxa de atraso, saldo de 70%, pontos de fidelidade e envio para manutenção. */
public class DevolucaoService {
    public static final double MULTA_ATRASO = 0.20;

    public record AtrasoAtual(int dias, double taxa) { }

    private final BancoDados bd;
    private final FrotaService frota;
    private final Relogio relogio;

    public DevolucaoService(BancoDados bd, FrotaService frota, Relogio relogio) {
        this.bd = bd;
        this.frota = frota;
        this.relogio = relogio;
    }

    /** Calcula sem alterar nada (para mostrar ao operador antes de confirmar). */
    public ResumoDevolucao simular(int reservaId, LocalDate data, double km) {
        return calcular(reservaId, data, km).resumo();
    }

    /** Exibe o atraso e a taxa acumulados até hoje sem concluir a locação. */
    public AtrasoAtual atrasoAtual(int reservaId) {
        Reserva r = bd.reservaObrigatoria(reservaId);
        if (r.getStatus() != StatusReserva.EM_LOCACAO || bd.locacaoAbertaDaReserva(reservaId).isEmpty()) {
            return new AtrasoAtual(0, 0);
        }
        int dias = (int) Math.max(0, ChronoUnit.DAYS.between(r.getFim(), relogio.hoje()));
        Veiculo v = bd.veiculoObrigatorio(r.getVeiculoPlaca());
        return new AtrasoAtual(dias, taxaAtraso(v, dias));
    }

    private record Calculo(Reserva reserva, Locacao locacao, Veiculo veiculo, LocalDate dia, ResumoDevolucao resumo) { }

    private Calculo calcular(int reservaId, LocalDate data, double km) {
        Reserva r = bd.reservaObrigatoria(reservaId);
        Locacao l = bd.locacaoAbertaDaReserva(reservaId)
                .orElseThrow(() -> new RegraNegocioException("Esta reserva não possui uma locação em andamento."));
        LocalDate dia = data != null ? data : relogio.hoje();
        exigir(!dia.isBefore(l.getDataRetirada()), "A devolução não pode ser anterior à retirada.");
        exigir(!dia.isAfter(relogio.hoje()), "A devolução não pode ser registrada no futuro.");
        exigir(Double.isFinite(km) && km >= 0, "Quilometragem deve ser um número maior ou igual a zero.");
        exigir(km >= l.getKmInicial(), "A quilometragem final não pode ser menor que a inicial.");

        Veiculo v = bd.veiculoObrigatorio(r.getVeiculoPlaca());
        int diasAtraso = (int) Math.max(0, ChronoUnit.DAYS.between(r.getFim(), dia));
        // Cada dia de atraso cobra a diária + 20% de multa (sem desconto VIP).
        double taxa = taxaAtraso(v, diasAtraso);
        double saldo = Dinheiro.arredondar(r.getValorTotal() + taxa - r.getValorSinal());
        int diasPagos = r.getDias() - (r.isUsouPontos() ? 1 : 0) + diasAtraso;
        int pontos = diasPagos * Cliente.PONTOS_POR_DIARIA;
        return new Calculo(r, l, v, dia, new ResumoDevolucao(r.getId(), r.getValorTotal(), diasAtraso, taxa,
                r.getValorSinal(), saldo, pontos));
    }

    private double taxaAtraso(Veiculo veiculo, int diasAtraso) {
        return Dinheiro.arredondar(diasAtraso * veiculo.getDiaria() * (1 + MULTA_ATRASO));
    }

    public ResumoDevolucao devolver(int reservaId, LocalDate data, double km) {
        Calculo c = calcular(reservaId, data, km);
        ResumoDevolucao resumo = c.resumo();
        c.locacao().registrarDevolucao(c.dia(), km, resumo.taxaAtraso(), resumo.saldo());
        if (resumo.saldo() > 0) {
            bd.pagamentos().add(new Pagamento(c.reserva().getId(), TipoPagamento.SALDO, resumo.saldo(), c.dia()));
        }
        bd.clienteObrigatorio(c.reserva().getClienteCpf()).adicionarPontos(resumo.pontosGanhos());
        c.veiculo().registrarQuilometragem(km);
        c.reserva().concluir();
        if (c.veiculo().precisaRevisao()) c.veiculo().setStatus(StatusVeiculo.MANUTENCAO);
        else frota.atualizarStatus(c.veiculo());
        bd.salvar();
        return resumo;
    }
}
