package io.github.lucasdanielalbanocorreadev.marcai.servico;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Tipo de serviço oferecido (corte, consulta...). A duração define o fim do agendamento. */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Servico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nome;

    private int duracaoMinutos;

    @Column(precision = 10, scale = 2)
    private BigDecimal preco;

    public Servico(String nome, int duracaoMinutos, BigDecimal preco) {
        this.nome = nome;
        this.duracaoMinutos = duracaoMinutos;
        this.preco = preco;
    }
}
