package com.gattopiccolo.ledger.web;

import com.gattopiccolo.ledger.exception.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Maps domain exceptions to HTTP status codes so the front-end sees a 404 for a
 * missing account/transaction and a 400 for an invalid amount, rather than the
 * default 500.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({AccountNotFoundException.class, TransactionNotFoundException.class})
    ProblemDetail handleNotFound(RuntimeException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND, ex.getMessage());
        pd.setTitle("Not found");
        return pd;
    }

    @ExceptionHandler(InvalidAmountException.class)
    ProblemDetail handleBadRequest(RuntimeException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNPROCESSABLE_CONTENT,
                ex.getMessage());
        pd.setTitle("Invalid amount");
        return pd;
    }

    @ExceptionHandler(InsufficientFundsException.class)
    ProblemDetail handleInsufficientFunds() {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNPROCESSABLE_CONTENT,
                "Insufficient funds for this operation.");
        pd.setTitle("Insufficient funds");
        return pd;
    }

    @ExceptionHandler(ExternalLoggingException.class)
    ProblemDetail handleExternal() {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.SERVICE_UNAVAILABLE,
                "The operation could not be completed: a required external service is temporarily unavailable. Please try again later.");
        pd.setTitle("Service temporarily unavailable");
        return pd;
    }
}
