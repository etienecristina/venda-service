## Serviço de Vendas de Veículos
Este é um serviço de backend desenvolvido em Java usando Spring Boot, focado na gestão do ciclo de vida das vendas de veículos. Ele se integra com outros microsserviços e utiliza o Stripe para processar pagamentos de forma segura e eficiente.

## Funcionalidades Principais
Criação de Vendas: Registra uma nova venda, vinculando-a a um veículo disponível.

Integração com Stripe: Gera links de pagamento dinâmicos para cada venda, facilitando o processo de checkout.

Processamento de Pagamentos: Trata eventos de webhook do Stripe para atualizar o status da venda e marcar o veículo como vendido após a confirmação do pagamento.

Consulta de Veículos: Permite a listagem de veículos disponíveis para venda e daqueles que já foram vendidos, consultando um serviço externo.

Gestão de Vendas: Oferece endpoints para consultar, editar e cancelar vendas, com validações de status para evitar inconsistências.

## Tecnologias e Ferramentas
**Linguagem:** Java 17

**Framework:** Spring Boot 3.3.4

**Banco de Dados:** PostgreSQL

**Persistência:** Spring Data JPA e Hibernate

**Pagamentos:** Stripe API (stripe-java 29.3.0)

### Dependências:

**spring-boot-starter-web:** Para a construção de APIs REST.

**spring-boot-starter-data-jpa:** Para a camada de persistência.

**spring-boot-starter-validation:** Para validação de DTOs.

**lombok:** Para reduzir a verbosidade do código.

**postgresql:** Driver de conexão com o banco de dados.


## Endpoints da API

**Vendas**

POST /vendas: Cria uma nova venda. Retorna o ID da venda e o link de pagamento do Stripe.

Body: VendaDto (com veiculoId e cpfComprador).

GET /vendas: Lista todas as vendas registradas.

GET /vendas/{vendaId}: Busca uma venda específica pelo seu ID.

PUT /vendas/{vendaId}: Edita uma venda existente.

PUT /vendas/{vendaId}/cancelar: Cancela uma venda, se o pagamento ainda não tiver sido efetuado.

**Webhook (Stripe)**

POST /webhook: Endpoint para receber eventos do Stripe. Não deve ser chamado diretamente. Ele processa eventos como checkout.session.completed para atualizar o status da venda e do veículo.

**Veículos**

GET /vendas/veiculos-a-venda: Lista todos os veículos disponíveis.

GET /vendas/veiculos-vendidos: Lista todos os veículos que já foram vendidos.


## Como Executar o Projeto
Clone o Repositório:
``` Bash 

git clone git@github.com:etienecristina/veiculo-service.git
```

**Configure as Variáveis de Ambiente:**
Crie um arquivo ```.env``` ou configure as variáveis de ambiente diretamente no seu ambiente de execução com as chaves necessárias.

```
# Configurações do Banco de Dados
DB_USERNAME=seu_usuario_postgres
DB_PASSWORD=sua_senha_postgres

# Configurações do Serviço de Veículos
URL_VEICULO_SERVICE=http://localhost:8080/veiculos
AUTH_TOKEN=seu_token_de_autenticacao_para_servico_de_veiculos

# Chaves da API do Stripe
SECRET_KEY=sua_chave_secreta_stripe
STRIPE_WEBHOOK_SECRET=sua_chave_secreta_webhook_stripe

# Configurações do Banco de Dados
DB_USERNAME=seu_usuario_postgres
DB_PASSWORD=sua_senha_postgres

# Configurações do Serviço de Veículos
URL_VEICULO_SERVICE=http://localhost:8080/veiculos
AUTH_TOKEN=seu_token_de_autenticacao_para_servico_de_veiculos

# **Chaves da API do Stripe**
SECRET_KEY=sua_chave_secreta_stripe
STRIPE_WEBHOOK_SECRET=sua_chave_secreta_webhook_stripe
```

**Compile e Execute:**
Você pode usar o Maven para construir e rodar o projeto:
``` Bash

mvn clean install
mvn spring-boot:run
```

Alternativamente, importe o projeto em sua IDE (IntelliJ, Eclipse) e execute a classe principal: ```br.com.fiap.challenge.vendas.VendaApplication.```

## Exposição do Serviço para Webhook com Ngrok
Para que o Stripe possa enviar eventos de pagamento para o nosso endpoint /webhook, o serviço precisa estar acessível publicamente na internet. Durante o desenvolvimento local, utilizamos o Ngrok para criar um túnel seguro da nossa máquina para a web.

Para usar o Ngrok, basta executar o seguinte comando no terminal, especificando a porta onde o serviço está rodando (neste caso, a porta 8081):

```Bash

ngrok http 8081 
``` 
O Ngrok fornecerá uma URL pública (por exemplo, ```https://abcdef12345.ngrok.io```) que deve ser adicionado o endpoint do webhook ao final (ficando assim: ```https://abcdef12345.ngrok.io/webhook```) e 
configurada no painel de desenvolvedor do Stripe como a URL do seu webhook. Isso permite que os eventos do Stripe cheguem até o serviço de vendas, mesmo que ele esteja rodando localmente.