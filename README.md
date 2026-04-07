# FinanceAI

Documentação técnica do projeto com foco em manutenção, diagnóstico e evolução segura da base atual.

## 1. Visão geral

`FinanceAI` é um aplicativo Android nativo, de módulo único, voltado ao registro de gastos em linguagem natural. O fluxo principal funciona assim:

1. O usuário abre a tela inicial (`HomeActivity`).
2. A partir dela, navega para a tela de chat (`ChatActivity`).
3. A tela de chat envia a mensagem do usuário diretamente para a API da OpenAI.
4. A resposta é interpretada como JSON e convertida em um objeto `Gasto`.
5. O usuário confirma o gasto.
6. O gasto é salvo em memória no `GastoRepository`.
7. A Home e o Histórico leem esse repositório em memória para exibir saldo e lista.

O projeto está mais próximo de um protótipo funcional do que de uma aplicação pronta para produção. Há acoplamento alto entre UI, rede e parsing, e não existe persistência local.

## 2. Estado atual da arquitetura

### Resumo técnico

| Item | Estado atual |
| --- | --- |
| Plataforma | Android nativo |
| Linguagem | Kotlin |
| UI | XML + ViewBinding + AppCompat + Material Components |
| Arquitetura | MVVM simplificado |
| Persistência | Em memória, via singleton |
| Rede | `HttpURLConnection` direto na `Activity` |
| IA | OpenAI Chat Completions |
| Serialização | `JSONObject` manual |
| Testes | Apenas templates gerados pelo Android Studio |
| Módulos | 1 módulo (`app`) |

### Camadas realmente usadas

- `model`: contém a entidade `Gasto`.
- `repository`: contém `GastoRepository`, singleton em memória.
- `viewmodel`: coordena estado de tela e acesso ao repositório.
- `ui`: `Activities`, `Adapter` e layouts XML.

### Camadas declaradas, mas não efetivamente adotadas

- Compose está habilitado no Gradle e há arquivos de tema Compose, mas a interface atual não usa Compose.
- Retrofit e Gson Converter estão declarados como dependências, mas a integração de rede atual usa `HttpURLConnection`.

## 3. Requisitos de build e ambiente

### Versões observadas no repositório

| Item | Valor |
| --- | --- |
| Gradle Wrapper | 8.13 |
| Android Gradle Plugin | 8.13.2 |
| Kotlin | 2.0.21 |
| `compileSdk` | 36 |
| `targetSdk` | 36 |
| `minSdk` | 24 |
| Java/Kotlin target | 11 |

### Pré-requisitos para manutenção local

- Android Studio com suporte ao AGP 8.13.x
- JDK 11
- SDK Android 36 instalado
- Arquivo `local.properties` com a chave:

```properties
OPENAI_API_KEY=sua_chave_aqui
```

### Observação importante sobre segredos

A chave da OpenAI é injetada em `BuildConfig` via `buildConfigField`. Isso significa que o segredo vai para o app cliente. Para produção, isso é inadequado: qualquer chave embarcada em APK pode ser extraída.

## 4. Fluxo funcional do aplicativo

### 4.1 Tela inicial

Arquivo principal: `app/src/main/java/com/lucasdaher/financeai/ui/home/HomeActivity.kt`

Responsabilidades:

- inicializar `ActivityHomeBinding`
- observar `total` e `gastos` do `HomeViewModel`
- abrir `ChatActivity`
- abrir `HistoricoActivity`
- recarregar dados no `onResume`

Dependências diretas:

- `HomeViewModel`
- `GastoAdapter`
- layout `activity_home.xml`

### 4.2 Chat com IA

Arquivo principal: `app/src/main/java/com/lucasdaher/financeai/ui/chat/ChatActivity.kt`

Responsabilidades:

- renderizar lista textual de mensagens
- receber entrada do usuário
- chamar a API da OpenAI
- interpretar a resposta como JSON
- montar o objeto `Gasto`
- exibir bloco de confirmação
- confirmar ou cancelar o gasto

