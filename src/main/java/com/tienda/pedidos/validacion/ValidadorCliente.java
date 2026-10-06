package com.tienda.pedidos.validacion;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalTime;

@Component
public class ValidadorCliente extends ValidadorPedido {

    private final JdbcTemplate jdbcTemplate;
    private Clock clock = Clock.systemDefaultZone();

    public ValidadorCliente(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void setClock(Clock clock) {
        this.clock = clock;
    }

    @Override
    protected void procesar(ContextoPedido contexto) {
        if (contexto.getPedidoRequest() == null || contexto.getPedidoRequest().getClienteId() == null) {
            contexto.rechazar("El cliente no existe");
            return;
        }

        Long clienteId = contexto.getPedidoRequest().getClienteId();

        // Validar existencia de cliente en base de datos
        var datosCliente = jdbcTemplate.query(
                "SELECT tipo_cliente, nit FROM clientes WHERE id = ?",
                rs -> {
                    if (rs.next()) {
                        return new String[]{rs.getString("tipo_cliente"), rs.getString("nit")};
                    }
                    return null;
                },
                clienteId
        );

        if (datosCliente == null) {
            contexto.rechazar("El cliente no existe");
            return;
        }

        String tipoCliente = datosCliente[0];
        String nit = datosCliente[1];
        contexto.setTipoCliente(tipoCliente);
        contexto.setNit(nit);

        // Validar morosidad si el cliente es de tipo MOROSO
        if ("MOROSO".equalsIgnoreCase(tipoCliente)) {
            Double deudaPendiente = jdbcTemplate.query(
                    "SELECT COALESCE(SUM(monto), 0) FROM facturas WHERE cliente_id = ? AND pagada = false",
                    rs -> rs.next() ? rs.getDouble(1) : 0.0,
                    clienteId
            );

            LocalTime horaActual = LocalTime.now(clock);
            // Horario laboral: antes de las 20:00 (8:00 PM)
            if (deudaPendiente != null && deudaPendiente > 0 && horaActual.isBefore(LocalTime.of(20, 0))) {
                contexto.rechazar("Cliente moroso con facturas pendientes");
                return;
            }
        }
    }
}
