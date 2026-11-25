package io.github.lucasdanielalbanocorreadev.marcai.whatsapp;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface EventoProcessadoRepository extends JpaRepository<EventoProcessado, String> {

    /**
     * Registra o evento de forma atômica, sem corrida entre entregas simultâneas.
     *
     * @return 1 se é a primeira vez que o evento chega, 0 se já foi processado
     */
    @Modifying
    @Query(value = "INSERT INTO evento_processado (id) VALUES (:id) ON CONFLICT (id) DO NOTHING", nativeQuery = true)
    int registrar(String id);
}
