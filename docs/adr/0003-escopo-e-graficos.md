# ADR 0003: Escopo da entrega e escolhas de interface

**Status:** aceita

## Escopo
- **Fora do escopo por pedido do usuário:** check-in diário (marcar presença com um toque, widget de
  check-in, notificação de check-in, perfis de treino salvos). O registro manual de treino (retroativo,
  com edição e exclusão) está implementado porque é necessário para o diário funcionar.
- **Não implementado nesta entrega:** módulo Wear OS (registrar treino pelo relógio é uma forma de
  check-in), backup em nuvem, editor de plano de jogo com arrastar e exportação em imagem, mapa corporal
  tocável (usa lista de regiões), faixas infantis e juvenis, idioma além de pt-BR e en.

## Decisões de interface
- **Gráficos de barras e calendário de calor com Canvas.** A API do Vico muda entre versões e a
  renderização não pôde ser validada neste ambiente. O calendário de calor já era Canvas pela especificação.
- **Compose + Material 3** com cores dinâmicas no Android 12+ e paleta de destaque única no restante.
- **Navegação Compose com rotas tipadas** (`@Serializable`) e barra inferior com cinco destinos.
- **Textos em `res/values` (pt-BR) e `res/values-en`**, com paridade de chaves verificada.

## Consequências
- Plurais: o lint sinaliza textos como "%d treinos". Convertê-los em `<plurals>` é o próximo passo.
- Acessibilidade: alvos de toque de 48dp e `contentDescription` nos elementos visuais principais;
  auditoria com TalkBack ainda pendente.
