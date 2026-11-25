package io.github.lucasdanielalbanocorreadev.marcai.whatsapp.webhook;

import io.github.lucasdanielalbanocorreadev.marcai.agendamento.AgendamentoService;
import io.github.lucasdanielalbanocorreadev.marcai.whatsapp.EventoProcessadoRepository;
import io.github.lucasdanielalbanocorreadev.marcai.whatsapp.webhook.dto.WebhookPayload;
import java.text.Normalizer;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Interpreta as respostas dos clientes ao lembrete ("1"/"sim" confirma, "2"/"não" cancela). */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookService {

    private static final Set<String> CONFIRMACOES = Set.of("1", "sim", "s", "ok", "confirmar", "confirmo", "confirmado");
    private static final Set<String> CANCELAMENTOS = Set.of("2", "nao", "n", "cancelar", "cancelo", "cancela");

    private final EventoProcessadoRepository eventos;
    private final AgendamentoService agendamentoService;

    /**
     * Tudo numa transação: se algo falhar, o registro do evento também é desfeito,
     * a requisição responde 5xx e a Meta reenvia.
     */
    @Transactional
    public void processar(WebhookPayload payload) {
        payload.mensagens().forEach(mensagem -> {
            if (mensagem.id() == null || eventos.registrar(mensagem.id()) == 0) {
                return; // sem id ou já processada
            }
            interpretar(mensagem.conteudo()).ifPresent(confirmou -> {
                boolean aplicado = agendamentoService.registrarRespostaAoLembrete(mensagem.from(), confirmou);
                log.info("Resposta {} de {} (mensagem {}): {}", confirmou ? "confirmação" : "cancelamento",
                        mascarar(mensagem.from()), mensagem.id(), aplicado ? "aplicada" : "nenhum agendamento aguardando resposta");
            });
        });
    }

    /** Telefone é dado pessoal (LGPD): no log só os 4 últimos dígitos. */
    static String mascarar(String telefone) {
        if (telefone == null || telefone.length() <= 4) {
            return "****";
        }
        return "*".repeat(telefone.length() - 4) + telefone.substring(telefone.length() - 4);
    }

    /** @return true = confirmou, false = cancelou, vazio = mensagem não reconhecida */
    static Optional<Boolean> interpretar(String conteudo) {
        if (conteudo == null) {
            return Optional.empty();
        }
        String normalizado = Normalizer.normalize(conteudo.strip(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
        if (CONFIRMACOES.contains(normalizado)) {
            return Optional.of(true);
        }
        if (CANCELAMENTOS.contains(normalizado)) {
            return Optional.of(false);
        }
        return Optional.empty();
    }
}
