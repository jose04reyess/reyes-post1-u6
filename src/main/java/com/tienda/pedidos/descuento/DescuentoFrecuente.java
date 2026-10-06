package com.tienda.pedidos.descuento;

import com.tienda.pedidos.validacion.ContextoPedido;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component("descuentoFrecuente")
public class DescuentoFrecuente implements EstrategiaDescuento {

    private final JdbcTemplate jdbcTemplate;

    public DescuentoFrecuente(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public double calcular(ContextoPedido contexto) {
        if (contexto.getPedidoRequest() == null || contexto.getPedidoRequest().getClienteId() == null) {
            return 0.0;
        }

        Long clienteId = contexto.getPedidoRequest().getClienteId();
        Integer cantidadPedidosPrevios = jdbcTemplate.query(
                "SELECT COUNT(*) FROM pedidos WHERE cliente_id = ?",
                rs -> rs.next() ? rs.getInt(1) : 0,
                clienteId
        );

        int totalPedidos = (cantidadPedidosPrevios != null) ? cantidadPedidosPrevios : 0;
        if (totalPedidos > 10) {
            return 0.08;
        } else if (totalPedidos > 3) {
            return 0.04;
        } else {
            return 0.0;
        }
    }
}
