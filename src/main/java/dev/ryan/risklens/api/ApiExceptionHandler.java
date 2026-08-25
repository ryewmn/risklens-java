package dev.ryan.risklens.api;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {
    private static final Logger LOG = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail validation(MethodArgumentNotValidException exception, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "One or more input fields are invalid.");
        problem.setTitle("Validation failed");
        problem.setType(URI.create("urn:risklens:error:validation"));
        problem.setInstance(URI.create(request.getRequestURI()));
        Map<String, String> violations = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors()
                .forEach(error -> violations.putIfAbsent(error.getField(), error.getDefaultMessage()));
        problem.setProperty("code", "INVALID_INPUT");
        problem.setProperty("violations", violations);
        return problem;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ProblemDetail malformed(HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "The request body must be valid JSON with the documented fields.");
        problem.setTitle("Malformed request");
        problem.setType(URI.create("urn:risklens:error:malformed-json"));
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("code", "MALFORMED_JSON");
        return problem;
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail unexpected(Exception exception, HttpServletRequest request) {
        LOG.error("event=unhandled_request_error path={} errorType={}",
                request.getRequestURI(), exception.getClass().getSimpleName());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
                "The prediction service could not complete the request.");
        problem.setTitle("Inference unavailable");
        problem.setType(URI.create("urn:risklens:error:inference"));
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("code", "INFERENCE_FAILED");
        return problem;
    }
}
