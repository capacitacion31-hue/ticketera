package com.example.ticketero.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.test.context.TestPropertySource;

import java.lang.reflect.Modifier;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestPropertySource(properties = {
    "spring.task.scheduling.pool.size=2",
    "spring.task.execution.pool.core-size=2"
})
@DisplayName("SchedulingConfig Tests")
class SchedulingConfigTest {

    @Nested
    @DisplayName("Configuración de Anotaciones")
    class ConfiguracionAnotaciones {

        @Test
        @DisplayName("Debe tener anotación @Configuration")
        void debeEstarAnotadaComoConfiguration() {
            assertThat(SchedulingConfig.class.isAnnotationPresent(Configuration.class))
                .isTrue();
        }

        @Test
        @DisplayName("Debe tener anotación @EnableScheduling")
        void debeHabilitarScheduling() {
            assertThat(SchedulingConfig.class.isAnnotationPresent(EnableScheduling.class))
                .isTrue();
        }

        @Test
        @DisplayName("Debe tener anotación @EnableAsync")
        void debeHabilitarAsync() {
            assertThat(SchedulingConfig.class.isAnnotationPresent(EnableAsync.class))
                .isTrue();
        }
    }

    @Nested
    @DisplayName("Instanciación de Configuración")
    class InstanciacionConfiguracion {

        @Test
        @DisplayName("Debe poder crear instancia de SchedulingConfig")
        void debePoderCrearInstancia() {
            SchedulingConfig config = new SchedulingConfig();
            assertThat(config).isNotNull();
        }

        @Test
        @DisplayName("Debe ser una clase de configuración válida")
        void debeSerClaseConfiguracionValida() {
            assertThat(SchedulingConfig.class)
                .isNotNull()
                .satisfies(clazz -> {
                    assertThat(clazz.isInterface()).isFalse();
                    assertThat(Modifier.isAbstract(clazz.getModifiers())).isFalse();
                });
        }
    }
}