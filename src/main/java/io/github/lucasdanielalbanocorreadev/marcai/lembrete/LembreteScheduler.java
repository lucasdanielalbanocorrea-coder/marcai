package io.github.lucasdanielalbanocorreadev.marcai.lembrete;

import io.github.lucasdanielalbanocorreadev.marcai.agendamento.Agendamento;
import io.github.lucasdanielalbanocorreadev.marcai.agendamento.AgendamentoService;
import io.github.lucasdanielalbanocorreadev.marcai.whatsapp.WhatsAppClient;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Envia, pelo WhatsApp, um lembrete para cada agendamento que começa nas próximas 24h.
 * O template precisa ter 3 variáveis no corpo: {{1}} nome do cliente, {{2}} serviço,
 * {{3}} data e hora — e pedir para responder 1 (confirmar) ou 2 (cancelar).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LembreteScheduler {

    private static final Duration ANTECEDENCIA = Duration.ofHours(24);
    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");
    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("dd/MM 'às' HH:mm");

    private final AgendamentoService agendamentoService;
    private final WhatsAppClient whatsAppClient;

    /** Intervalo em lembrete.intervalo: 5 min em produção, menor em dev para testar sem esperar. */
    @Scheduled(initialDelayString = "${lembrete.intervalo}", fixedDelayString = "${lembrete.intervalo}")
    public void enviarLembretes() {
        for (Agendamento agendamento : agendamentoService.pendentesDeLembrete(OffsetDateTime.now().plus(ANTECEDENCIA))) {
            try {
                whatsAppClient.enviarLembrete(agendamento.getCliente().getTelefone(), List.of(
                        agendamento.getCliente().getNome(),
                        agendamento.getServico().getNome(),
                        agendamento.getInicio().atZoneSameInstant(FUSO).format(FORMATO)));
                agendamentoService.marcarLembreteEnviado(agendamento.getId());
            } catch (RuntimeException ex) {
                // Não marca como enviado: tenta de novo na próxima execução.
                log.warn("Falha ao enviar lembrete do agendamento {}", agendamento.getId(), ex);
            }
        }
    }
}
