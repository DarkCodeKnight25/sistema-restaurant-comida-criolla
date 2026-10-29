package pe.dvega.restaurant.servicio;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Map;

/**
 * Avisos en vivo por WebSocket. Se envían después del commit: así, cuando la pantalla de cocina
 * o el celular del comensal vuelven a pedir los datos, ya encuentran el cambio guardado.
 */
@Component
public class Eventos {

    public enum Tipo { PEDIDO, ITEM, MESA, CARTA, PAGO }

    /** mesaToken y codigo son opcionales: dicen a qué comensal o seguimiento también avisar. */
    public record Cambio(Tipo tipo, Long id, String mesaToken, String codigo) {
    }

    private final ApplicationEventPublisher publicador;
    private final SimpMessagingTemplate ws;

    public Eventos(ApplicationEventPublisher publicador, SimpMessagingTemplate ws) {
        this.publicador = publicador;
        this.ws = ws;
    }

    public void avisar(Tipo tipo, Long id, String mesaToken, String codigo) {
        publicador.publishEvent(new Cambio(tipo, id, mesaToken, codigo));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    void enviar(Cambio c) {
        Object cuerpo = Map.of("tipo", c.tipo().name(), "id", c.id() == null ? 0 : c.id());
        ws.convertAndSend("/topic/staff", cuerpo);
        if (c.tipo() == Tipo.CARTA) {
            ws.convertAndSend("/topic/carta", cuerpo);
        }
        if (c.mesaToken() != null) {
            ws.convertAndSend("/topic/mesa/" + c.mesaToken(), cuerpo);
        }
        if (c.codigo() != null) {
            ws.convertAndSend("/topic/seguimiento/" + c.codigo(), cuerpo);
        }
    }
}
