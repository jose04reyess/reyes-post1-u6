package com.tienda.pedidos.validacion;

import com.tienda.pedidos.dto.ItemPedido;
import com.tienda.pedidos.dto.PedidoRequest;

public class ContextoPedido {
    private final PedidoRequest pedidoRequest;
    private String tipoCliente;
    private String nit;
    private double subtotal;
    private boolean rechazado;
    private String motivoRechazo;

    public ContextoPedido(PedidoRequest pedidoRequest) {
        this.pedidoRequest = pedidoRequest;
        this.rechazado = false;
        this.subtotal = 0.0;
    }

    public PedidoRequest getPedidoRequest() {
        return pedidoRequest;
    }

    public String getTipoCliente() {
        return tipoCliente;
    }

    public void setTipoCliente(String tipoCliente) {
        this.tipoCliente = tipoCliente;
    }

    public String getNit() {
        return nit;
    }

    public void setNit(String nit) {
        this.nit = nit;
    }

    public double getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(double subtotal) {
        this.subtotal = subtotal;
    }

    public boolean isRechazado() {
        return rechazado;
    }

    public void setRechazado(boolean rechazado) {
        this.rechazado = rechazado;
    }

    public String getMotivoRechazo() {
        return motivoRechazo;
    }

    public void setMotivoRechazo(String motivoRechazo) {
        this.motivoRechazo = motivoRechazo;
    }

    public void rechazar(String motivo) {
        this.rechazado = true;
        this.motivoRechazo = motivo;
    }

    public int getTotalUnidades() {
        if (pedidoRequest == null || pedidoRequest.getItems() == null) {
            return 0;
        }
        return pedidoRequest.getItems().stream()
                .mapToInt(ItemPedido::getCantidad)
                .sum();
    }
}
