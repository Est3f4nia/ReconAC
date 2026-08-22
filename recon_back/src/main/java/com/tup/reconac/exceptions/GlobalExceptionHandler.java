package com.tup.reconac.exceptions;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ProblemDetail> handleCustomException(CustomException ex, HttpServletRequest req) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(ex.getStatus(), ex.getMessage());
        problem.setType(ex.getType());
        problem.setInstance(URI.create(req.getRequestURI()));
        problem.setProperty("errors", ex.getErrors());
        problem.setProperty("timestamp", Instant.now().toString());
        return ResponseEntity.status(ex.getStatus())
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    // Acceso a endpoints admin por parte de usuarios con privilegios básicos
//    @ExceptionHandler(AuthorizationDeniedException.class)
//    public ResponseEntity<ProblemDetail> handleAuthorizationDenied(
//            AuthorizationDeniedException ex,
//            HttpServletRequest req) {
//
//        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
//                HttpStatus.FORBIDDEN,
//                "No tiene permisos para realizar esta acción"
//        );
//
//        problem.setType(URI.create("/errors/forbidden"));
//        problem.setInstance(URI.create(req.getRequestURI()));
//        problem.setProperty("errors",
//                List.of("Esta acción requiere de más privilegios"));
//        problem.setProperty("timestamp", Instant.now().toString());
//
//        return ResponseEntity.status(HttpStatus.FORBIDDEN)
//                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
//                .body(problem);
//    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        List<String> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .toList();

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "Error de validación");
        problem.setType(URI.create("/errors/validation"));
        problem.setInstance(URI.create(req.getRequestURI()));
        problem.setProperty("errors", errors);
        problem.setProperty("timestamp", Instant.now().toString());
        return ResponseEntity.badRequest()
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleGeneric(Exception ex, HttpServletRequest req) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrió un error inesperado");
        problem.setType(URI.create("/errors/internal"));
        problem.setInstance(URI.create(req.getRequestURI()));
        problem.setProperty("errors", List.of("Contacte al administrador"));
        problem.setProperty("timestamp", Instant.now().toString());
        return ResponseEntity.internalServerError()
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }
}
