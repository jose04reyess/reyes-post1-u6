package com.tienda.pedidos.descuento;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class SelectorEstrategiaDescuento {

    private final Map<String, EstrategiaDescuento> estrategias = new HashMap<>();
    private final DescuentoEstandar descuentoEstandar;

    public SelectorEstrategiaDescuento(DescuentoVip descuentoVip,
                                      DescuentoFrecuente descuentoFrecuente,
                                      DescuentoEstandar descuentoEstandar) {
        this.descuentoEstandar = descuentoEstandar;
        estrategias.put("VIP", descuentoVip);
        estrategias.put("FRECUENTE", descuentoFrecuente);
        estrategias.put("ESTANDAR", descuentoEstandar);
    }

    public EstrategiaDescuento obtenerEstrategia(String tipoCliente) {
        if (tipoCliente == null) {
            return descuentoEstandar;
        }
        return estrategias.getOrDefault(tipoCliente.trim().toUpperCase(), descuentoEstandar);
    }
}
