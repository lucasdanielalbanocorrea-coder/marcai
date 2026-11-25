package io.github.lucasdanielalbanocorreadev.marcai.whatsapp;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.time.OffsetDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Id (wamid) de uma mensagem do webhook já tratada. A Meta entrega "pelo menos uma vez",
 * então o mesmo evento pode chegar repetido. A gravação é feita por
 * {@link EventoProcessadoRepository#registrar(String)}.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EventoProcessado {

    @Id
    private String id;

    private OffsetDateTime processadoEm;
}
