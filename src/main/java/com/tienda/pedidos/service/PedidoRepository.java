package com.tienda.pedidos.service;

import com.tienda.pedidos.dto.ItemPedido;
import com.tienda.pedidos.validacion.ContextoPedido;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;

@Repository
public class PedidoRepository {

    private final JdbcTemplate jdbcTemplate;

    public PedidoRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public Long guardar(ContextoPedido contexto, double descuento, double impuesto, double total) {
        String sqlPedido = "INSERT INTO pedidos (cliente_id, subtotal, descuento, impuesto, total, fecha, estado) " +
                           "VALUES (?, ?, ?, ?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sqlPedido, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, contexto.getPedidoRequest().getClienteId());
            ps.setDouble(2, contexto.getSubtotal());
            ps.setDouble(3, descuento);
            ps.setDouble(4, impuesto);
            ps.setDouble(5, total);
            ps.setTimestamp(6, Timestamp.from(Instant.now()));
            ps.setString(7, "CONFIRMADO");
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("No se pudo obtener el identificador generado para el pedido.");
        }
        Long pedidoId = key.longValue();

        // Registrar cada item en detalle_pedido y descontar unidades en inventario
        for (ItemPedido item : contexto.getPedidoRequest().getItems()) {
            jdbcTemplate.update(
                    "INSERT INTO detalle_pedido (pedido_id, producto_id, cantidad) VALUES (?, ?, ?)",
                    pedidoId, item.getProductoId(), item.getCantidad()
            );

            jdbcTemplate.update(
                    "UPDATE inventario SET stock = stock - ? WHERE producto_id = ?",
                    item.getCantidad(), item.getProductoId()
            );
        }

        return pedidoId;
    }
}
