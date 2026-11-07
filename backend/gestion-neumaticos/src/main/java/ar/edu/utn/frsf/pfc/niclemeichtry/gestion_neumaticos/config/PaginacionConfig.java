package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.data.web.config.PageableHandlerMethodArgumentResolverCustomizer;
import org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode;

/**
 * Serializa las {@code Page} con el formato estable de Spring Data
 * ({@code content} + {@code page{size, number, totalElements, totalPages}}), en
 * lugar de exponer la implementación interna, y acota el tamaño de página.
 *
 * <p>Con {@code @EnableSpringDataWebSupport} Spring Boot deja de aplicar las
 * propiedades {@code spring.data.web.pageable.*}, por eso el tope se fija acá.
 */
@Configuration
@EnableSpringDataWebSupport(pageSerializationMode = PageSerializationMode.VIA_DTO)
public class PaginacionConfig {

    @Bean
    PageableHandlerMethodArgumentResolverCustomizer tamanioMaximoDePagina() {
        final int tamanioMaximoPagina = 100;
        return resolver -> resolver.setMaxPageSize(tamanioMaximoPagina);
    }
}
