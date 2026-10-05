package com.example.Foodies.Exception;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

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

    /**
     * Violaciones que se detectan al guardar la entidad (no al validar el DTO).
     * Sin este handler salian como 500.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<?> handleConstraintViolation(ConstraintViolationException ex) {
        Map<String, String> errores = ex.getConstraintViolations().stream()
                .collect(Collectors.toMap(
                        v -> v.getPropertyPath().toString(),
                        ConstraintViolation::getMessage,
                        (msg1, msg2) -> msg1
                ));
        return ResponseEntity.badRequest().body(Map.of(
                "error", "datos invalidos",
                "errores", errores,
                "mensaje", "Revisá los campos enviados"));
    }

    /**
     * Body que ni siquiera se puede convertir (enums inexistentes, tipos raros, JSON roto).
     * Sin esto Spring devuelve su propia respuesta vacía.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<?> handleNotReadable(HttpMessageNotReadableException ex) {
        String causa = ex.getMostSpecificCause().getMessage();
        String campo = causa != null && causa.contains("\"")
                ? causa.substring(causa.indexOf('"') + 1, causa.lastIndexOf('"'))
                : null;
        return ResponseEntity.badRequest().body(Map.of(
                "error", "json invalido",
                "mensaje", campo != null
                        ? "El valor de '" + campo + "' no es válido. Revisá los campos permitidos."
                        : "El cuerpo de la petición no es válido"));
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<?> handleEntityNotFound(EntityNotFoundException ex){
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error","entidad no encontrada","mensaje",ex.getMessage()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<?> handleAccessDenied(AccessDeniedException ex){
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", "sin permisos", "mensaje", ex.getMessage()));
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
        return ResponseEntity.noContent().build();
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

    @ExceptionHandler(CredencialesInvalidasException.class)
    public ResponseEntity<?> handlerCredencialesInvalidas(CredencialesInvalidasException ex){
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error","no autorizado","mensaje",ex.getMessage()));
    }

    @ExceptionHandler(HistorialAsociadoException.class)
    public ResponseEntity<?> handlerHistorialAsociado(HistorialAsociadoException ex){
        return  ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(Map.of("error","error conflicto","mensaje",ex.getMessage()));
    }

    @ExceptionHandler(EmailDuplicadoException.class)
    public ResponseEntity<?> handlerEmailDuplicadoException(EmailDuplicadoException ex){
        return  ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(Map.of("error","error conflicto","mensaje","Este email esta en uso"));
    }

    @ExceptionHandler(NotApprovedException.class)
    public ResponseEntity<?> handlerNotApprovedException(NotApprovedException ex){
        return  ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error","error conflicto","mensaje",ex.getMessage()));
    }

    @ExceptionHandler(NotValidCupoException.class)
    public ResponseEntity<?> handlerNotValidCupoException(NotValidCupoException ex){
        return  ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error","error conflicto","mensaje",ex.getMessage()));
    }
}
