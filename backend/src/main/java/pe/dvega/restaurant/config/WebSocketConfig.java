package pe.dvega.restaurant.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.Arrays;

/**
 * WebSocket (STOMP) para avisos en vivo. Los canales públicos (/topic/carta, /topic/mesa/*, /topic/seguimiento/*)
 * no piden sesión; /topic/staff solo acepta suscripciones con un JWT válido en el CONNECT.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    static final String CON_SESION = "conSesion";

    private final JwtDecoder jwt;
    private final String[] origenes;

    public WebSocketConfig(JwtDecoder jwt, @Value("${app.cors.origenes}") String origenes) {
        this.jwt = jwt;
        this.origenes = Arrays.stream(origenes.split(",")).map(String::trim).toArray(String[]::new);
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registro) {
        registro.addEndpoint("/ws").setAllowedOriginPatterns(origenes);
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registro) {
        registro.enableSimpleBroker("/topic");
        // Los clientes solo escuchan; no hay destinos de aplicación.
        registro.setApplicationDestinationPrefixes("/sin-uso");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registro) {
        registro.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> mensaje, MessageChannel canal) {
                StompHeaderAccessor h = MessageHeaderAccessor.getAccessor(mensaje, StompHeaderAccessor.class);
                if (h == null || h.getCommand() == null) {
                    return mensaje;
                }
                if (h.getCommand() == StompCommand.CONNECT) {
                    String auth = h.getFirstNativeHeader("Authorization");
                    boolean valido = false;
                    if (auth != null && auth.startsWith("Bearer ")) {
                        try {
                            jwt.decode(auth.substring(7));
                            valido = true;
                        } catch (JwtException ignorado) {
                            // Sin sesión válida: solo podrá escuchar canales públicos.
                        }
                    }
                    if (h.getSessionAttributes() != null) {
                        h.getSessionAttributes().put(CON_SESION, valido);
                    }
                }
                if (h.getCommand() == StompCommand.SEND) {
                    throw new IllegalArgumentException("Este canal es solo de lectura.");
                }
                if (h.getCommand() == StompCommand.SUBSCRIBE && h.getDestination() != null
                        && h.getDestination().startsWith("/topic/staff")) {
                    Object conSesion = h.getSessionAttributes() == null ? null : h.getSessionAttributes().get(CON_SESION);
                    if (!Boolean.TRUE.equals(conSesion)) {
                        throw new IllegalArgumentException("El canal del personal pide iniciar sesión.");
                    }
                }
                return mensaje;
            }
        });
    }
}