Detalhes do fluxo:

1. A mensagem do usuário é enviada para `enviarParaIA`.
2. `enviarParaIA` inicia coroutine em `Dispatchers.Main`.
3. A chamada HTTP real ocorre em `withContext(Dispatchers.IO)`.
4. `chamarAPI` faz `POST` em `https://api.openai.com/v1/chat/completions`.
5. O modelo fixo no código é `gpt-4o-mini`.
6. O prompt instrui a IA a devolver apenas JSON.
7. `interpretarResposta` extrai `choices[0].message.content`.
8. Esse conteúdo é parseado com `JSONObject`.
9. Em caso de sucesso, cria-se um `Gasto` com data do dia e `id` baseado em `System.currentTimeMillis().toInt()`.
10. O usuário precisa confirmar para persistir no repositório.

### 4.3 Histórico

Arquivo principal: `app/src/main/java/com/lucasdaher/financeai/ui/historico/HistoricoActivity.kt`

Responsabilidades:

- carregar os gastos atuais do `HistoricoViewModel`
- exibir estado vazio quando não houver itens
- renderizar a lista com `GastoAdapter`
- fechar a tela ao tocar no botão de voltar

Observação:

Essa tela usa ViewBinding para a view principal, mas ainda usa `findViewById` para o botão de voltar. O comportamento funciona, mas cria inconsistência de estilo e manutenção.

## 5. Fluxo de dados

```text
Usuário
  -> ChatActivity
  -> ChatViewModel
  -> chamada HTTP para OpenAI
  -> resposta JSON
  -> Gasto
  -> ChatViewModel.confirmarGasto()
  -> GastoRepository.instance
  -> HomeViewModel / HistoricoViewModel
  -> HomeActivity / HistoricoActivity
```

## 6. Mapa técnico dos arquivos

### 6.1 Raiz do projeto

- `build.gradle.kts`: plugins de alto nível compartilhados pelo projeto.
- `settings.gradle.kts`: define repositórios, nome do projeto e inclusão do módulo `:app`.
- `gradle.properties`: parâmetros globais do Gradle, uso de AndroidX e configuração de JVM.
- `gradle/libs.versions.toml`: catálogo de versões e aliases de dependências/plugins.
- `gradle/wrapper/gradle-wrapper.properties`: fixa o Gradle Wrapper em `8.13`.
- `gradlew` e `gradlew.bat`: scripts padrão do wrapper.
- `gradle/wrapper/gradle-wrapper.jar`: binário do wrapper.

### 6.2 Módulo `app`

- `app/build.gradle.kts`: configuração do app Android, `BuildConfig.OPENAI_API_KEY`, ViewBinding, Compose habilitado e dependências.
- `app/proguard-rules.pro`: arquivo padrão, sem regras específicas para o projeto.
- `app/src/main/AndroidManifest.xml`: registra permissões e `Activities`.

### 6.3 Código Kotlin

#### Domínio

- `app/src/main/java/com/lucasdaher/financeai/model/Gasto.kt`
  - entidade simples para representar um gasto
  - campos: `id`, `valor`, `categoria`, `descricao`, `data`

#### Repositório

- `app/src/main/java/com/lucasdaher/financeai/repository/GastoRepository.kt`
  - singleton com lista mutável em memória
  - expõe `gastos`
  - adiciona itens via `adicionarGasto`
  - calcula saldo total via `calcularTotal`

#### ViewModels

- `app/src/main/java/com/lucasdaher/financeai/viewmodel/HomeViewModel.kt`
  - publica lista de gastos e total
  - lê diretamente do repositório singleton

- `app/src/main/java/com/lucasdaher/financeai/viewmodel/HistoricoViewModel.kt`
  - publica apenas a lista de gastos
  - não faz transformação adicional

