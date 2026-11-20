package com.viajafacil.repository;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.viajafacil.model.*;

import java.nio.file.Path;

/** O Gson não sabe criar uma classe abstrata; por isso gravamos a categoria e escolhemos a subclasse ao ler. */
public class VeiculoRepositorio extends JsonStorage<Veiculo> {
    public VeiculoRepositorio(Path arquivo) { super(arquivo, Veiculo.class); }

    @Override
    protected JsonElement serializar(Veiculo veiculo) {
        JsonObject objeto = gson.toJsonTree(veiculo, veiculo.getClass()).getAsJsonObject();
        objeto.addProperty("categoria", veiculo.getCategoria().name());
        return objeto;
    }

    @Override
    protected Veiculo desserializar(JsonElement elemento) {
        Categoria categoria = Categoria.valueOf(elemento.getAsJsonObject().get("categoria").getAsString());
        return switch (categoria) {
            case ECONOMICO -> gson.fromJson(elemento, VeiculoEconomico.class);
            case SUV -> gson.fromJson(elemento, VeiculoSUV.class);
            case LUXO -> gson.fromJson(elemento, VeiculoLuxo.class);
        };
    }
}
