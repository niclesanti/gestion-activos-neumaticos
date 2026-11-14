package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalManagementPort;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestcontainersConfiguration;

/**
 * Actuator vive en un puerto de gestión aparte: solo health y prometheus, sin
 * token, y nada de eso se ve por el puerto de la API.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, properties = "management.prometheus.metrics.export.enabled=true")
@Import(TestcontainersConfiguration.class)
class ActuatorGestionTest {

	/**
	 * Un puerto fijo distinto del de la API: con ambos en 0 (aleatorio) Spring Boot
	 * los toma como el mismo puerto y el matcher de actuator no distingue contextos.
	 */
	@DynamicPropertySource
	static void puertoDeGestion(DynamicPropertyRegistry registry) throws IOException {
		try (ServerSocket libre = new ServerSocket(0)) {
			final int puerto = libre.getLocalPort();
			registry.add("management.server.port", () -> puerto);
		}
	}

	@LocalManagementPort
	int puertoGestion;

	@LocalServerPort
	int puertoApi;

	private HttpResponse<String> get(int puerto, String ruta) throws Exception {
		HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + puerto + ruta)).GET().build();
		return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
	}

	@Test
	void prometheusYHealthSonPublicosEnElPuertoDeGestion() throws Exception {
		HttpResponse<String> prometheus = get(puertoGestion, "/actuator/prometheus");
		assertThat(prometheus.statusCode()).isEqualTo(200);
		assertThat(prometheus.body()).contains("application=\"gestion-neumaticos-local\"");

		HttpResponse<String> salud = get(puertoGestion, "/actuator/health");
		assertThat(salud.statusCode()).isEqualTo(200);
		assertThat(salud.body()).contains("\"status\":\"UP\"").doesNotContain("components");
	}

	@Test
	void otrosEndpointsNoEstanExpuestos() throws Exception {
		for (String ruta : new String[] { "/actuator/env", "/actuator/heapdump", "/actuator/loggers", "/actuator/info" }) {
			assertThat(get(puertoGestion, ruta).statusCode()).as(ruta).isEqualTo(404);
		}
	}

	@Test
	void actuatorNoSeSirvePorElPuertoDeLaApi() throws Exception {
		assertThat(get(puertoApi, "/actuator/prometheus").statusCode()).isEqualTo(404);
		assertThat(get(puertoApi, "/actuator/health").statusCode()).isEqualTo(404);
	}

	@Test
	void laApiSigueExigiendoToken() throws Exception {
		assertThat(get(puertoApi, "/api/v1/neumaticos").statusCode()).isEqualTo(401);
	}

}