- `app/src/main/java/com/lucasdaher/financeai/viewmodel/ChatViewModel.kt`
  - mantém lista de mensagens em memória
  - mantém `gastoInterpretado`
  - confirma gasto no repositório

#### UI

- `app/src/main/java/com/lucasdaher/financeai/ui/home/HomeActivity.kt`
  - tela inicial, saldo e atalhos

- `app/src/main/java/com/lucasdaher/financeai/ui/chat/ChatActivity.kt`
  - concentra UI, rede, prompt, parsing e confirmação
  - é o ponto mais crítico da aplicação

- `app/src/main/java/com/lucasdaher/financeai/ui/historico/HistoricoActivity.kt`
  - lista todos os gastos atuais

- `app/src/main/java/com/lucasdaher/financeai/ui/GastoAdapter.kt`
  - adapter de `ListView`
  - escolhe ícone por categoria
  - formata valor como saída negativa

#### Artefatos Compose herdados do template

- `app/src/main/java/com/lucasdaher/financeai/ui/theme/Color.kt`
- `app/src/main/java/com/lucasdaher/financeai/ui/theme/Theme.kt`
- `app/src/main/java/com/lucasdaher/financeai/ui/theme/Type.kt`

Esses arquivos não são usados pelo fluxo atual baseado em XML. Devem ser mantidos apenas se houver plano real de migração para Compose.

### 6.4 Layouts XML

- `app/src/main/res/layout/activity_home.xml`
  - cabeçalho com saldo
  - botões de navegação
  - lista de gastos recentes

- `app/src/main/res/layout/activity_chat.xml`
  - cabeçalho da conversa
  - `ListView` de mensagens
  - card de confirmação do gasto
  - área de input e envio

- `app/src/main/res/layout/activity_historico.xml`
  - cabeçalho
  - estado vazio
  - lista de histórico

- `app/src/main/res/layout/item_gasto.xml`
  - card visual para cada item de gasto

### 6.5 Recursos de valores

- `app/src/main/res/values/strings.xml`
  - contém parte dos textos da interface
  - ainda coexistem com strings hardcoded em código e XML

- `app/src/main/res/values/colors.xml`
  - paleta principal baseada em tons de azul, fundo claro e cores de feedback

- `app/src/main/res/values/themes.xml`
  - tema XML principal com `Theme.MaterialComponents.Light.NoActionBar`

### 6.6 Regras de backup e extração

- `app/src/main/res/xml/backup_rules.xml`
  - arquivo padrão comentado
  - não há política de backup customizada

- `app/src/main/res/xml/data_extraction_rules.xml`
  - arquivo padrão comentado
  - não há política customizada de extração/restauração

### 6.7 Recursos de launcher

- `app/src/main/res/drawable/ic_launcher_background.xml`
- `app/src/main/res/drawable/ic_launcher_foreground.xml`
- `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`
- `app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml`
- `app/src/main/res/mipmap-mdpi/ic_launcher.webp`
- `app/src/main/res/mipmap-mdpi/ic_launcher_round.webp`
- `app/src/main/res/mipmap-hdpi/ic_launcher.webp`
- `app/src/main/res/mipmap-hdpi/ic_launcher_round.webp`
- `app/src/main/res/mipmap-xhdpi/ic_launcher.webp`
- `app/src/main/res/mipmap-xhdpi/ic_launcher_round.webp`
- `app/src/main/res/mipmap-xxhdpi/ic_launcher.webp`
- `app/src/main/res/mipmap-xxhdpi/ic_launcher_round.webp`
- `app/src/main/res/mipmap-xxxhdpi/ic_launcher.webp`
- `app/src/main/res/mipmap-xxxhdpi/ic_launcher_round.webp`

Esses arquivos são recursos padrão de ícone e não participam da lógica funcional do app.

### 6.8 Testes

- `app/src/test/java/com/lucasdaher/financeai/ExampleUnitTest.kt`
  - teste de template (`2 + 2 = 4`)

