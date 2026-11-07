# ADR 0002: Persistência criptografada, backup e sincronização

**Status:** aceita

## Contexto
O app guarda dados de saúde (lesões, peso, dor) e precisa funcionar offline. A especificação pede
Room como fonte única da verdade, criptografia em repouso e backup opcional.

## Decisões
- **Room + SQLCipher 4.20** (`net.zetetic:sqlcipher-android`). Banco criptografado em repouso. Esquema
  exportado para `data/schemas/`; migrações explícitas, sem `fallbackToDestructiveMigration`.
- **Chave do banco**: 256 bits aleatórios, cifrados com AES-GCM cuja chave fica no Android Keystore.
  Só o texto cifrado vai para o DataStore. Copiar arquivos do app não basta para ler os dados.
- **Datas e enums como TEXT** (ISO-8601 e nome do enum). Ordenação lexicográfica coincide com a
  cronológica e o backup JSON fica legível e estável.
- **Exclusão lógica** (`deletedAt`) em todas as tabelas sincronizáveis: a remoção é propagada no backup
  e na sincronização.
- **Conflitos por última modificação** (`LastWriteWins`, empate mantém o local). Implementado no domínio
  e testado com Room real (Robolectric) no backup.
- **Backup JSON versionado** (`format` + `version`), limite de 25 MB, validação de cada registro pelos
  modelos de domínio antes de gravar, importação em uma única transação.
- **Exclusão total** (LGPD): fecha o banco, apaga o arquivo, remove a chave do Keystore e limpa as configurações.

## Alternativas consideradas
- **Sincronização em nuvem agora (Firebase/Drive):** exige projeto e OAuth configurados pelo dono do app.
  Fica para depois, com o backup JSON como contrato.
- **Criptografar o backup com senha do usuário:** recomendado. Não implementado nesta etapa; o aviso na
  tela informa que o arquivo está em texto puro.
- **Room + Keystore direto sem SQLCipher:** os dados ficariam legíveis no arquivo do banco se o aparelho
  fosse rootado. Descartado.

## Limites conhecidos
- Verificação de integridade do dispositivo (root/tamper) não implementada.
- Backup JSON sem criptografia própria (ver acima).
- Testes instrumentados (Espresso/Compose UI em aparelho) não rodam neste ambiente; a abertura do banco com
  SQLCipher e o Keystore precisam ser validados em um aparelho ou emulador real.
