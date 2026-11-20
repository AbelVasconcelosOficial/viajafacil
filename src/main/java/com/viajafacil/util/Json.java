package com.viajafacil.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;

import java.time.LocalDate;

/** Gson configurado para datas no formato ISO (aaaa-mm-dd). */
public final class Json {
    public static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .registerTypeAdapter(LocalDate.class,
                    (JsonSerializer<LocalDate>) (data, tipo, ctx) -> new JsonPrimitive(data.toString()))
            .registerTypeAdapter(LocalDate.class,
                    (JsonDeserializer<LocalDate>) (json, tipo, ctx) ->
                            json.isJsonNull() || json.getAsString().isBlank() ? null : LocalDate.parse(json.getAsString()))
            .create();

    private Json() { }
}
