package com.example.habitforge.exception;

/** Thrown when a request violates a business rule (e.g. logging a future day). */
public class BusinessRuleException extends RuntimeException {
    public BusinessRuleException(String message) { super(message); }
}
