package desafioBackend.order_ms.service;

import desafioBackend.order_ms.dto.OrderCreatedEvent;
import desafioBackend.order_ms.dto.OrderResponse;
import desafioBackend.order_ms.entity.OrdemItem;
import desafioBackend.order_ms.entity.OrderEntity;
import desafioBackend.order_ms.repository.OrderRepository;
import org.bson.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.*;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final MongoTemplate mongoTemplate;

    public OrderService(OrderRepository orderRepository, MongoTemplate mongoTemplate) {
        this.orderRepository = orderRepository;
        this.mongoTemplate = mongoTemplate;
    }

    public void save(OrderCreatedEvent event) {
        var entity = new OrderEntity();
        entity.setOrderId(event.codigoPedido());
        entity.setCustomerId(event.codigoCliente());
        entity.setItens(getOrderItens(event));
        entity.setTotal(getTotal(event));

        orderRepository.save(entity);

    }

    private BigDecimal getTotal(OrderCreatedEvent event) {

        return event.itens().stream().map(
               e -> e.preco().multiply(BigDecimal.valueOf(e.quantidade())))
                       .reduce(BigDecimal::add)
                .orElseThrow()
               ;

    }

    private static List<OrdemItem> getOrderItens(OrderCreatedEvent event) {
        return event.itens().stream()
                .map(e -> new OrdemItem(e.produto(), e.quantidade(), e.preco()))
                .toList();
    }

    public Page<OrderResponse> findAllByCustomerId(Long customerId, PageRequest pageRequest) {

        var orders = orderRepository.findAllByCustomerId(customerId, pageRequest);
        return orders.map(OrderResponse::fromEntity);
    }

    public BigDecimal findTotalOnOrdersByCustomerId(Long customerId) {
        var aggregations = newAggregation(
                match(Criteria.where("customerId").is(customerId)),
                group().sum("total").as("total")
        );
        var response = mongoTemplate.aggregate(aggregations, "tb_orders", Document.class);
        return new BigDecimal(response.getUniqueMappedResult().get("total").toString());
    }
}
