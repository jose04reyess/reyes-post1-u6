package com.tienda.pedidos.service;

import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class NotificacionPedidoService {

    private final EmailService emailService;

    public NotificacionPedidoService(EmailService emailService) {
        this.emailService = emailService;
    }

    public void notificarConfirmacion(Long pedidoId, String email, double total) {
        if (email == null || email.isBlank()) {
            return;
        }
        String asunto = "Confirmación de Pedido #" + pedidoId;
        String cuerpo = String.format(Locale.US,
                "Estimado cliente, su pedido #%d ha sido confirmado satisfactoriamente por un total de $%.2f.",
                pedidoId, total);
        emailService.enviar(email, asunto, cuerpo);
    }
}
