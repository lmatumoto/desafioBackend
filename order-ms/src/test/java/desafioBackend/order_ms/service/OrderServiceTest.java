package desafioBackend.order_ms.service;

import desafioBackend.order_ms.dto.OrderCreatedEvent;
import desafioBackend.order_ms.dto.OrdemItemEvent;
import desafioBackend.order_ms.entity.OrderEntity;
import desafioBackend.order_ms.entity.OrdemItem;
import desafioBackend.order_ms.repository.OrderRepository;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private MongoTemplate mongoTemplate;

    @InjectMocks
    private OrderService orderService;

    @Test
    void save_shouldComputeTotalAndSave() {
        var itens = List.of(
                new OrdemItemEvent("produto-a", 2, new BigDecimal("10.50")),
                new OrdemItemEvent("produto-b", 3, new BigDecimal("5.00"))
        );

        var event = new OrderCreatedEvent(1L, 42L, itens);

        orderService.save(event);

        ArgumentCaptor<OrderEntity> captor = ArgumentCaptor.forClass(OrderEntity.class);
        verify(orderRepository, times(1)).save(captor.capture());

        OrderEntity saved = captor.getValue();
        assertEquals(1L, saved.getOrderId());
        assertEquals(42L, saved.getCustomerId());
        // total = 2*10.50 + 3*5.00 = 21.00 + 15.00 = 36.00
        assertEquals(new BigDecimal("36.00"), saved.getTotal());
        assertNotNull(saved.getItens());
        assertEquals(2, saved.getItens().size());
    OrdemItem first = saved.getItens().get(0);
    assertEquals("produto-a", first.getProduct());
    assertEquals(2, first.getQuantity());
    assertEquals(new BigDecimal("10.50"), first.getPrice());
    }

    @Test
    void findAllByCustomerId_shouldReturnMappedPage() {
        long customerId = 99L;
        var entity = new OrderEntity();
        entity.setOrderId(10L);
        entity.setCustomerId(customerId);
        entity.setTotal(new BigDecimal("123.45"));

        PageRequest pr = PageRequest.of(0, 10);
        Page<OrderEntity> page = new PageImpl<>(List.of(entity), pr, 1);

        when(orderRepository.findAllByCustomerId(customerId, pr)).thenReturn(page);

        var result = orderService.findAllByCustomerId(customerId, pr);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        var first = result.getContent().get(0);
        assertEquals(10L, first.orderId());
        assertEquals(customerId, first.customerId());
        assertEquals(new BigDecimal("123.45"), first.total());
    }

    @Test
    void findTotalOnOrdersByCustomerId_shouldReturnAggregatedTotal() {
        long customerId = 7L;

        Document doc = new Document();
        doc.put("total", new BigDecimal("150.25"));

    @SuppressWarnings("unchecked")
    AggregationResults<Document> aggResults = mock(AggregationResults.class);
    when(aggResults.getUniqueMappedResult()).thenReturn(doc);

    Mockito.lenient().when(mongoTemplate.aggregate(any(Aggregation.class), eq("tb_orders"), eq(Document.class))).thenReturn(aggResults);

    BigDecimal total = orderService.findTotalOnOrdersByCustomerId(customerId);

        assertEquals(new BigDecimal("150.25"), total);
    }
}
