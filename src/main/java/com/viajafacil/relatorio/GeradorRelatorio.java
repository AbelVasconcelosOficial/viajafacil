package com.viajafacil.relatorio;

import com.viajafacil.service.ResumoRelatorio;

import java.io.IOException;
import java.io.OutputStream;

/** Abstração para formatos de relatório: o serviço de relatório não precisa saber se é PDF ou texto. */
public interface GeradorRelatorio {
    void gerar(ResumoRelatorio resumo, OutputStream saida) throws IOException;

    String contentType();

    String extensao();
}
