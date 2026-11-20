package com.viajafacil.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.viajafacil.exception.ArmazenamentoException;
import com.viajafacil.exception.RegraNegocioException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Servidor HTTP mínimo (com.sun.net.httpserver, já incluído no JDK): roteia /api/* para os handlers
 * e serve a página estática de src/main/resources/public. Usa uma única thread, então as operações
 * sobre os dados nunca acontecem em paralelo.
 */
public class Servidor {
    private static final Logger LOG = Logger.getLogger(Servidor.class.getName());

    @FunctionalInterface
    public interface Handler {
        Resposta tratar(Requisicao requisicao) throws Exception;
    }

    private record Rota(String metodo, Pattern padrao, Handler handler) { }

    private final List<Rota> rotas = new ArrayList<>();
    private HttpServer http;

    public void rota(String metodo, String padrao, Handler handler) {
        rotas.add(new Rota(metodo, Pattern.compile(padrao), handler));
    }

    public void iniciar(int porta) throws IOException {
        http = HttpServer.create(new InetSocketAddress(porta), 0);
        http.setExecutor(Executors.newSingleThreadExecutor());
        http.createContext("/", this::tratar);
        http.start();
    }

    public void parar() { if (http != null) http.stop(0); }

    private void tratar(HttpExchange ex) throws IOException {
        Resposta resposta;
        try {
            String caminho = ex.getRequestURI().getPath();
            resposta = caminho.startsWith("/api/") ? api(ex, caminho) : estatico(caminho);
        } catch (Throwable t) {
            LOG.log(Level.SEVERE, "Erro inesperado", t);
            resposta = Resposta.erro(500, "Erro interno inesperado.");
        }
        ex.getResponseHeaders().set("Content-Type", resposta.contentType());
        byte[] corpo = resposta.corpo();
        ex.sendResponseHeaders(resposta.status(), corpo.length == 0 ? -1 : corpo.length);
        if (corpo.length > 0) {
            try (OutputStream os = ex.getResponseBody()) { os.write(corpo); }
        }
        ex.close();
    }

    private Resposta api(HttpExchange ex, String caminho) throws IOException {
        String metodo = ex.getRequestMethod();
        String corpo = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        boolean caminhoExiste = false;
        for (Rota rota : rotas) {
            Matcher m = rota.padrao().matcher(caminho);
            if (!m.matches()) continue;
            caminhoExiste = true;
            if (!rota.metodo().equals(metodo)) continue;
            List<String> params = new ArrayList<>();
            for (int i = 1; i <= m.groupCount(); i++) params.add(URLDecoder.decode(m.group(i), StandardCharsets.UTF_8));
            try {
                return rota.handler().tratar(new Requisicao(params, corpo));
            } catch (RegraNegocioException e) {
                return Resposta.erro(400, e.getMessage());
            } catch (ArmazenamentoException e) {
                LOG.log(Level.SEVERE, "Falha de armazenamento", e);
                return Resposta.erro(500, e.getMessage());
            } catch (Exception e) {
                LOG.log(Level.SEVERE, "Erro ao tratar " + metodo + " " + caminho, e);
                return Resposta.erro(500, "Erro interno inesperado.");
            }
        }
        return caminhoExiste ? Resposta.erro(405, "Método não permitido.") : Resposta.erro(404, "Rota não encontrada.");
    }

    private Resposta estatico(String caminho) throws IOException {
        if (caminho.equals("/")) caminho = "/index.html";
        if (caminho.contains("..") || !caminho.matches("/[A-Za-z0-9_./-]+")) return Resposta.erro(404, "Não encontrado.");
        try (InputStream in = Servidor.class.getResourceAsStream("/public" + caminho)) {
            if (in == null) return Resposta.erro(404, "Não encontrado.");
            String tipo = caminho.endsWith(".html") ? "text/html; charset=utf-8"
                    : caminho.endsWith(".css") ? "text/css; charset=utf-8"
                    : caminho.endsWith(".js") ? "application/javascript; charset=utf-8"
                    : "application/octet-stream";
            return new Resposta(200, tipo, in.readAllBytes());
        }
    }
}
