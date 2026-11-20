package com.viajafacil.repository;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.viajafacil.exception.ArmazenamentoException;
import com.viajafacil.util.Json;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Repositório genérico que guarda uma lista de objetos em um arquivo JSON.
 * <ul>
 *   <li>Arquivo ausente ou vazio: começa com lista vazia.</li>
 *   <li>Arquivo corrompido: é renomeado para {@code *.corrompido-<hora>} (nada é perdido) e começa vazio.</li>
 *   <li>Gravação atômica: escreve em arquivo temporário e só depois troca o original.</li>
 * </ul>
 * Subclasses sobrescrevem {@link #serializar} e {@link #desserializar} quando o tipo é abstrato
 * (veículos e clientes), gravando um campo que identifica a subclasse concreta.
 */
public class JsonStorage<T> implements Repositorio<T> {
    private static final Logger LOG = Logger.getLogger(JsonStorage.class.getName());

    private final Path arquivo;
    private final Class<T> tipo;
    protected final Gson gson = Json.GSON;

    public JsonStorage(Path arquivo, Class<T> tipo) {
        this.arquivo = arquivo;
        this.tipo = tipo;
    }

    protected JsonElement serializar(T item) {
        return gson.toJsonTree(item);
    }

    protected T desserializar(JsonElement elemento) {
        return gson.fromJson(elemento, tipo);
    }

    @Override
    public List<T> carregar() {
        List<T> lista = new ArrayList<>();
        if (!Files.exists(arquivo)) return lista;
        try {
            String texto = Files.readString(arquivo, StandardCharsets.UTF_8);
            if (texto.isBlank()) return lista;
            JsonArray array = JsonParser.parseString(texto).getAsJsonArray();
            for (JsonElement elemento : array) lista.add(desserializar(elemento));
            return lista;
        } catch (IOException | RuntimeException e) {
            // Qualquer problema de leitura/formato: preserva o arquivo e segue com dados vazios.
            isolarArquivoCorrompido(e);
            return new ArrayList<>();
        }
    }

    @Override
    public void salvar(List<T> itens) {
        Path destino = arquivo.toAbsolutePath();
        Path temporario = null;
        try {
            Files.createDirectories(destino.getParent());
            JsonArray array = new JsonArray();
            for (T item : itens) array.add(serializar(item));
            temporario = Files.createTempFile(destino.getParent(), "viajafacil-", ".tmp");
            Files.writeString(temporario, gson.toJson(array), StandardCharsets.UTF_8);
            try {
                Files.move(temporario, destino, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporario, destino, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new ArmazenamentoException("Não foi possível salvar " + destino.getFileName() + ": " + e.getMessage(), e);
        } finally {
            if (temporario != null) {
                try { Files.deleteIfExists(temporario); } catch (IOException ignorado) { /* já foi movido ou não há o que fazer */ }
            }
        }
    }

    private void isolarArquivoCorrompido(Exception causa) {
        Path copia = arquivo.resolveSibling(arquivo.getFileName() + ".corrompido-" + System.currentTimeMillis());
        try {
            Files.move(arquivo, copia, StandardCopyOption.REPLACE_EXISTING);
            LOG.warning("Arquivo " + arquivo.getFileName() + " ilegível (" + causa.getMessage()
                    + "). Guardado como " + copia.getFileName() + "; iniciando vazio.");
        } catch (IOException e) {
            LOG.warning("Arquivo " + arquivo.getFileName() + " ilegível e não foi possível isolá-lo: " + e.getMessage());
        }
    }
}
