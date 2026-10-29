package com.greeting.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public abstract class ExceptionBase extends RuntimeException {

    private final HttpStatus status;

    public ExceptionBase(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }
}
