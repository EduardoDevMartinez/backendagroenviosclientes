package com.agroenvios.clientes.primary.dto.pago;

import lombok.Data;
import java.util.List;

@Data
public class PreferenciaRequest {
    private List<ItemPagoDto> items;
    private Long direccionId; // Para referencia del pedido
}