- `app/src/androidTest/java/com/lucasdaher/financeai/ExampleInstrumentedTest.kt`
  - teste de template do package name

Na prática, o projeto ainda não possui cobertura útil para regras de negócio, rede ou interface.

## 7. Dependências e uso real

### Dependências efetivamente usadas pelo código atual

- `androidx.core:core-ktx`
- `androidx.appcompat:appcompat`
- `androidx.lifecycle:lifecycle-runtime-ktx`
- `androidx.lifecycle:lifecycle-viewmodel-ktx`
- `androidx.lifecycle:lifecycle-livedata-ktx`
- `org.jetbrains.kotlinx:kotlinx-coroutines-android`
- `androidx.constraintlayout:constraintlayout`
- `com.google.android.material:material`

### Dependências declaradas, mas sem uso aparente no código atual

- `androidx.activity:activity-compose`
- Compose BOM
- `androidx.compose.ui:*`
- `androidx.compose.material3:*`
- `com.squareup.retrofit2:retrofit`
- `com.squareup.retrofit2:converter-gson`

Impacto para manutenção:

- aumentam ruído cognitivo
- sugerem uma arquitetura que não existe de fato
- dificultam decisões de refactor por misturar direção atual com intenção futura

## 8. Integração com OpenAI

### Ponto de integração

O ponto único de integração está em `ChatActivity.kt`, no método `chamarAPI`.

### Comportamento atual

- endpoint: `https://api.openai.com/v1/chat/completions`
- autenticação: `Authorization: Bearer ${BuildConfig.OPENAI_API_KEY}`
- modelo: `gpt-4o-mini`
- resposta esperada: JSON puro no conteúdo da mensagem

### Fragilidades do desenho atual

- a chave fica no cliente Android
- o parsing depende da IA respeitar exatamente o formato
- não há retry, backoff ou classificação estruturada de erros
- não há camada de serviço para mock em teste
- não há observabilidade além de logs locais
- não há timeout configurável por ambiente

## 9. Persistência e ciclo de vida

### Como funciona hoje

Todos os gastos ficam em `mutableListOf<Gasto>()` dentro do singleton `GastoRepository.instance`.

### Consequências práticas

- os dados desaparecem ao encerrar o processo do app
- não há armazenamento em banco, arquivo ou `SharedPreferences`
- não existe sincronização multiusuário
- não há controle de concorrência

### Implicação de manutenção

Qualquer funcionalidade que dependa de histórico confiável, relatórios mensais, filtro temporal ou recuperação após reinício exige introdução de persistência real.

## 10. Decisões de implementação relevantes

### ViewBinding

O projeto está baseado em XML com ViewBinding habilitado. Esse é o padrão dominante da base atual e deve ser preservado em manutenções pequenas, a menos que exista um plano explícito de migração para Compose.

### `ListView` em vez de `RecyclerView`

As listas de gastos e mensagens usam `ListView`. Para o tamanho atual da aplicação isso funciona, mas limita evolução de animações, composição de células e otimizações.

### Formatação de valores

Os valores são exibidos manualmente com `String.format`, por exemplo:

- Home: `R$ %.2f`
- Item de gasto: `- R$ %.2f`

Não há centralização nem uso de `NumberFormat` com locale.

## 11. Riscos e dívida técnica prioritária

### Prioridade alta

1. Chave da OpenAI embutida no app cliente.
2. Ausência total de persistência.
3. Lógica de rede, prompt e parsing concentrada em `ChatActivity`.

### Prioridade média

1. Dependências e arquivos Compose sem uso.
2. Retrofit declarado, mas não utilizado.
3. `android:usesCleartextTraffic="true"` habilitado sem necessidade aparente.
4. Strings distribuídas entre `strings.xml`, XML hardcoded e código Kotlin.
5. `System.currentTimeMillis().toInt()` pode gerar IDs inadequados ao longo do tempo.

### Prioridade baixa

