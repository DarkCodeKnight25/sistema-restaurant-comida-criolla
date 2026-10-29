package pe.dvega.restaurant.web;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.transaction.support.TransactionTemplate;
import pe.dvega.restaurant.RelojFijo;
import pe.dvega.restaurant.demo.DatosDemo;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
@Import(RelojFijo.class)
class ApiTests {

    @Autowired MockMvcTester mvc;
    @Autowired DatosDemo demo;
    @Autowired TransactionTemplate tx;

    @BeforeEach
    void sembrar() {
        tx.executeWithoutResult(s -> {
            demo.borrarTodo();
            demo.sembrar(20261029L);
        });
    }

    @AfterEach
    void limpiar() {
        tx.executeWithoutResult(s -> demo.borrarTodo());
    }

    @Test
    void laCartaYLaMesaSonPublicasPeroNoMuestranDatosDeClientes() {
        assertThat(mvc.get().uri("/api/publico/carta")).hasStatusOk()
                .bodyJson().extractingPath("$.platos.length()").isEqualTo(14);
        assertThat(mvc.get().uri("/api/publico/mesas/" + DatosDemo.token(9))).hasStatusOk()
                .bodyJson().extractingPath("$.estadoCuenta").isEqualTo("CUENTA");
        assertThat(mvc.get().uri("/api/publico/mesas/no-existe")).hasStatus(404);
    }

    @Test
    void unPlatoAgotadoDevuelve409ConSuId() throws Exception {
        String carta = mvc.get().uri("/api/publico/carta").exchange().getResponse().getContentAsString(StandardCharsets.UTF_8);
        java.util.List<Integer> ids = JsonPath.read(carta, "$.platos[?(@.nombre == 'Tallarín saltado')].id");
        Integer tallarin = ids.get(0);
        assertThat(mvc.post().uri("/api/publico/mesas/" + DatosDemo.token(1) + "/pedidos").contentType(MediaType.APPLICATION_JSON)
                .content("{\"lineas\":[{\"platoId\":" + tallarin + ",\"cantidad\":1}]}"))
                .hasStatus(409)
                .bodyJson().extractingPath("$.platosAgotados[0]").isEqualTo(tallarin);
    }

    @Test
    void elPanelPideSesionYRespetaLosRoles() throws Exception {
        assertThat(mvc.get().uri("/api/pedidos")).hasStatus(401);

        String cocina = token("cocina@donameche.demo");
        assertThat(mvc.get().uri("/api/pedidos").header("Authorization", "Bearer " + cocina)).hasStatusOk();
        assertThat(mvc.get().uri("/api/caja/resumen").header("Authorization", "Bearer " + cocina)).hasStatus(403);

        String caja = token("caja@donameche.demo");
        assertThat(mvc.get().uri("/api/caja/resumen").header("Authorization", "Bearer " + caja)).hasStatusOk()
                .bodyJson().extractingPath("$.tickets").isEqualTo(15);
    }

    @Test
    void unPedidoConCantidadInvalidaDevuelveErroresPorCampo() {
        assertThat(mvc.post().uri("/api/publico/mesas/" + DatosDemo.token(1) + "/pedidos").contentType(MediaType.APPLICATION_JSON)
                .content("{\"lineas\":[{\"platoId\":1,\"cantidad\":0}]}"))
                .hasStatus(400);
    }

    private String token(String email) throws Exception {
        var r = mvc.post().uri("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"" + DatosDemo.PASSWORD_DEMO + "\"}").exchange();
        assertThat(r).hasStatusOk();
        return JsonPath.read(r.getResponse().getContentAsString(StandardCharsets.UTF_8), "$.token");
    }
}
