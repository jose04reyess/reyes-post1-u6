package com.tienda.pedidos.validacion;

public abstract class ValidadorPedido {

    protected ValidadorPedido siguiente;

    public ValidadorPedido encadenar(ValidadorPedido siguiente) {
        this.siguiente = siguiente;
        return siguiente;
    }

    public final void validar(ContextoPedido contexto) {
        procesar(contexto);
        if (!contexto.isRechazado() && siguiente != null) {
            siguiente.validar(contexto);
        }
    }

    protected abstract void procesar(ContextoPedido contexto);
}
