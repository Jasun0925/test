package com.greeting.exception;

import com.greeting.common.exception.ExceptionBase;
import org.springframework.http.HttpStatus;

public class GreetingNotFoundException extends ExceptionBase {

    public GreetingNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, message);
    }
}
