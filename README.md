# ViajaFácil – Sistema de Aluguel de Carros

Sistema web local (Java + HTML) para controlar veículos, clientes, reservas, devoluções, fidelidade, manutenção e relatórios de uma locadora.
Os dados ficam em arquivos JSON na pasta `dados/`. O relatório pode ser baixado em PDF (iText).

## Requisitos

- JDK 17 ou superior (`java -version`)
- Maven 3.8 ou superior (`mvn -version`)
- Internet na primeira execução, para o Maven baixar as dependências (Gson, iText, JUnit)
- Internet para consultar CEP pelo ViaCEP (opcional; endereço também pode ser preenchido manualmente)
- Internet para consultar marca, modelo, ano e preço de referência FIPE (opcional)

## Como executar

1. Abra um terminal na pasta do projeto (onde está o `pom.xml`).
2. Execute: `mvn compile exec:java`
3. Abra o navegador em http://localhost:7070
4. Para parar, use Ctrl + C no terminal.

Outra porta ou pasta de dados: `mvn compile exec:java -Dexec.args="8080 minha-pasta"`

No IntelliJ: abra a pasta como projeto Maven, aguarde a importação e execute a classe `com.viajafacil.Main`.

## Testes

`mvn test` executa os testes de regras de negócio e de persistência (JUnit 4).

## Entrega acadêmica P2

O relatório final do projeto, com requisitos, arquitetura, demonstração, desafios e referências, está em [docs/Relatorio_Final_P2_ViajaFacil.pdf](docs/Relatorio_Final_P2_ViajaFacil.pdf).

## Roteiro de demonstração

1. **Clientes**: cadastre um cliente Regular e um VIP.
2. **Veículos**: cadastre um Econômico (km atual 9000, revisão 0), um SUV e um Luxo.
3. **Reservas**: tente reservar o Luxo para o cliente Regular (é recusado) e depois para o VIP (15% de desconto). Use "Calcular orçamento" e depois "Receber sinal e confirmar reserva" (sinal de 30%).
4. **Retirar** a reserva na data de início.
5. **Multa de atraso**: retire o veículo dentro do período reservado. No topo da tela, escolha uma data futura e clique em "Simular" (a data do sistema muda sem mexer no relógio do computador). Em Reservas, a locação em andamento mostra os dias e a taxa acumulada de atraso; clique em **Devolver**, informe a quilometragem final e confirme o saldo.
6. **Manutenção**: ao devolver com 10.000 km ou mais desde a última revisão, o veículo vai para manutenção e fica bloqueado. Em Veículos, use "Concluir revisão" e informe o custo.
7. **Fidelidade**: cada diária paga rende 10 pontos. Com 200 pontos, uma reserva Econômica pode usar "1 diária grátis".
8. **Relatórios**: veículos alugados, receitas, custos de manutenção e resultado; botão para baixar o PDF.

## Regras implementadas

- Diárias: Econômico R$ 120, SUV R$ 220, Luxo R$ 350.
- Luxo é exclusivo para clientes VIP; VIP tem 15% de desconto automático em cada locação.
- Reserva só é confirmada com o sinal de 30%; o restante é pago na devolução.
- Disponibilidade por período: o dia de devolução é livre para outra reserva começar. Veículo em manutenção ou atrasado não pode ser reservado.
- Ao atingir 10.000 km desde a última revisão, o veículo é bloqueado para manutenção.
- Atraso: cada dia cobra a diária da categoria + 20% (sem desconto VIP).
- Fidelidade: 10 pontos por diária paga (inclusive diárias de atraso); 200 pontos = 1 diária grátis no Econômico.
- Cancelamento antes da retirada: reembolso integral do sinal e devolução dos pontos (política escolhida pelo projeto, pois o enunciado não define).
- Receitas = sinais + saldos − reembolsos. Custos de manutenção aparecem separados.

## Estrutura e orientação a objetos

```
src/main/java/com/viajafacil
├── model/       entidades (Veiculo e Cliente abstratos + subclasses), Reserva, Locacao, Pagamento, Manutencao
├── exception/   RegraNegocioException (erro do usuário) e ArmazenamentoException (falha de disco)
├── repository/  Repositorio<T> (interface), JsonStorage<T> e repositórios de Veiculo e Cliente
├── service/     regras de negócio: Cadastro, Frota, Reserva, Devolucao, Relatorio
├── relatorio/   GeradorRelatorio (interface) com RelatorioPdf e RelatorioTxt
├── web/         servidor HTTP embutido e rotas da API (ApiController)
├── util/        Relogio (data simulável), Dinheiro, Json
└── Main.java
src/main/resources/public/index.html   interface web
```

- **Abstração**: `Veiculo` e `Cliente` são classes abstratas; `Repositorio<T>` e `GeradorRelatorio` são interfaces.
- **Herança**: `VeiculoEconomico`, `VeiculoSUV` e `VeiculoLuxo` estendem `Veiculo`; `ClienteRegular` e `ClienteVIP` estendem `Cliente`.
- **Polimorfismo**: os serviços chamam `getDiaria()`, `aceitaCliente()` e `getDesconto()` sem saber a subclasse. A regra "Luxo só para VIP" fica dentro de `VeiculoLuxo`, e o desconto VIP dentro de `ClienteVIP`. O relatório é gerado por qualquer `GeradorRelatorio`.
- **Encapsulamento**: campos privados; o estado só muda por métodos que protegem as regras (por exemplo `Cliente.resgatarDiariaGratis()`, `Reserva.cancelar()`, `Veiculo.precisaRevisao()`).

## Persistência e tratamento de erros

- Arquivos em `dados/`: `veiculos.json`, `clientes.json`, `reservas.json`, `locacoes.json`, `pagamentos.json`, `manutencoes.json`. São criados automaticamente.
- Pasta ou arquivo ausente: o sistema começa vazio.
- Arquivo corrompido: é renomeado para `*.corrompido-<hora>` (nada se perde) e o sistema começa vazio.
- Gravação atômica (arquivo temporário e troca), para não deixar JSON pela metade.
- Entradas inválidas (CPF, placa, datas, números, regras) retornam mensagem clara e o sistema continua funcionando. Porta em uso é avisada no terminal.
- Endereço de cliente é salvo localmente. A busca pelo CEP tem timeout e trata CEP inexistente ou ViaCEP fora do ar; os campos continuam editáveis para preenchimento manual.
- Marca, ano e referência FIPE são salvos com o veículo. A consulta é informativa, tenta a BrasilAPI e usa a API v2 Parallelum como fallback; respostas ficam em cache durante 6 horas. O preço não altera a diária e o cadastro continua manual se as APIs estiverem indisponíveis.
- Remover um veículo o arquiva da frota se não houver reserva confirmada ou locação em andamento; o registro fica preservado para o histórico de reservas.
- Remover um cliente o arquiva da lista se não houver reserva confirmada ou locação em andamento; CPF e dados do cadastro ficam preservados para o histórico e não podem ser duplicados.
- A aba **Arquivados** permite reativar clientes e veículos sem apagar o histórico relacionado.
- Na reserva, o cliente é selecionado por nome e CPF; o orçamento lista primeiro os veículos disponíveis para o período escolhido.

## Decisões e limitações

- A interface usa o servidor HTTP que já vem no JDK (`com.sun.net.httpserver`), sem framework web. A camada `web/` é separada, então pode ser trocada por Javalin ou Spring.
- Não implementado: salvar os arquivos no Google Drive (desafio opcional) e autenticação de usuários.
- Pagamentos são registrados pelo operador; não há integração bancária.
