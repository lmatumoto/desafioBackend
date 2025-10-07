"desafioBackend"
# order-ms

Vídeo
Resolvendo DESAFIO BACKEND com Java, Spring Boot, RabbitMQ e MongoDB


Link de referência: https://www.youtube.com/watch?v=e_WgAB0Th_I&list=LL&index=2&t=950s

- Adição de testes unitários

Projeto de exemplo (microserviço) para gerenciamento/listagem de pedidos. Implementado com Spring Boot, MongoDB e RabbitMQ.

Este README cobre como compilar, executar (localmente ou com Docker Compose), e descreve o endpoint público e a fila usada para eventos de pedido criado.

## Principais tecnologias

- Java 17
- Spring Boot 3.5.6
- Spring AMQP (RabbitMQ)
- Spring Data MongoDB
- Docker / Docker Compose (para levantar MongoDB e RabbitMQ localmente)

## Pré-requisitos

- JDK 17 instalado
- Maven (opcional — o repositório inclui o wrapper `mvnw`)
- Docker e Docker Compose (opcional, recomendado para desenvolvimento local)

## Estrutura relevante

- `src/main/java/desafioBackend/order_ms` — código-fonte do microserviço
- `src/main/resources/application.properties` — configurações (MongoDB)
- `local/docker-compose.yml` — compose para MongoDB e RabbitMQ

## Configuração padrão

O serviço espera um MongoDB configurado conforme `application.properties`. As propriedades principais:

- host: `localhost`
- porta: `27017`
- database: `desafiodb`
- usuário: `admin`
- senha: `123`

Uma URI de conexão também está definida:

```
spring.data.mongodb.uri=mongodb://admin:123@localhost:27017/desafiodb?authSource=admin
```

Para RabbitMQ não há configuração no `application.properties`; o projeto usa a configuração padrão do broker (localhost:5672) e declara a fila `order-created` automaticamente.

Fila usada no projeto (constante): `order-created` (definida em `RabbitMqConfig.ORDER_CREATED_QUEUE`).

## Levantar dependências com Docker Compose (recomendado)

No diretório do projeto rode:

```powershell
cd d:\backend\order-ms\local
docker compose up -d
```

Isso irá subir:

- MongoDB (porta 27017)
- RabbitMQ com management (porta 15672) e broker (porta 5672)

A interface de gerenciamento do RabbitMQ ficará disponível em http://localhost:15672 (usuário/senha padrão quando não alterado na imagem: guest/guest). Note que a imagem usada no compose define somente portas; a interface usará as credenciais internas defaults.

## Compilar e executar

Usando o Maven Wrapper incluído (recomendado):

```powershell
cd d:\backend\order-ms
.\mvnw clean package
.\mvnw spring-boot:run
```

Ou executar o jar gerado:

```powershell
java -jar target\order-ms-0.0.1-SNAPSHOT.jar
```

Por padrão o serviço irá iniciar na porta 8080.

## Endpoint disponível

Endpoint principal para listar pedidos por cliente:

- GET /customers/{customerId}/orders

Query params:
- page (default: 0)
- pageSize (default: 10)

Exemplo de chamada (com curl):

```powershell
curl -sS "http://localhost:8080/customers/123/orders?page=0&pageSize=10"
```

Resposta é um objeto `ApiResponse` contendo metadados (por exemplo `totalOnOrders`), lista de `OrderResponse` e informação de paginação.

## Eventos / Listener

O projeto declara uma fila `order-created`. Há um listener no projeto (`OrderCreatedListener`) que consome eventos do tipo `OrderCreatedEvent`. O produtor desses eventos não faz parte deste repositório — a fila e conversão JSON estão configuradas para aceitar mensagens JSON via `Jackson2JsonMessageConverter`.

Se quiser testar envio de mensagens ao RabbitMQ (assumindo broker local em `localhost:5672`), pode usar a interface de administração ou um script/produtor que publique um JSON compatível na fila `order-created`.

## Testes

O projeto contém testes básicos (pasta `src/test`). Para rodar os testes:

```powershell
.\mvnw test
```

### Testes unitários adicionados

Os testes unitários atuais estão em `src/test/java/desafioBackend/order_ms/service` e cobrem a classe `OrderService`.

- `OrderServiceTest.java` — principais testes:
	- `save_shouldComputeTotalAndSave()`
		- Gera um `OrderCreatedEvent` com dois itens, chama `orderService.save(event)` e verifica que:
			- Um `OrderEntity` é criado e passado para `orderRepository.save(...)`.
			- O `orderId` e `customerId` são atribuídos corretamente.
			- O `total` é calculado corretamente (multiplicação quantidade * preço e soma).
			- Os itens são mapeados para `OrdemItem` com os campos corretos.

	- `findAllByCustomerId_shouldReturnMappedPage()`
		- Mocka `OrderRepository.findAllByCustomerId(...)` retornando um `Page<OrderEntity>` e verifica que `OrderService.findAllByCustomerId(...)` retorna um `Page<OrderResponse>` com os valores mapeados corretamente.

	- `findTotalOnOrdersByCustomerId_shouldReturnAggregatedTotal()`
		- Mocka `MongoTemplate.aggregate(...)` para simular o resultado da agregação que soma os campos `total` na coleção `tb_orders` e valida que `OrderService.findTotalOnOrdersByCustomerId(...)` retorna o BigDecimal correto.

Observações sobre os testes:
- Os testes usam Mockito para mockar `OrderRepository` e `MongoTemplate`.
- O teste global de contexto (`OrderMsApplicationTests`) foi convertido para um teste mínimo que não carrega o contexto Spring (para evitar tentativa de conexão com MongoDB/RabbitMQ durante a execução unitária).
- Esses testes são unitários (isolados) — não exigem serviços externos em execução.

Para rodar apenas os testes do pacote de serviço com Maven:

```powershell
cd d:\backend\order-ms
.\mvnw -Dtest=desafioBackend.order_ms.service.* test
```


