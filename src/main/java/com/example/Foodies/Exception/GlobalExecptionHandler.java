package com.example.Foodies.Exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExecptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, String> errores = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        FieldError::getDefaultMessage,
                        (msg1, msg2) -> msg1 // si hay campos repetidos
                ));

        return ResponseEntity.badRequest().body(errores);
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<?> handleEntityNotFound(EntityNotFoundException ex){
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error","entidad no encontrada","mensaje",ex.getMessage()));
    }
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<?> handleBusinessException(BusinessException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                        "error", "error de negocio",
                        "mensaje", ex.getMessage()
                ));
    }

    @ExceptionHandler(ListNoContentException.class)
    public ResponseEntity<?> handleListNoContent(ListNoContentException ex) {
        return ResponseEntity
                .status(HttpStatus.NO_CONTENT)
                .body(Map.of("error","204","mensaje",ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleGenericException(Exception ex) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                        "error", "error interno",
                        "mensaje", ex.getMessage()
                ));
    }

    @ExceptionHandler(EmailDuplicadoException.class)
    public ResponseEntity<?> handlerEmailDuplicadoException(EmailDuplicadoException ex){
        return  ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(Map.of("error","error conflicto","mensaje","Este email esta en uso"));
    }

    @ExceptionHandler(NotApprovedException.class)
    public ResponseEntity<?> handlerNotApprovedException(EmailDuplicadoException ex){
        return  ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error","error conflicto","mensaje",ex.getMessage()));
    }

    @ExceptionHandler(NotValidCupoException.class)
    public ResponseEntity<?> handlerNotValidCupoException(EmailDuplicadoException ex){
        return  ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error","error conflicto","mensaje",ex.getMessage()));
    }
}
