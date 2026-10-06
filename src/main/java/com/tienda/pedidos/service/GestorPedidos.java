package com.tienda.pedidos.service;

import com.tienda.pedidos.descuento.CalculadorDescuentoFinal;
import com.tienda.pedidos.dto.PedidoRequest;
import com.tienda.pedidos.dto.ResultadoPedido;
import com.tienda.pedidos.validacion.ContextoPedido;
import com.tienda.pedidos.validacion.ValidadorCliente;
import com.tienda.pedidos.validacion.ValidadorStock;
import org.springframework.stereotype.Service;

@Service
public class GestorPedidos {

    private final ValidadorStock validadorStock;
    private final ValidadorCliente validadorCliente;
    private final CalculadorDescuentoFinal calculadorDescuentoFinal;
    private final PedidoRepository pedidoRepository;
    private final NotificacionPedidoService notificacionPedidoService;

    public GestorPedidos(ValidadorStock validadorStock,
                         ValidadorCliente validadorCliente,
                         CalculadorDescuentoFinal calculadorDescuentoFinal,
                         PedidoRepository pedidoRepository,
                         NotificacionPedidoService notificacionPedidoService) {
        this.validadorStock = validadorStock;
        this.validadorCliente = validadorCliente;
        this.calculadorDescuentoFinal = calculadorDescuentoFinal;
        this.pedidoRepository = pedidoRepository;
        this.notificacionPedidoService = notificacionPedidoService;
    }

    public ResultadoPedido procesarPedido(PedidoRequest request) {
        ContextoPedido contexto = new ContextoPedido(request);

        // Chain of Responsibility: Validaciones secuenciales con corte anticipado
        validadorStock.encadenar(validadorCliente);
        validadorStock.validar(contexto);

        if (contexto.isRechazado()) {
            return ResultadoPedido.rechazado(contexto.getMotivoRechazo());
        }

        // Strategy: Cálculo desacoplado del porcentaje de descuento óptimo
        double subtotal = contexto.getSubtotal();
        double porcentajeDescuento = calculadorDescuentoFinal.calcularDescuentoMaximo(contexto);
        double montoDescuento = subtotal * porcentajeDescuento;
        double baseImponible = subtotal - montoDescuento;
        double impuesto = baseImponible * 0.19; // 19% IVA sobre base imponible
        double total = baseImponible + impuesto;

        // Persistencia transaccional
        Long pedidoId = pedidoRepository.guardar(contexto, montoDescuento, impuesto, total);

        // Notificación desacoplada
        notificacionPedidoService.notificarConfirmacion(pedidoId, request.getClienteEmail(), total);

        return ResultadoPedido.confirmado(pedidoId, total);
    }
}
