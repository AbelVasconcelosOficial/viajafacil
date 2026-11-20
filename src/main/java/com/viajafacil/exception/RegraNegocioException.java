package com.viajafacil.exception;

/** Erro de regra de negócio ou de dado inválido informado pelo usuário. A mensagem é exibida ao usuário. */
public class RegraNegocioException extends RuntimeException {
    public RegraNegocioException(String mensagem) {
        super(mensagem);
    }

    /** Lança a exceção quando a condição não é verdadeira. */
    public static void exigir(boolean condicao, String mensagem) {
        if (!condicao) throw new RegraNegocioException(mensagem);
    }
}
