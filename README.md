# Jiu-Jitsu Diário

Diário de treino para atletas de jiu-jitsu brasileiro: faixa e graduações, treinos, estatísticas,
evolução técnica, saúde (lesões e peso), campeonatos e modo professor. Funciona offline e guarda os
dados criptografados no aparelho.

> Esta entrega não inclui check-in diário (marcar presença com um toque). Veja `docs/adr/0003-escopo-e-graficos.md`.

## Requisitos

- JDK 17
- Android SDK com `platforms;android-37.2` e `build-tools;37.0.0`
- `local.properties` com `sdk.dir=<caminho do SDK>` (não versionado)

## Como rodar

```bash
./gradlew :domain:test                 # regras de negócio (JVM)
./gradlew :data:testDebugUnitTest      # Room, backup, mapeamento (Robolectric)
./gradlew :app:testDebugUnitTest       # lógica de telas
./gradlew :app:lintDebug :data:lintDebug
./gradlew :app:assembleDebug           # APK de debug
./gradlew :app:installDebug            # instala em aparelho ou emulador conectado
```

## Funcionalidades

| Área | Entregue |
|---|---|
| Perfil e graduação | Cadastro, faixa e grau (IBJJF adulto), registro de graduações com professor e foto, linha do tempo com intervalos, estimativa do próximo grau, contadores desde a última graduação |
| Treinos (registro manual) | Criar, editar, excluir e registrar retroativamente; mais de um treino por dia; faixa do dia gravada; calendário mensal; busca e filtros combináveis |
| Observações | Campos "aprendi", "preciso corrigir" e "observações"; tags de onde treinei e do que corrigir; técnicas vinculadas; fotos (seletor do sistema) e links de vídeo validados por allowlist; ranking e nuvem de tags |
| Estatísticas | Semana, mês e ano com navegação; gráfico de barras; calendário de calor (por treinos ou horas); streak e maior streak; meta semanal com status; horas por faixa; distribuições gi/no-gi e sparring/técnica; comparativo com o período anterior; dias e horários preferidos; filtros por tipo e faixa |
| Técnicas | Biblioteca com categoria e status (com histórico de mudanças por data); lista inicial de sugestões em lote; planos de jogo como fluxograma de etapas e conexões; adversários com nível e anotações |
| Saúde | Lesões com região, gravidade, previsão e recuperação real; alerta de região recorrente; peso com gráfico, variação semanal, alerta de perda acima de 1% por semana e peso-alvo |
| Campeonatos | Calendário com contagem regressiva; lutas com método, finalização, pontos e vídeos; campanha, taxa de vitória e finalizações usadas e sofridas; checklist; plano de corte de peso com alerta de ritmo rápido |
| Conquistas | Marcos de treinos, sequências de semanas, ano de treino, primeiro campeonato e primeira graduação; data do primeiro desbloqueio preservada |
| Modo professor | Turmas, alunos, chamada por data, frequência por aluno, ausentes há 14 dias, graduação de aluno, exportação CSV da presença |
| Lembretes | Dias e horário configuráveis; aviso de meta semanal abaixo do ritmo (WorkManager, canais separados) |
| Widget | Resumo da semana (treinos e meta), somente leitura (Glance) |
| Dados | Backup JSON versionado (exportar e importar), CSV de treinos, lesões e peso, PDF de treinos, exclusão total |
| Privacidade | Consentimento registrado no onboarding (LGPD), política de privacidade no app, dados de saúde com aviso de que o app não substitui orientação médica |

## Segurança

- Banco **SQLCipher** com chave aleatória de 256 bits, protegida por **AES-GCM no Android Keystore**.
- Backup automático do sistema desabilitado (`allowBackup=false` e regras de extração).
- Links de vídeo aceitos somente em HTTPS e apenas de hosts permitidos (YouTube, Instagram, Drive).
- Nomes de arquivo de exportação validados; compartilhamento por `FileProvider` com permissão temporária.
- CSV neutraliza células que começam com `= + - @` (injeção de fórmulas).
- Importação de backup com limite de tamanho, validação de cada registro pelo domínio e transação única.
- Exclusão total apaga o arquivo do banco e a chave do Keystore.

Pendências de segurança: backup JSON em texto puro (há aviso na tela; criptografia por senha é o próximo passo),
verificação de integridade do aparelho, SAST além do lint e auditoria de dependências no CI (apenas `dependency-review` em PRs).

## Arquitetura

```
domain/   Kotlin puro: modelos, regras (streak, carga, alertas, estatísticas, CSV), contratos de repositório
data/     Room + SQLCipher, DataStore, Keystore, backup JSON, implementações dos repositórios
app/      Compose (telas e navegação), ViewModels (Hilt), notificações, widget, exportação
docs/adr/ Decisões de arquitetura
```

Regra de dependência: `app → data → domain`. O domínio não importa Android, Room nem Compose. Veja
`docs/adr/0001-arquitetura-em-camadas.md` e `docs/adr/0002-persistencia-criptografia-e-sync.md`.

## Versões

Verificadas nos repositórios Google e Maven Central em 2026-11-02.

| Item | Versão |
|---|---|
| Gradle | 9.8.2 |
| Android Gradle Plugin | 9.5.0 |
| Kotlin | 2.4.21 |
| KSP | 2.3.13 |
| Hilt | 2.60.1 (androidx 1.4.0) |
| Room | 2.8.5 |
| SQLCipher (Android) | 4.20.0 |
| Compose BOM | 2026.09.00 |
| Navigation Compose | 2.10.2 |
| WorkManager | 2.12.0 |
| Glance | 1.2.0 |
| DataStore | 1.3.0 |
| compileSdk / targetSdk | 37.2 / 37 |
| minSdk | 26 (Android 8.0) |

Se alguma versão for atualizada, revise o changelog antes de subir.

## Testes

| Módulo | Testes | Cobertura |
|---|---|---|
| `domain` | 103 unitários | faixa, treino, streak, carga, alertas, estatísticas, periodos, progressão, CSV, mídia, saúde, campeonatos, conquistas, elenco |
| `data` | 9 (Robolectric + Room em memória) | mapeamento ida e volta, backup com tags, conflito por última modificação, formato e tamanho inválidos |
| `app` | 9 unitários | validação do onboarding, lembretes, parsing de tags e links |

Não há testes instrumentados (Compose UI em aparelho) nem testes de DAO com SQLCipher: o ambiente de
desenvolvimento não tem emulador com aceleração. Esses testes devem rodar no CI com emulador antes da publicação.

## Idiomas

Português (padrão) e inglês. Todas as strings estão em `app/src/main/res/values` e `values-en`, com paridade de chaves.
Há 41 avisos de lint sobre plurais (`%d treinos`), pendentes de conversão para `<plurals>`.

## Publicação na Google Play (checklist)

- [ ] Ícone adaptativo definitivo (o atual é provisório)
- [ ] URL pública da política de privacidade (a tela dentro do app já existe)
- [ ] Formulário de Segurança de Dados: dados de saúde, dados pessoais, armazenamento local criptografado
- [ ] Classificação etária
- [ ] Permissões: `POST_NOTIFICATIONS` (opcional, para lembretes)
- [ ] Assinatura de release e `minifyEnabled` validado com testes em aparelho
- [ ] Testes instrumentados e de aceitação em aparelho real

## Pendências

1. Módulo Wear OS (fora do escopo por ser uma forma de check-in).
2. Backup criptografado por senha e backup em nuvem.
3. Editor de plano com arrastar e exportação em imagem; mapa corporal tocável.
4. Faixas infantis e juvenis.
5. Plurais e revisão de acessibilidade com TalkBack.
