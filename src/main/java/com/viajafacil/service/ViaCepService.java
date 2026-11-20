package com.viajafacil.service;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.viajafacil.exception.RegraNegocioException;
import com.viajafacil.model.Endereco;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/** Consulta opcional ao ViaCEP; falhas nunca bloqueiam o preenchimento manual do endereço. */
public class ViaCepService {
    private final HttpClient cliente = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();

    public Endereco buscar(String cepInformado) {
        String cep = cepInformado == null ? "" : cepInformado.replaceAll("\\D", "");
        if (!cep.matches("\\d{8}")) throw new RegraNegocioException("Digite um CEP com 8 dígitos.");
        HttpRequest request = HttpRequest.newBuilder(URI.create("https://viacep.com.br/ws/" + cep + "/json/"))
                .timeout(Duration.ofSeconds(4)).GET().build();
        try {
            HttpResponse<String> resposta = cliente.send(request, HttpResponse.BodyHandlers.ofString());
            if (resposta.statusCode() != 200) throw new RegraNegocioException("Não foi possível consultar o CEP agora.");
            JsonObject json = JsonParser.parseString(resposta.body()).getAsJsonObject();
            if (json.has("erro") && json.get("erro").getAsBoolean())
                throw new RegraNegocioException("CEP não encontrado. Você pode preencher o endereço manualmente.");
            return new Endereco(cep, texto(json, "logradouro"), "", texto(json, "complemento"),
                    texto(json, "bairro"), texto(json, "localidade"), texto(json, "uf"));
        } catch (RegraNegocioException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RegraNegocioException("Consulta interrompida. Preencha o endereço manualmente se preferir.");
        } catch (IOException | RuntimeException e) {
            throw new RegraNegocioException("ViaCEP indisponível. Confira sua conexão ou preencha o endereço manualmente.");
        }
    }

    private String texto(JsonObject json, String chave) {
        return json.has(chave) && !json.get(chave).isJsonNull() ? json.get(chave).getAsString() : "";
    }
}
