package com.viajafacil.web;

import com.viajafacil.util.Json;

import java.nio.charset.StandardCharsets;

public record Resposta(int status, String contentType, byte[] corpo) {
    public static Resposta json(int status, Object objeto) {
        return new Resposta(status, "application/json; charset=utf-8", Json.GSON.toJson(objeto).getBytes(StandardCharsets.UTF_8));
    }

    public static Resposta json(Object objeto) { return json(200, objeto); }

    public static Resposta erro(int status, String mensagem) {
        return json(status, java.util.Map.of("erro", mensagem));
    }
}
