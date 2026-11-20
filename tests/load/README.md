# Testes de carga da API com k6

Este conjunto mede a latência e a taxa de erro de leituras autenticadas da API usando usuários virtuais concorrentes. Os testes são somente de leitura: não criam empréstimos, não alteram exemplares e não modificam o catálogo.

## Segurança do alvo

Por padrão, o script aceita somente `localhost` e `127.0.0.1`. Não execute carga contra produção ou uma base compartilhada.

O uso de host remoto é bloqueado, a menos que `K6_ALLOW_REMOTE=true` seja definido explicitamente. Essa opção não deve ser usada em produção; só habilite em um ambiente isolado, com autorização e limites acordados.

## Pré-requisitos

- API iniciada localmente no perfil `dev`.
- PostgreSQL e RabbitMQ disponíveis conforme o ambiente de desenvolvimento.
- Seed local com as contas de demonstração `admin@library.com` e `user@library.com`.
- k6 instalado: https://grafana.com/docs/k6/latest/set-up/install-k6/

Na raiz do repositório:

```bash
docker compose up -d --build
```

## Perfis de execução

Smoke, 1 usuário virtual por 10 segundos:

```bash
k6 run tests/load/library-api.js
```

Carga nominal, rampa até 5 usuários virtuais:

```bash
K6_PROFILE=load k6 run tests/load/library-api.js
```

Stress controlado, rampa até 25 usuários virtuais:

```bash
K6_PROFILE=stress k6 run tests/load/library-api.js
```

O script autentica uma vez no início, usando as contas admin e user. Durante a execução, cada VU faz chamadas concorrentes para:

- `GET /books`
- `GET /authors`
- `GET /loans/my`
- `GET /fines/my`
- `GET /loans`
- `GET /fines`

## Configuração

Credenciais podem ser sobrescritas sem alterar o script:

```bash
ADMIN_EMAIL=admin@library.com \
ADMIN_PASSWORD='Admin123!' \
USER_EMAIL=user@library.com \
USER_PASSWORD='User123!' \
K6_PROFILE=load \
k6 run tests/load/library-api.js
```

O alvo padrão é `http://localhost:8080`. Para apontar para outra porta local:

```bash
API_BASE_URL=http://127.0.0.1:8081 k6 run tests/load/library-api.js
```

## Critérios de qualidade

O script aplica os limites iniciais abaixo:

- Taxa de requisições com falha inferior a 1%.
- Percentil 95 de duração inferior a 1.500 ms.

Esses limites são uma baseline inicial, não um SLA de produção. Interprete os resultados considerando hardware, recursos do Docker, latência do banco, volume de seed e perfil selecionado. Não aumente os VUs sem observar CPU, memória, conexões PostgreSQL e logs da API.

O resultado do k6 apresenta throughput, duração, percentis e falhas por execução. Compare execuções com o mesmo ambiente e perfil.

## Escopo

O k6 mede concorrência e desempenho HTTP. Os cenários Cypress em `library-web` cobrem fluxos funcionais de navegador e operações temporárias em lote. Os testes automatizados Java continuam responsáveis pelas invariantes transacionais e regras de negócio.
