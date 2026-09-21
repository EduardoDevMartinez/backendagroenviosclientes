package com.agroenvios.clientes.primary.dto.pago;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Una página de pedidos del cliente para scroll infinito. {@code conteos} (pedidos por
 * estado, sin importar el filtro aplicado) viaja solo en la página 0; en las demás es null.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PedidoPageDTO {
    private List<PedidoResponse> items;
    private boolean hasMore;
    private int page;
    private Map<String, Long> conteos;
}
