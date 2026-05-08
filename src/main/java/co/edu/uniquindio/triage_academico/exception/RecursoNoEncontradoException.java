package co.edu.uniquindio.triage_academico.exception;

public class RecursoNoEncontradoException extends RuntimeException {
    public RecursoNoEncontradoException(String recurso, Long id) {
        super(String.format("%s con ID %d no fue encontrado", recurso, id));
    }
    
    public RecursoNoEncontradoException(String message) {
        super(message);
    }
}
