# ADR 0001: Módulo `domain` puro Kotlin e camadas

**Status:** aceita

## Contexto
O app precisa de regras de negócio testáveis (faixa, streak, carga, alertas) independentes de
Android, e de uma base que escale para Room, Hilt, Wear OS e widgets.

## Decisão
- `:domain` é um módulo JVM puro, sem dependência de Android, Room ou Compose. Contém entidades,
  regras e cálculos. O domínio nunca importa framework.
- `:app` é a camada de apresentação/infraestrutura Android (Compose). Consome `:domain`.
- Cálculos recebem `today: LocalDate` como parâmetro, nunca leem o relógio do sistema, para
  serem determinísticos nos testes.
- Erros de regra usam a hierarquia `DomainException` (`InvalidRankException`,
  `InvalidSessionException`, `InvalidGoalException`).

## Alternativas consideradas
- **Tudo em um único módulo Android:** mais simples no início, mas as regras ficariam presas ao
  ciclo de testes instrumentados e ao framework. Descartada.
- **Módulo `data` separado já agora:** sem persistência ainda, seria over-engineering (YAGNI).
  Entra quando o Room for implementado.

## Consequências
- Testes de regra rodam na JVM (`./gradlew :domain:test`), em segundos.
- Quando o Room entrar, criaremos `:data` com DAOs e mappers, implementando interfaces de
  repositório definidas em `:domain`.

## Premissas de domínio (a validar com o usuário)
- Graus 0 a 4 para todas as faixas, incluindo coral e vermelha. O 0 representa "sem grau".
- Treinos "desde a última graduação" são os posteriores à data da graduação.
- Semanas começam na segunda-feira (ISO-8601). A semana atual incompleta não quebra o streak.
- Semanas sem treino contam como carga zero na média de sobrecarga.
