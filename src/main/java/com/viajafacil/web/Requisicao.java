package com.viajafacil.web;

import com.google.gson.JsonParseException;
import com.viajafacil.exception.RegraNegocioException;
import com.viajafacil.util.Json;

import java.util.List;

/** Dados de uma chamada à API: parâmetros do caminho e corpo JSON. */
public class Requisicao {
    private final List<String> parametros;
    private final String corpo;

    public Requisicao(List<String> parametros, String corpo) {
        this.parametros = parametros;
        this.corpo = corpo;
    }

    public String param(int indice) { return parametros.get(indice); }

    public int paramInt(int indice) {
        try {
            return Integer.parseInt(param(indice));
        } catch (NumberFormatException e) {
            throw new RegraNegocioException("Identificador inválido.");
        }
    }

    /** Lê o corpo como JSON; um corpo vazio vira um objeto com todos os campos nulos. */
    public <T> T corpo(Class<T> tipo) {
        try {
            T objeto = Json.GSON.fromJson(corpo == null || corpo.isBlank() ? "{}" : corpo, tipo);
            if (objeto == null) throw new RegraNegocioException("Corpo da requisição inválido.");
            return objeto;
        } catch (JsonParseException | java.time.DateTimeException e) {
            throw new RegraNegocioException("Dados inválidos na requisição. Confira datas (aaaa-mm-dd) e números.");
        }
    }
}
