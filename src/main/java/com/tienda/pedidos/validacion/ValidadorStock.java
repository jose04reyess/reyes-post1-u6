package com.tienda.pedidos.validacion;

import com.tienda.pedidos.dto.ItemPedido;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ValidadorStock extends ValidadorPedido {

    private final JdbcTemplate jdbcTemplate;

    public ValidadorStock(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    protected void procesar(ContextoPedido contexto) {
        if (contexto.getPedidoRequest() == null) {
            contexto.rechazar("El pedido no contiene información");
            return;
        }

        List<ItemPedido> items = contexto.getPedidoRequest().getItems();
        if (items == null || items.isEmpty()) {
            contexto.rechazar("El pedido no contiene productos");
            return;
        }

        double subtotalAcumulado = 0.0;

        for (ItemPedido item : items) {
            if (item == null || item.getProductoId() == null) {
                contexto.rechazar("Identificador de producto inválido");
                return;
            }

            if (item.getCantidad() <= 0) {
                contexto.rechazar("Cantidad inválida para el producto: " + item.getProductoId());
                return;
            }

            // Consultar existencia y stock en inventario
            Integer stockDisponible = jdbcTemplate.query(
                    "SELECT stock FROM inventario WHERE producto_id = ?",
                    rs -> rs.next() ? rs.getInt("stock") : null,
                    item.getProductoId()
            );

            if (stockDisponible == null || stockDisponible < item.getCantidad()) {
                contexto.rechazar("Stock insuficiente para el producto: " + item.getProductoId());
                return; // Corte anticipado
            }

            // Consultar precio del producto
            Double precio = jdbcTemplate.query(
                    "SELECT precio FROM productos WHERE id = ?",
                    rs -> rs.next() ? rs.getDouble("precio") : null,
                    item.getProductoId()
            );

            if (precio == null) {
                contexto.rechazar("El producto no existe: " + item.getProductoId());
                return;
            }

            subtotalAcumulado += precio * item.getCantidad();
        }

        contexto.setSubtotal(subtotalAcumulado);
    }
}
