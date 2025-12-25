package com.example.ticketero.model.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MessageTemplate Enum Tests")
class MessageTemplateTest {

    @Nested
    @DisplayName("Valores del Enum")
    class ValoresDelEnum {

        @Test
        @DisplayName("Debe tener todos los valores esperados")
        void debeTenerTodosLosValoresEsperados() {
            // When
            MessageTemplate[] values = MessageTemplate.values();

            // Then
            assertThat(values).hasSize(3);
            assertThat(values).containsExactly(
                MessageTemplate.TOTEM_TICKET_CREADO,
                MessageTemplate.TOTEM_PROXIMO_TURNO,
                MessageTemplate.TOTEM_ES_TU_TURNO
            );
        }
    }

    @Nested
    @DisplayName("Propiedades")
    class Propiedades {

        @Test
        @DisplayName("TOTEM_TICKET_CREADO debe tener propiedades correctas")
        void totemTicketCreadoDebeTenerPropiedadesCorrectas() {
            // When
            MessageTemplate template = MessageTemplate.TOTEM_TICKET_CREADO;

            // Then
            assertThat(template.getDescription()).isEqualTo("Confirmación de creación");
            assertThat(template.getTriggerMoment()).isEqualTo("INMEDIATO");
        }

        @Test
        @DisplayName("TOTEM_PROXIMO_TURNO debe tener propiedades correctas")
        void totemProximoTurnoDebeTenerPropiedadesCorrectas() {
            // When
            MessageTemplate template = MessageTemplate.TOTEM_PROXIMO_TURNO;

            // Then
            assertThat(template.getDescription()).isEqualTo("Pre-aviso");
            assertThat(template.getTriggerMoment()).isEqualTo("CUANDO_POSICION_3");
        }

        @Test
        @DisplayName("TOTEM_ES_TU_TURNO debe tener propiedades correctas")
        void totemEsTuTurnoDebeTenerPropiedadesCorrectas() {
            // When
            MessageTemplate template = MessageTemplate.TOTEM_ES_TU_TURNO;

            // Then
            assertThat(template.getDescription()).isEqualTo("Turno activo");
            assertThat(template.getTriggerMoment()).isEqualTo("AL_ASIGNAR");
        }
    }

    @Nested
    @DisplayName("Momentos de Activación")
    class MomentosDeActivacion {

        @Test
        @DisplayName("Debe tener momentos de activación únicos")
        void debeTenerMomentosDeActivacionUnicos() {
            // When
            String[] triggerMoments = {
                MessageTemplate.TOTEM_TICKET_CREADO.getTriggerMoment(),
                MessageTemplate.TOTEM_PROXIMO_TURNO.getTriggerMoment(),
                MessageTemplate.TOTEM_ES_TU_TURNO.getTriggerMoment()
            };

            // Then
            assertThat(triggerMoments).containsExactly(
                "INMEDIATO",
                "CUANDO_POSICION_3",
                "AL_ASIGNAR"
            );
        }
    }
}