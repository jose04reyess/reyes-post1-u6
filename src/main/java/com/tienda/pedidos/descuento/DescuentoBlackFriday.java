package com.tienda.pedidos.descuento;

import com.tienda.pedidos.validacion.ContextoPedido;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component("descuentoBlackFriday")
public class DescuentoBlackFriday implements EstrategiaDescuento {

    private final boolean activa;

    public DescuentoBlackFriday(@Value("${promo.black-friday.activa:false}") boolean activa) {
        this.activa = activa;
    }

    @Override
    public double calcular(ContextoPedido contexto) {
        return activa ? 0.25 : 0.0;
    }

    public boolean isActiva() {
        return activa;
    }
}
