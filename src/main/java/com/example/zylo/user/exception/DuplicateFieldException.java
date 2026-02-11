package com.example.zylo.user.exception;

public class DuplicateFieldException extends RuntimeException{
    public DuplicateFieldException(String message) {
        super(message);
    }
}