1. Inconsistência entre ViewBinding e `findViewById`.
2. Arquivos padrão de backup e ProGuard sem customização.
3. Testes ainda no estado de template.

## 12. Direcionamento para manutenção

### Ao alterar categorias de gasto

É necessário revisar pelo menos:

- prompt em `ChatActivity.kt`
- parsing da resposta em `ChatActivity.kt`
- mapeamento de ícones em `GastoAdapter.kt`
- textos exibidos ao usuário, se aplicável

### Ao alterar a estrutura de `Gasto`

É necessário revisar:

- `Gasto.kt`
- criação do objeto em `ChatActivity.kt`
- adapter `GastoAdapter.kt`
- `HomeViewModel.kt`
- `HistoricoViewModel.kt`
- qualquer persistência futura, se já tiver sido introduzida

### Ao trocar a estratégia de IA

Os pontos de impacto imediatos são:

- método `chamarAPI`
- método `interpretarResposta`
- regras de prompt
- tratamento de erro e logs

### Ao introduzir persistência local

A transição natural seria:

1. substituir `GastoRepository` por uma interface
2. criar implementação local com Room
3. mover operações para camada de dados
4. expor estado assíncrono aos ViewModels
5. remover dependência de memória volátil

## 13. Estratégia recomendada de evolução

### Etapa 1: estabilização mínima

- mover a chamada da OpenAI para uma classe de serviço ou datasource
- mover o parsing para uma classe separada
- centralizar mensagens e textos em `strings.xml`
- remover dependências não usadas

### Etapa 2: segurança e persistência

- remover a chave do cliente
- criar backend intermediário para a chamada à IA
- persistir gastos localmente com Room
- introduzir identificadores estáveis

### Etapa 3: testabilidade

- adicionar testes unitários para parsing de resposta
- adicionar testes para `GastoRepository`
- adicionar testes de ViewModel
- adicionar ao menos um fluxo instrumentado básico

### Etapa 4: consolidação arquitetural

- separar domínio, dados e UI com contratos claros
- padronizar navegação e tratamento de estado
- decidir se a UI seguirá em XML ou migrará para Compose

## 14. Checklist de diagnóstico rápido

### O chat não responde

Verificar:

- se `OPENAI_API_KEY` existe em `local.properties`
- se o app foi recompilado após alterar a chave
- se há conectividade de rede
- logs com tag `FinanceAI`
- código HTTP retornado pela API

### O gasto foi confirmado, mas sumiu depois

Comportamento esperado na implementação atual: o repositório é apenas em memória. Reiniciar processo ou app descarta os dados.

### O histórico aparece vazio

Verificar:

- se houve confirmação do gasto no chat
- se o app ainda está no mesmo processo
- se `HistoricoViewModel.carregarGastos()` foi chamado

### O saldo da Home não atualiza

Verificar:

- se o usuário voltou para a Home após confirmar o gasto
- se `HomeActivity.onResume()` executou
- se `viewModel.atualizarDados()` leu o repositório com itens

## 15. Comandos úteis

```bash
./gradlew assembleDebug
./gradlew test
./gradlew connectedAndroidTest
```

Observação: a base atual possui apenas testes de template. Mesmo quando esses comandos passam, isso não significa que a lógica principal esteja coberta.

### Status de validação observado

Em `2026-04-07`, o comando `./gradlew test` executou com sucesso. Isso valida apenas a saúde mínima do build e os testes de template atualmente existentes.

## 16. Leitura final para quem for assumir manutenção

Se a intenção for apenas corrigir defeitos pontuais, preserve o padrão atual de XML + ViewBinding e trate `ChatActivity.kt` como ponto mais sensível da aplicação.

Se a intenção for evoluir o produto, as três primeiras frentes devem ser:

1. retirar segredos do cliente
2. persistir gastos fora da memória
3. quebrar o acoplamento entre UI, rede e parsing

Enquanto essas três frentes não forem tratadas, qualquer funcionalidade nova ficará apoiada sobre uma base frágil.
