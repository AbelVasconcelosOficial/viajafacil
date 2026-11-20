package com.viajafacil.exception;

/** Falha ao gravar os arquivos de dados (disco cheio, sem permissão, etc.). */
public class ArmazenamentoException extends RuntimeException {
    public ArmazenamentoException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
