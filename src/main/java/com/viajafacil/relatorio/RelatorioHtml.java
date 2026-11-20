package com.viajafacil.relatorio;

import com.viajafacil.service.ResumoRelatorio;
import com.viajafacil.util.Dinheiro;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

/** Versão formatada para leitura e impressão no navegador. */
public class RelatorioHtml implements GeradorRelatorio {
    @Override
    public void gerar(ResumoRelatorio r, OutputStream saida) throws IOException {
        String alugados = r.alugados().stream().map(a -> "<tr><td>#" + a.reservaId() + "</td><td>" + esc(a.placa())
                + "</td><td>" + esc(a.modelo()) + "</td><td>" + esc(a.cliente()) + "</td><td>"
                + a.devolucaoPrevista().format(RelatorioTxt.DATA)
                + (a.emAtraso() ? " <span class=\"atraso\">Em atraso</span>" : "") + "</td></tr>").collect(Collectors.joining());
        if (alugados.isEmpty()) alugados = "<tr><td colspan=\"5\" class=\"vazio\">Nenhum veículo alugado no momento.</td></tr>";
        String resultadoCor = r.resultado() < 0 ? " negativo" : "";
        String html = """
                <!doctype html>
                <html lang="pt-BR"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
                <title>Relatório ViajaFácil</title><style>
                *{box-sizing:border-box}body{margin:0;background:#f2f5f1;color:#192824;font:15px/1.5 Inter,ui-sans-serif,system-ui,-apple-system,"Segoe UI",sans-serif}
                .pagina{max-width:1040px;margin:36px auto;padding:0 20px}.topo{padding:28px 32px;border-radius:20px;background:linear-gradient(112deg,#123d30,#17694f);color:#fff;box-shadow:0 12px 30px #123d301c}
                .marca{color:#d7ef8c;font-size:11px;font-weight:800;letter-spacing:.14em}.topo h1{margin:8px 0 3px;font-size:30px;letter-spacing:-.04em}.topo p{margin:0;color:#d4e5d9}.acoes{display:flex;justify-content:space-between;align-items:center;margin:18px 0}.acoes span{color:#718079;font-size:12px}
                button{padding:10px 15px;border:0;border-radius:10px;background:#17694f;color:white;font:inherit;font-size:13px;font-weight:700;cursor:pointer}.secao{margin-top:24px;padding:24px;border:1px solid #e3ebe4;border-radius:18px;background:#fff;box-shadow:0 10px 28px #1436290d}.secao h2{margin:0 0 16px;color:#104a39;font-size:18px}
                .cards{display:grid;grid-template-columns:repeat(3,1fr);gap:12px}.card{padding:16px;border:1px solid #dfe9df;border-radius:14px;background:linear-gradient(145deg,#fff,#f6faf5)}.card small{display:block;color:#718079;font-size:10px;font-weight:800;letter-spacing:.08em;text-transform:uppercase}.card strong{display:block;margin-top:6px;color:#17694f;font-size:22px;letter-spacing:-.03em}.card.negativo strong{color:#a63d34}
                .tabela{overflow-x:auto;border:1px solid #e7ede8;border-radius:12px}table{width:100%%;border-collapse:collapse;background:white}th,td{padding:12px 14px;border-bottom:1px solid #edf1ed;text-align:left;white-space:nowrap}th{background:#f5f8f5;color:#718079;font-size:10px;letter-spacing:.08em;text-transform:uppercase}td{font-size:13px}.atraso{padding:3px 8px;border-radius:99px;background:#fff0ee;color:#a63d34;font-size:10px;font-weight:800}.vazio{padding:20px;color:#718079;text-align:center}
                @media(max-width:640px){.pagina{margin:16px auto;padding:0 12px}.topo{padding:23px}.topo h1{font-size:25px}.secao{padding:17px}.cards{grid-template-columns:1fr}.acoes{align-items:flex-start;gap:12px;flex-direction:column}}
                @media print{body{background:#fff}.pagina{max-width:none;margin:0;padding:0}.topo,.secao{box-shadow:none}.acoes button{display:none}.secao{break-inside:avoid}}
                </style></head><body><main class="pagina"><header class="topo"><div class="marca">VIAJAFÁCIL · MOBILIDADE E FROTA</div><h1>Relatório gerencial</h1><p>Resumo da operação em %s</p></header>
                <div class="acoes"><span>Gerado pelo sistema ViajaFácil</span><button onclick="window.print()">Imprimir / salvar em PDF</button></div>
                <section class="secao"><h2>Visão geral</h2><div class="cards">
                <div class="card"><small>Veículos cadastrados</small><strong>%d</strong></div><div class="card"><small>Clientes cadastrados</small><strong>%d</strong></div><div class="card"><small>Reservas registradas</small><strong>%d</strong></div>
                </div></section>
                <section class="secao"><h2>Resumo financeiro</h2><div class="cards">
                <div class="card"><small>Receitas líquidas</small><strong>%s</strong></div><div class="card"><small>Custos de manutenção</small><strong>%s</strong></div><div class="card%s"><small>Resultado</small><strong>%s</strong></div>
                </div></section>
                <section class="secao"><h2>Veículos alugados agora</h2><div class="tabela"><table><thead><tr><th>Reserva</th><th>Placa</th><th>Modelo</th><th>Cliente</th><th>Devolução prevista</th></tr></thead><tbody>%s</tbody></table></div></section>
                </main></body></html>
                """.formatted(r.data().format(RelatorioTxt.DATA), r.totalVeiculos(), r.totalClientes(), r.totalReservas(),
                Dinheiro.formatar(r.receitas()), Dinheiro.formatar(r.custosManutencao()), resultadoCor,
                Dinheiro.formatar(r.resultado()), alugados);
        saida.write(html.getBytes(StandardCharsets.UTF_8));
    }

    private String esc(String valor) {
        return valor == null ? "" : valor.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }

    @Override public String contentType() { return "text/html; charset=utf-8"; }
    @Override public String extensao() { return "html"; }
}
