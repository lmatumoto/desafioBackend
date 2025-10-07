package desafioBackend.order_ms.dto;

import java.util.List;

public record OrderCreatedEvent(Long codigoPedido,
                                Long codigoCliente,
                                List<OrdemItemEvent> itens) {
}
