package com.example.doglistener.history;

public class ResponseHistoryException
        extends RuntimeException {

    public ResponseHistoryException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }
}