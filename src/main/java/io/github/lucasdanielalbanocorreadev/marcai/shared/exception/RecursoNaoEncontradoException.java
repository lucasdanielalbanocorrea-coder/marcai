package io.github.lucasdanielalbanocorreadev.marcai.shared.exception;

public class RecursoNaoEncontradoException extends RuntimeException {

    public RecursoNaoEncontradoException(String recurso, Object id) {
        super(recurso + " " + id + " não encontrado");
    }
}
