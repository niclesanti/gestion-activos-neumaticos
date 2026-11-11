package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.data.web.config.PageableHandlerMethodArgumentResolverCustomizer;
import org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode;

/**
 * Serializa las {@code Page} con el formato estable de Spring Data
 * ({@code content} + {@code page{size, number, totalElements, totalPages}}), en
 * lugar de exponer la implementación interna, y unifica el criterio de
 * paginación de todos los listados: 10 por defecto y 20 como máximo (un tamaño
 * mayor se acota en silencio, sin 400).
 *
 * <p>Con {@code @EnableSpringDataWebSupport} Spring Boot deja de aplicar las
 * propiedades {@code spring.data.web.pageable.*}, por eso se fija acá. Los
 * controladores usan {@link #TAMANIO_PAGINA_POR_DEFECTO} en su
 * {@code @PageableDefault}, que tiene precedencia sobre el pageable de reserva.
 * El frontend lo replica en {@code DEFAULT_PAGE_SIZE} ({@code lib/api/page.ts}).
 */
@Configuration
@EnableSpringDataWebSupport(pageSerializationMode = PageSerializationMode.VIA_DTO)
public class PaginacionConfig {

    public static final int TAMANIO_PAGINA_POR_DEFECTO = 10;
    public static final int TAMANIO_PAGINA_MAXIMO = 20;

    @Bean
    PageableHandlerMethodArgumentResolverCustomizer tamanioMaximoDePagina() {
        return resolver -> {
            resolver.setMaxPageSize(TAMANIO_PAGINA_MAXIMO);
            resolver.setFallbackPageable(PageRequest.of(0, TAMANIO_PAGINA_POR_DEFECTO));
        };
    }
}
