package com.tienda.pedidos.descuento;

import com.tienda.pedidos.validacion.ContextoPedido;
import org.springframework.stereotype.Component;

@Component("descuentoVolumen")
public class DescuentoVolumen implements EstrategiaDescuento {

    @Override
    public double calcular(ContextoPedido contexto) {
        int totalUnidades = contexto.getTotalUnidades();
        if (totalUnidades > 20) {
            return 0.12;
        }
        return 0.0;
    }
}
