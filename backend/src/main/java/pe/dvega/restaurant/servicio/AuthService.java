package pe.dvega.restaurant.servicio;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.dvega.restaurant.dominio.Usuario;
import pe.dvega.restaurant.repositorio.UsuarioRepositorio;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

@Service
public class AuthService {

    /** Una jornada de recepción completa sin volver a iniciar sesión. */
    static final Duration VIGENCIA = Duration.ofHours(12);

    private final UsuarioRepositorio usuarios;
    private final PasswordEncoder passwords;
    private final JwtEncoder jwt;
    private final Clock reloj;

    public AuthService(UsuarioRepositorio usuarios, PasswordEncoder passwords, JwtEncoder jwt, Clock reloj) {
        this.usuarios = usuarios;
        this.passwords = passwords;
        this.jwt = jwt;
        this.reloj = reloj;
    }

    public record Sesion(String token, Instant expira, Usuario usuario) {
    }

    @Transactional(readOnly = true)
    public Sesion login(String email, String password) {
        Usuario u = usuarios.findByEmailIgnoreCase(email.trim())
                .filter(x -> passwords.matches(password, x.getPasswordHash()))
                .orElseThrow(() -> new BadCredentialsException("Correo o contraseña incorrectos."));
        Instant ahora = reloj.instant();
        Instant expira = ahora.plus(VIGENCIA);
        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .issuer("restaurant")
                .subject(u.getEmail())
                .issuedAt(ahora)
                .expiresAt(expira)
                .claim("nombre", u.getNombre())
                .claim("rol", u.getRol().name());
        String token = jwt.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims.build()))
                .getTokenValue();
        return new Sesion(token, expira, u);
    }

    @Transactional(readOnly = true)
    public Usuario actual(String email) {
        return usuarios.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new BadCredentialsException("La sesión ya no es válida."));
    }
}
