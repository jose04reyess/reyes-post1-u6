package com.tienda.pedidos;

import com.tienda.pedidos.descuento.*;
import com.tienda.pedidos.dto.ItemPedido;
import com.tienda.pedidos.dto.PedidoRequest;
import com.tienda.pedidos.dto.ResultadoPedido;
import com.tienda.pedidos.service.GestorPedidos;
import com.tienda.pedidos.validacion.ContextoPedido;
import com.tienda.pedidos.validacion.ValidadorCliente;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class GestorPedidosTest {

    @Autowired
    private GestorPedidos gestorPedidos;

    @Autowired
    private ValidadorCliente validadorCliente;

    @Autowired
    private DescuentoVip descuentoVip;

    @Autowired
    private DescuentoFrecuente descuentoFrecuente;

    @Autowired
    private DescuentoCorporativo descuentoCorporativo;

    @Autowired
    private DescuentoVolumen descuentoVolumen;

    @Autowired
    private DescuentoBlackFriday descuentoBlackFriday;

    @Autowired
    private CalculadorDescuentoFinal calculadorDescuentoFinal;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        // Fijar el reloj por defecto a las 10:00 AM (horario laboral)
        Clock clockFijo = Clock.fixed(Instant.parse("2026-10-06T10:00:00Z"), ZoneId.of("UTC"));
        validadorCliente.setClock(clockFijo);
    }

    @Test
    @DisplayName("1. Rechazo por stock insuficiente (Corte anticipado en ValidadorStock)")
    void testRechazoPorStockInsuficiente() {
        // Producto 102 solo tiene 5 unidades en inventario
        PedidoRequest request = new PedidoRequest(
                1L,
                "vip@cliente.com",
                List.of(new ItemPedido(102L, 10)) // Solicita 10 unidades
        );

        ResultadoPedido resultado = gestorPedidos.procesarPedido(request);

        assertNotNull(resultado);
        assertFalse(resultado.isConfirmado(), "El pedido debería ser rechazado por stock insuficiente");
        assertNotNull(resultado.getMotivoRechazo());
        assertTrue(resultado.getMotivoRechazo().contains("Stock insuficiente para el producto: 102"),
                "El mensaje debe especificar el producto sin stock");
        assertNull(resultado.getPedidoId(), "No se debió generar un identificador de pedido");
    }

    @Test
    @DisplayName("2. Rechazo por cliente inexistente (Corte anticipado en ValidadorCliente)")
    void testRechazoPorClienteInexistente() {
        // Cliente 999 no existe en la base de datos
        PedidoRequest request = new PedidoRequest(
                999L,
                "fantasma@correo.com",
                List.of(new ItemPedido(101L, 2))
        );

        ResultadoPedido resultado = gestorPedidos.procesarPedido(request);

        assertNotNull(resultado);
        assertFalse(resultado.isConfirmado(), "El pedido debería ser rechazado por cliente inexistente");
        assertEquals("El cliente no existe", resultado.getMotivoRechazo());
        assertNull(resultado.getPedidoId());
    }

    @Test
    @DisplayName("3. Rechazo por cliente moroso en horario laboral")
    void testRechazoPorClienteMorosoEnHorarioLaboral() {
        // Cliente 3 es MOROSO y tiene una factura pendiente de $150,000
        // Reloj fijado a las 14:00 (horario laboral < 20:00)
        validadorCliente.setClock(Clock.fixed(Instant.parse("2026-10-06T14:00:00Z"), ZoneId.of("UTC")));

        PedidoRequest request = new PedidoRequest(
                3L,
                "moroso@correo.com",
                List.of(new ItemPedido(101L, 1))
        );

        ResultadoPedido resultado = gestorPedidos.procesarPedido(request);

        assertNotNull(resultado);
        assertFalse(resultado.isConfirmado(), "El cliente moroso debe ser rechazado en horario laboral");
        assertEquals("Cliente moroso con facturas pendientes", resultado.getMotivoRechazo());
    }

    @Test
    @DisplayName("4. Confirmación de pedido VIP con escala de descuento (15%, 10%, 5%)")
    void testConfirmacionPedidoVipConEscalaDescuento() {
        // Escala > 1M (15%)
        ContextoPedido ctxAlto = new ContextoPedido(new PedidoRequest(1L, "vip@tienda.com", List.of()));
        ctxAlto.setSubtotal(1_200_000.0);
        assertEquals(0.15, descuentoVip.calcular(ctxAlto), 0.001);

        // Escala > 500k (10%)
        ContextoPedido ctxMedio = new ContextoPedido(new PedidoRequest(1L, "vip@tienda.com", List.of()));
        ctxMedio.setSubtotal(750_000.0);
        assertEquals(0.10, descuentoVip.calcular(ctxMedio), 0.001);

        // Escala base VIP <= 500k (5%)
        ContextoPedido ctxBase = new ContextoPedido(new PedidoRequest(1L, "vip@tienda.com", List.of()));
        ctxBase.setSubtotal(200_000.0);
        assertEquals(0.05, descuentoVip.calcular(ctxBase), 0.001);

        // Procesamiento de pedido VIP exitoso
        PedidoRequest request = new PedidoRequest(
                1L,
                "vip@tienda.com",
                List.of(new ItemPedido(101L, 5)) // 5 * 10,000 = 50,000
        );

        ResultadoPedido resultado = gestorPedidos.procesarPedido(request);
        assertNotNull(resultado);
        assertTrue(resultado.isConfirmado(), "El pedido VIP debe ser confirmado");
        assertNotNull(resultado.getPedidoId());
        assertTrue(resultado.getTotal() > 0);
    }

    @Test
    @DisplayName("5. Aplicación de Campañas Promocionales (Black Friday, Corporativo, Volumen)")
    void testAplicacionCampanasPromocionales() {
        // A. Black Friday activo (25%)
        assertTrue(descuentoBlackFriday.isActiva());
        ContextoPedido ctxBF = new ContextoPedido(new PedidoRequest(4L, "estandar@tienda.com", List.of()));
        assertEquals(0.25, descuentoBlackFriday.calcular(ctxBF), 0.001);

        // B. Descuento Corporativo con NIT (10%)
        PedidoRequest reqCorp = new PedidoRequest(5L, "corp@empresa.com", List.of(new ItemPedido(101L, 2)));
        ContextoPedido ctxCorp = new ContextoPedido(reqCorp);
        ctxCorp.setNit("900123456-1");
        assertEquals(0.10, descuentoCorporativo.calcular(ctxCorp), 0.001);

        // Corporativo sin NIT no aplica (0%)
        ContextoPedido ctxSinNit = new ContextoPedido(new PedidoRequest(4L, "sin@nit.com", List.of()));
        assertEquals(0.0, descuentoCorporativo.calcular(ctxSinNit), 0.001);

        // C. Descuento por Volumen > 20 unidades (12%)
        PedidoRequest reqVolumen = new PedidoRequest(4L, "mayorista@tienda.com",
                List.of(new ItemPedido(101L, 15), new ItemPedido(103L, 10))); // 25 unidades
        ContextoPedido ctxVolumen = new ContextoPedido(reqVolumen);
        assertEquals(0.12, descuentoVolumen.calcular(ctxVolumen), 0.001);

        // Volumen <= 20 unidades (0%)
        PedidoRequest reqPoco = new PedidoRequest(4L, "minorista@tienda.com",
                List.of(new ItemPedido(101L, 5)));
        ContextoPedido ctxPoco = new ContextoPedido(reqPoco);
        assertEquals(0.0, descuentoVolumen.calcular(ctxPoco), 0.001);
    }

    @Test
    @DisplayName("6. Descuento para cliente FRECUENTE con historial de pedidos (>10 pedidos -> 8%)")
    void testDescuentoClienteFrecuente() {
        // Cliente 2 tiene 15 pedidos previos en data.sql (> 10 -> 8%)
        PedidoRequest request = new PedidoRequest(2L, "frecuente@tienda.com", List.of(new ItemPedido(101L, 1)));
        ContextoPedido contexto = new ContextoPedido(request);

        double desc = descuentoFrecuente.calcular(contexto);
        assertEquals(0.08, desc, 0.001, "Cliente con 15 pedidos previos debe obtener 8% de descuento");
    }

    @Test
    @DisplayName("7. CalculadorDescuentoFinal selecciona el mayor beneficio disponible (Math.max)")
    void testCalculadorDescuentoFinalSeleccionaElMayor() {
        // Cliente VIP (subtotal 200k -> 5%), NIT corporativo (10%), volumen 25 unidades (12%), Black Friday (25%)
        PedidoRequest request = new PedidoRequest(
                5L,
                "corp@tienda.com",
                List.of(new ItemPedido(101L, 25))
        );
        ContextoPedido ctx = new ContextoPedido(request);
        ctx.setTipoCliente("VIP");
        ctx.setSubtotal(250_000.0);
        ctx.setNit("900123456-1");

        // El calculador debe elegir 25% (Black Friday) frente al 12% de volumen, 10% corporativo y 5% VIP
        double maxDescuento = calculadorDescuentoFinal.calcularDescuentoMaximo(ctx);
        assertEquals(0.25, maxDescuento, 0.001, "Debe seleccionar el mayor porcentaje entre todas las promociones aplicables");
    }

    @Test
    @DisplayName("8. Rechazo cuando el pedido no tiene items (Corte anticipado)")
    void testRechazoPedidoSinItems() {
        PedidoRequest request = new PedidoRequest(1L, "vip@tienda.com", List.of());
        ResultadoPedido resultado = gestorPedidos.procesarPedido(request);

        assertNotNull(resultado);
        assertFalse(resultado.isConfirmado());
        assertEquals("El pedido no contiene productos", resultado.getMotivoRechazo());
    }

    @Test
    @DisplayName("9. Descuento de stock en inventario tras confirmación de pedido")
    void testDescuentoDeStockEnInventario() {
        // Stock inicial de producto 103 es 50
        Integer stockAntes = jdbcTemplate.queryForObject(
                "SELECT stock FROM inventario WHERE producto_id = 103",
                Integer.class
        );
        assertNotNull(stockAntes);

        PedidoRequest request = new PedidoRequest(
                1L,
                "vip@tienda.com",
                List.of(new ItemPedido(103L, 5))
        );

        ResultadoPedido resultado = gestorPedidos.procesarPedido(request);
        assertTrue(resultado.isConfirmado());

        Integer stockDespues = jdbcTemplate.queryForObject(
                "SELECT stock FROM inventario WHERE producto_id = 103",
                Integer.class
        );
        assertNotNull(stockDespues);
        assertEquals(stockAntes - 5, stockDespues.intValue(), "El stock debe descontarse exactamente en 5 unidades");
    }
}
