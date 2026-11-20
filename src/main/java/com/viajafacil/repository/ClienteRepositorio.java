package com.viajafacil.repository;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.viajafacil.model.*;

import java.nio.file.Path;

public class ClienteRepositorio extends JsonStorage<Cliente> {
    public ClienteRepositorio(Path arquivo) { super(arquivo, Cliente.class); }

    @Override
    protected JsonElement serializar(Cliente cliente) {
        JsonObject objeto = gson.toJsonTree(cliente, cliente.getClass()).getAsJsonObject();
        objeto.addProperty("tipo", cliente.getTipo().name());
        return objeto;
    }

    @Override
    protected Cliente desserializar(JsonElement elemento) {
        TipoCliente tipo = TipoCliente.valueOf(elemento.getAsJsonObject().get("tipo").getAsString());
        return switch (tipo) {
            case REGULAR -> gson.fromJson(elemento, ClienteRegular.class);
            case VIP -> gson.fromJson(elemento, ClienteVIP.class);
        };
    }
}
