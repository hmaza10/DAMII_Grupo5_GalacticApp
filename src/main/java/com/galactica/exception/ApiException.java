package com.galactica.exception;

import org.springframework.http.HttpStatus;

import java.util.Map;

public class ApiException extends RuntimeException{

    private final HttpStatus status;
    private final transient Object cuerpo;

    public ApiException(HttpStatus status, String mensaje, Object cuerpo) {
        super(mensaje);
        this.status = status;
        this.cuerpo = cuerpo;
    }

    public static ApiException validacion(Map<String, String> errores) {
        return new ApiException(HttpStatus.BAD_REQUEST, "Datos inválidos", errores);
    }

    public static ApiException noEncontrado(String mensaje) {
        return new ApiException(HttpStatus.NOT_FOUND, mensaje, Map.of("mensaje", mensaje));
    }

    public static ApiException conflicto(String mensaje) {
        return new ApiException(HttpStatus.CONFLICT, mensaje, Map.of("mensaje", mensaje));
    }

    public HttpStatus getStatus() {
        return status;
    }

    public Object getCuerpo() {
        return cuerpo;
    }
}
