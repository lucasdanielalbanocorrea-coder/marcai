package io.github.lucasdanielalbanocorreadev.marcai.shared.exception;

public class HorarioIndisponivelException extends RuntimeException {

    public HorarioIndisponivelException() {
        super("Já existe um agendamento nesse horário");
    }
}
