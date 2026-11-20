package com.viajafacil;

import com.viajafacil.service.*;
import com.viajafacil.util.Relogio;
import com.viajafacil.web.ApiController;
import com.viajafacil.web.Servidor;

import java.nio.file.Path;

/** Uso: java -cp ... com.viajafacil.Main [porta] [pasta-de-dados]. Padrão: 7070 e ./dados */
public class Main {
    public static void main(String[] args) {
        int porta = 7070;
        Path pasta = Path.of("dados");
        try {
            if (args.length > 0) porta = Integer.parseInt(args[0]);
            if (args.length > 1) pasta = Path.of(args[1]);
        } catch (NumberFormatException e) {
            System.err.println("Porta inválida: " + args[0] + ". Uso: Main [porta] [pasta-de-dados]");
            return;
        }

        try {
            Relogio relogio = new Relogio();
            BancoDados bd = new BancoDados(pasta);
            FrotaService frota = new FrotaService(bd, relogio);
            ApiController api = new ApiController(bd, relogio, new CadastroService(bd), frota,
                    new ReservaService(bd, frota, relogio), new DevolucaoService(bd, frota, relogio),
                    new RelatorioService(bd, relogio));
            Servidor servidor = new Servidor();
            api.registrar(servidor);
            servidor.iniciar(porta);
            System.out.println("ViajaFácil no ar: http://localhost:" + porta + "  (dados em " + pasta.toAbsolutePath() + ")");
        } catch (java.net.BindException e) {
            System.err.println("A porta " + porta + " já está em uso. Rode com outra: Main 8080");
        } catch (Exception e) {
            System.err.println("Não foi possível iniciar: " + e.getMessage());
        }
    }
}
