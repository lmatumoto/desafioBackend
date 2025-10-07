package desafioBackend.order_ms.dto;

import java.math.BigDecimal;

public record OrdemItemEvent(String produto,
                             Integer quantidade,
                             BigDecimal preco) {
}
