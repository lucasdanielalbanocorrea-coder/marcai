package io.github.lucasdanielalbanocorreadev.marcai.agendamento;

import io.github.lucasdanielalbanocorreadev.marcai.cliente.Cliente;
import io.github.lucasdanielalbanocorreadev.marcai.servico.Servico;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import java.time.OffsetDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Agendamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Servico servico;

    private OffsetDateTime inicio;

    /** Calculado pela duração do serviço; a constraint de sobreposição no banco usa [inicio, fim). */
    private OffsetDateTime fim;

    @Enumerated(EnumType.STRING)
    private StatusAgendamento status;

    private OffsetDateTime lembreteEnviadoEm;

    public Agendamento(Cliente cliente, Servico servico, OffsetDateTime inicio) {
        this.cliente = cliente;
        this.servico = servico;
        this.inicio = inicio;
        this.fim = inicio.plusMinutes(servico.getDuracaoMinutos());
        this.status = StatusAgendamento.AGENDADO;
    }

    public void confirmar() {
        if (status == StatusAgendamento.CANCELADO) {
            throw new IllegalStateException("Agendamento " + id + " está cancelado");
        }
        status = StatusAgendamento.CONFIRMADO;
    }

    public void cancelar() {
        status = StatusAgendamento.CANCELADO;
    }

    public void marcarLembreteEnviado(OffsetDateTime quando) {
        lembreteEnviadoEm = quando;
    }
}
