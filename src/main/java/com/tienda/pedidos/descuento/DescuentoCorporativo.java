package com.tienda.pedidos.descuento;

import com.tienda.pedidos.validacion.ContextoPedido;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component("descuentoCorporativo")
public class DescuentoCorporativo implements EstrategiaDescuento {

    private final JdbcTemplate jdbcTemplate;

    public DescuentoCorporativo(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public double calcular(ContextoPedido contexto) {
        String nit = contexto.getNit();

        if (nit == null && contexto.getPedidoRequest() != null && contexto.getPedidoRequest().getClienteId() != null) {
            nit = jdbcTemplate.query(
                    "SELECT nit FROM clientes WHERE id = ?",
                    rs -> rs.next() ? rs.getString("nit") : null,
                    contexto.getPedidoRequest().getClienteId()
            );
            contexto.setNit(nit);
        }

        if (nit != null && !nit.trim().isEmpty()) {
            return 0.10;
        }

        return 0.0;
    }
}
