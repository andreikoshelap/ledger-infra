package com.gattopiccolo.ledger.web;

import com.gattopiccolo.ledger.exception.AccountNotFoundException;
import com.gattopiccolo.ledger.exception.InsufficientFundsException;
import com.gattopiccolo.ledger.exception.InvalidAmountException;
import com.gattopiccolo.ledger.exception.TransactionNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * Maps domain exceptions to HTTP status codes so the front-end sees a 404 for a
 * missing account/transaction and a 400 for an invalid amount, rather than the
 * default 500.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler({AccountNotFoundException.class, TransactionNotFoundException.class})
    public Map<String, String> handleNotFound(RuntimeException ex) {
        return Map.of("error", "Not Found", "message", ex.getMessage());
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(InvalidAmountException.class)
    public Map<String, String> handleBadRequest(RuntimeException ex) {
        return Map.of("error", "Bad Request", "message", ex.getMessage());
    }

    @ExceptionHandler(InsufficientFundsException.class)
    ProblemDetail handleInsufficientFunds() {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNPROCESSABLE_CONTENT,         // 422
                "Insufficient funds for this operation.");
        pd.setTitle("Insufficient funds");
        return pd;
    }
}
