package com.tienda.pedidos.descuento;

import com.tienda.pedidos.validacion.ContextoPedido;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CalculadorDescuentoFinal {

    private final SelectorEstrategiaDescuento selectorEstrategia;
    private final List<EstrategiaDescuento> estrategiasCampana;

    public CalculadorDescuentoFinal(SelectorEstrategiaDescuento selectorEstrategia,
                                   DescuentoBlackFriday descuentoBlackFriday,
                                   DescuentoCorporativo descuentoCorporativo,
                                   DescuentoVolumen descuentoVolumen) {
        this.selectorEstrategia = selectorEstrategia;
        this.estrategiasCampana = List.of(descuentoBlackFriday, descuentoCorporativo, descuentoVolumen);
    }

    public double calcularDescuentoMaximo(ContextoPedido contexto) {
        // Descuento según el tipo de cliente (VIP, FRECUENTE, ESTANDAR)
        EstrategiaDescuento estrategiaCliente = selectorEstrategia.obtenerEstrategia(contexto.getTipoCliente());
        double maxDescuento = estrategiaCliente.calcular(contexto);

        // Comparar con las campañas promocionales transversales y elegir el mayor beneficio
        for (EstrategiaDescuento campana : estrategiasCampana) {
            double descuentoCampana = campana.calcular(contexto);
            if (descuentoCampana > maxDescuento) {
                maxDescuento = descuentoCampana;
            }
        }

        return maxDescuento;
    }
}
