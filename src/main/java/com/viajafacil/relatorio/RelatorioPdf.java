package com.viajafacil.relatorio;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.ColumnText;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfPageEventHelper;
import com.itextpdf.text.pdf.PdfWriter;
import com.viajafacil.service.ResumoRelatorio;
import com.viajafacil.util.Dinheiro;

import java.io.IOException;
import java.io.OutputStream;
import java.time.format.DateTimeFormatter;

/** Relatório gerencial de locações com identidade visual ViajaFácil. */
public class RelatorioPdf implements GeradorRelatorio {
    private static final BaseColor VERDE = new BaseColor(18, 61, 48);
    private static final BaseColor VERDE_CLARO = new BaseColor(235, 244, 237);
    private static final BaseColor VERDE_MEDIO = new BaseColor(23, 105, 79);
    private static final BaseColor LIMA = new BaseColor(215, 239, 140);
    private static final BaseColor TEXTO = new BaseColor(35, 51, 43);
    private static final BaseColor CINZA = new BaseColor(105, 120, 111);
    private static final BaseColor BORDA = new BaseColor(220, 230, 222);
    private static final BaseColor ALERTA = new BaseColor(166, 61, 52);
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Override
    public void gerar(ResumoRelatorio r, OutputStream saida) throws IOException {
        Document doc = new Document(PageSize.A4, 42, 42, 42, 42);
        try {
            PdfWriter writer = PdfWriter.getInstance(doc, saida);
            writer.setPageEvent(new Rodape());
            doc.open();

            PdfPTable topo = new PdfPTable(1);
            topo.setWidthPercentage(100);
            PdfPCell marca = new PdfPCell();
            marca.setBackgroundColor(VERDE);
            marca.setBorder(Rectangle.NO_BORDER);
            marca.setPadding(20);
            marca.addElement(new Phrase("VIAJAFÁCIL  /  MOBILIDADE · FROTA", font(9, Font.BOLD, LIMA)));
            marca.addElement(new Phrase("Relatório gerencial", font(23, Font.BOLD, BaseColor.WHITE)));
            marca.addElement(new Phrase("Posição em " + r.data().format(DATA), font(10, Font.NORMAL, new BaseColor(220, 235, 225))));
            topo.addCell(marca);
            doc.add(topo);

            doc.add(tituloSecao("Visão geral"));
            PdfPTable contagens = new PdfPTable(3);
            contagens.setWidthPercentage(100);
            contagens.setWidths(new float[] {1, 1, 1});
            contagens.setSpacingAfter(15);
            card(contagens, "VEÍCULOS CADASTRADOS", String.valueOf(r.totalVeiculos()));
            card(contagens, "CLIENTES CADASTRADOS", String.valueOf(r.totalClientes()));
            card(contagens, "RESERVAS REGISTRADAS", String.valueOf(r.totalReservas()));
            doc.add(contagens);

            doc.add(tituloSecao("Resumo financeiro"));
            PdfPTable financeiro = new PdfPTable(3);
            financeiro.setWidthPercentage(100);
            financeiro.setWidths(new float[] {1, 1, 1});
            financeiro.setSpacingAfter(17);
            card(financeiro, "RECEITAS LÍQUIDAS", Dinheiro.formatar(r.receitas()));
            card(financeiro, "CUSTOS DE MANUTENÇÃO", Dinheiro.formatar(r.custosManutencao()));
            card(financeiro, "RESULTADO", Dinheiro.formatar(r.resultado()), r.resultado() < 0 ? ALERTA : VERDE);
            doc.add(financeiro);

            doc.add(tituloSecao("Veículos alugados agora"));
            if (r.alugados().isEmpty()) {
                PdfPTable vazio = new PdfPTable(1);
                vazio.setWidthPercentage(100);
                PdfPCell mensagem = new PdfPCell(new Phrase("Nenhum veículo alugado no momento.", font(10, Font.NORMAL, CINZA)));
                mensagem.setBackgroundColor(VERDE_CLARO);
                mensagem.setBorderColor(BORDA);
                mensagem.setPadding(13);
                vazio.addCell(mensagem);
                doc.add(vazio);
            } else {
                PdfPTable tabela = new PdfPTable(5);
                tabela.setWidthPercentage(100);
                tabela.setWidths(new float[] {0.65f, 1.15f, 1.7f, 1.6f, 1.35f});
                tabela.setHeaderRows(1);
                tabela.setSplitRows(true);
                for (String cabecalho : new String[] {"Reserva", "Placa", "Modelo", "Cliente", "Devolução prevista"}) {
                    PdfPCell celula = new PdfPCell(new Phrase(cabecalho, font(8, Font.BOLD, BaseColor.WHITE)));
                    celula.setBackgroundColor(VERDE);
                    celula.setBorderColor(VERDE);
                    celula.setPadding(8);
                    tabela.addCell(celula);
                }
                boolean alternada = false;
                for (ResumoRelatorio.VeiculoAlugado a : r.alugados()) {
                    BaseColor fundo = alternada ? VERDE_CLARO : BaseColor.WHITE;
                    String[] valores = {"#" + a.reservaId(), a.placa(), a.modelo(), a.cliente(), a.devolucaoPrevista().format(DATA) + (a.emAtraso() ? " · ATRASADA" : "")};
                    for (int i = 0; i < valores.length; i++) {
                        PdfPCell celula = new PdfPCell(new Phrase(valores[i], font(8, Font.NORMAL, a.emAtraso() && i == 4 ? ALERTA : TEXTO)));
                        celula.setBackgroundColor(fundo);
                        celula.setBorderColor(BORDA);
                        celula.setPadding(8);
                        tabela.addCell(celula);
                    }
                    alternada = !alternada;
                }
                doc.add(tabela);
            }
        } catch (DocumentException e) {
            throw new IOException("Falha ao gerar o PDF: " + e.getMessage(), e);
        } finally {
            if (doc.isOpen()) doc.close();
        }
    }

    private static Paragraph tituloSecao(String texto) {
        Paragraph p = new Paragraph(texto, font(12, Font.BOLD, VERDE));
        p.setSpacingBefore(16);
        p.setSpacingAfter(8);
        return p;
    }

    private static void card(PdfPTable tabela, String rotulo, String valor) {
        card(tabela, rotulo, valor, VERDE_MEDIO);
    }

    private static void card(PdfPTable tabela, String rotulo, String valor, BaseColor corValor) {
        PdfPCell celula = new PdfPCell();
        celula.setBackgroundColor(VERDE_CLARO);
        celula.setBorderColor(BORDA);
        celula.setPadding(12);
        celula.addElement(new Phrase(rotulo, font(7, Font.BOLD, CINZA)));
        celula.addElement(new Phrase(valor, font(15, Font.BOLD, corValor)));
        tabela.addCell(celula);
    }

    private static Font font(float tamanho, int estilo, BaseColor cor) {
        return new Font(Font.FontFamily.HELVETICA, tamanho, estilo, cor);
    }

    private static final class Rodape extends PdfPageEventHelper {
        @Override public void onEndPage(PdfWriter writer, Document document) {
            Phrase texto = new Phrase("ViajaFácil  ·  Relatório gerencial                         Página " + writer.getPageNumber(), font(8, Font.NORMAL, CINZA));
            ColumnText.showTextAligned(writer.getDirectContent(), Element.ALIGN_LEFT, texto,
                    document.left(), document.bottom() - 18, 0);
        }
    }

    @Override public String contentType() { return "application/pdf"; }
    @Override public String extensao() { return "pdf"; }
}
