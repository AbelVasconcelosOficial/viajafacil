package com.viajafacil.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.viajafacil.exception.RegraNegocioException;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Logger;

/** Consulta FIPE pela BrasilAPI, com fallback para a API v2 Parallelum. */
public class BrasilApiFipeService {
    private static final Logger LOG = Logger.getLogger(BrasilApiFipeService.class.getName());
    private static final String BRASIL = "https://brasilapi.com.br/api/fipe";
    private static final String ALTERNATIVA = "https://fipe.parallelum.com.br/api/v2";
    private static final long CACHE_MS = Duration.ofHours(6).toMillis();
    private final HttpClient cliente = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
    private final ConcurrentMap<String, ItemCache> cache = new ConcurrentHashMap<>();
    private record ItemCache(long expiraEm, JsonElement dados) { }

    public JsonElement marcas() {
        return consultar("/marcas/v1/carros", "/cars/brands", TipoResposta.LISTA);
    }

    public JsonElement modelos(String marca) {
        String idMarca = id(marca);
        return consultar("/veiculos/v1/carros/" + idMarca, "/cars/brands/" + idMarca + "/models", TipoResposta.LISTA);
    }

    public JsonElement anos(String marca, String modelo) {
        String idMarca = id(marca), idModelo = id(modelo);
        return consultar("/anos/v1/carros/" + idMarca + "/" + idModelo,
                "/cars/brands/" + idMarca + "/models/" + idModelo + "/years", TipoResposta.LISTA);
    }

    public JsonElement detalhe(String marca, String modelo, String ano) {
        String idMarca = id(marca), idModelo = id(modelo);
        if (ano == null || !ano.matches("[0-9]{4,5}-[0-9]+")) throw new RegraNegocioException("Ano FIPE inválido.");
        return consultar("/detalhes/v1/carros/" + idMarca + "/" + idModelo + "/" + ano,
                "/cars/brands/" + idMarca + "/models/" + idModelo + "/years/" + ano, TipoResposta.DETALHE);
    }

    private String id(String valor) {
        if (valor == null || !valor.matches("[0-9]+")) throw new RegraNegocioException("Código FIPE inválido.");
        return valor;
    }

    private JsonElement consultar(String caminhoBrasil, String caminhoAlternativo, TipoResposta tipo) {
        ItemCache salvo = cache.get(caminhoBrasil);
        if (salvo != null && salvo.expiraEm() > System.currentTimeMillis()) return salvo.dados().deepCopy();

        HttpResponse<String> respostaBrasil = null;
        String falhaBrasil = "sem resposta";
        try {
            respostaBrasil = enviar(BRASIL + caminhoBrasil);
            if (sucesso(respostaBrasil)) return guardar(caminhoBrasil, JsonParser.parseString(respostaBrasil.body()));
            falhaBrasil = "HTTP " + respostaBrasil.statusCode();
        } catch (IOException e) {
            falhaBrasil = e.getClass().getSimpleName();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RegraNegocioException("Consulta FIPE interrompida. O cadastro manual continua disponível.");
        }

        try {
            HttpResponse<String> respostaAlternativa = enviar(ALTERNATIVA + caminhoAlternativo);
            if (sucesso(respostaAlternativa)) {
                LOG.info("Consulta FIPE atendida pela API alternativa após BrasilAPI " + falhaBrasil + ".");
                return guardar(caminhoBrasil, normalizar(JsonParser.parseString(respostaAlternativa.body()), tipo));
            }
            if (respostaAlternativa.statusCode() == 429 || (respostaBrasil != null && respostaBrasil.statusCode() == 429))
                throw new RegraNegocioException("As APIs FIPE limitaram temporariamente as consultas (HTTP 429). Aguarde alguns minutos e tente novamente. O cadastro manual continua disponível.");
            if (respostaAlternativa.statusCode() == 404)
                throw new RegraNegocioException("Não há dados FIPE para essa combinação. Você pode preencher o veículo manualmente.");
            LOG.warning("BrasilAPI " + falhaBrasil + "; API FIPE alternativa HTTP " + respostaAlternativa.statusCode() + " em " + caminhoAlternativo);
            throw new RegraNegocioException("As APIs FIPE estão temporariamente indisponíveis (BrasilAPI " + falhaBrasil
                    + ", alternativa HTTP " + respostaAlternativa.statusCode() + "). O cadastro manual continua disponível.");
        } catch (RegraNegocioException e) {
            throw e;
        } catch (IOException e) {
            LOG.warning("BrasilAPI " + falhaBrasil + "; falha de conexão com a API FIPE alternativa: " + e.getMessage());
            throw new RegraNegocioException("As consultas FIPE estão indisponíveis no momento. Confira a conexão ou preencha o veículo manualmente.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RegraNegocioException("Consulta FIPE interrompida. O cadastro manual continua disponível.");
        }
    }

    private HttpResponse<String> enviar(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(5))
                .header("Accept", "application/json").GET().build();
        return cliente.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private boolean sucesso(HttpResponse<?> resposta) { return resposta.statusCode() >= 200 && resposta.statusCode() < 300; }

    private JsonElement guardar(String chave, JsonElement dados) {
        cache.put(chave, new ItemCache(System.currentTimeMillis() + CACHE_MS, dados.deepCopy()));
        return dados;
    }

    private JsonElement normalizar(JsonElement origem, TipoResposta tipo) {
        if (tipo == TipoResposta.DETALHE) {
            JsonObject de = origem.getAsJsonObject(), para = new JsonObject();
            copiar(de, para, "brand", "marca"); copiar(de, para, "model", "modelo");
            copiar(de, para, "modelYear", "anoModelo"); copiar(de, para, "price", "valor");
            copiar(de, para, "referenceMonth", "mesReferencia"); copiar(de, para, "codeFipe", "codigoFipe");
            return para;
        }
        JsonArray para = new JsonArray();
        for (JsonElement item : origem.getAsJsonArray()) {
            JsonObject de = item.getAsJsonObject(), normalizado = new JsonObject();
            String nome = valor(de, "nome", "name", "modelo");
            String codigo = valor(de, "valor", "code");
            normalizado.addProperty("nome", nome);
            normalizado.addProperty("valor", codigo);
            para.add(normalizado);
        }
        return para;
    }

    private void copiar(JsonObject origem, JsonObject destino, String chaveOrigem, String chaveDestino) {
        if (origem.has(chaveOrigem) && !origem.get(chaveOrigem).isJsonNull()) destino.add(chaveDestino, origem.get(chaveOrigem));
    }

    private String valor(JsonObject json, String... chaves) {
        for (String chave : chaves) if (json.has(chave) && !json.get(chave).isJsonNull()) return json.get(chave).getAsString();
        return "";
    }

    private enum TipoResposta { LISTA, DETALHE }
}
