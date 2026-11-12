package com.example.Foodies.Exception;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<?> handleForbidden(ForbiddenException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "prohibido", "mensaje", ex.getMessage()));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<?> handleMaxUploadSize(MaxUploadSizeExceededException ex) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(Map.of("error", "archivo demasiado grande", "mensaje", "El archivo supera el tamaño máximo de 10 MB"));
    }

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

    /** The project's own {@code com.example.Foodies.Exception.Exception}, not {@code java.lang.Exception}. */
    @ExceptionHandler(com.example.Foodies.Exception.Exception.class)
    public ResponseEntity<?> handleGenericException(com.example.Foodies.Exception.Exception ex) {
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

    /** A path or query value of the wrong type, e.g. {@code /api/restaurantes/abc} or an unknown enum value. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<?> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return badRequest("parámetro inválido", valorInvalido(ex.getValue(), ex.getName(), ex.getRequiredType()));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<?> handleMissingParameter(MissingServletRequestParameterException ex) {
        return badRequest("parámetro faltante", "Falta el parámetro obligatorio '" + ex.getParameterName() + "'");
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<?> handleMissingPart(MissingServletRequestPartException ex) {
        return badRequest("parámetro faltante", "Falta la parte obligatoria '" + ex.getRequestPartName() + "'");
    }

    /** Malformed JSON, or a JSON value that does not fit the field (e.g. an unknown enum value). */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<?> handleNotReadable(HttpMessageNotReadableException ex) {
        Throwable cause = ex.getCause();
        if (cause instanceof InvalidFormatException invalid && !invalid.getPath().isEmpty()) {
            return badRequest("cuerpo inválido",
                    valorInvalido(invalid.getValue(), campo(invalid), invalid.getTargetType()));
        }
        if (cause instanceof MismatchedInputException mismatched && !mismatched.getPath().isEmpty()) {
            return badRequest("cuerpo inválido", "El campo '" + campo(mismatched) + "' tiene un formato inválido");
        }
        return badRequest("cuerpo inválido", "El cuerpo de la solicitud no es un JSON válido");
    }

    /** Thrown by {@code @PreAuthorize}; same answer as the security filter chain gives. */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<?> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", "No tenés permisos para acceder a este recurso"));
    }

    /** Same answer as the security filter chain gives to an unauthenticated request. */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<?> handleAuthentication(AuthenticationException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "No estás autenticado"));
    }

    /**
     * Last resort: nothing internal (class names, messages, stack traces) reaches the client.
     * Spring web errors keep their status; data rejected by bean validation or by the database is a 400.
     */
    @ExceptionHandler(java.lang.Exception.class)
    public ResponseEntity<?> handleUnexpected(java.lang.Exception ex) {
        if (ex instanceof ErrorResponse errorResponse) {
            HttpStatusCode status = errorResponse.getStatusCode();
            return ResponseEntity.status(status)
                    .body(Map.of("error", "solicitud inválida", "mensaje", mensajeParaEstado(status)));
        }
        ConstraintViolationException violations = causa(ex, ConstraintViolationException.class);
        if (violations != null) {
            String mensaje = violations.getConstraintViolations().stream()
                    .map(ConstraintViolation::getMessage)
                    .sorted()
                    .collect(Collectors.joining(". "));
            return badRequest("datos inválidos", mensaje.isBlank() ? "Los datos enviados no son válidos" : mensaje);
        }
        if (causa(ex, DataIntegrityViolationException.class) != null) {
            return badRequest("datos inválidos",
                    "Los datos enviados no son válidos: revisá que no falten datos obligatorios y que ningún texto sea demasiado largo");
        }
        log.error("Error inesperado", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "error interno",
                        "mensaje", "Ocurrió un error inesperado. Intentá de nuevo más tarde."));
    }

    private static ResponseEntity<Map<String, String>> badRequest(String error, String mensaje) {
        return ResponseEntity.badRequest().body(Map.of("error", error, "mensaje", mensaje));
    }

    private static String valorInvalido(Object valor, String campo, Class<?> tipo) {
        String mensaje = "El valor '" + valor + "' no es válido para '" + campo + "'";
        if (tipo != null && tipo.isEnum()) {
            mensaje += ". Valores posibles: " + Arrays.stream(tipo.getEnumConstants())
                    .map(String::valueOf)
                    .collect(Collectors.joining(", "));
        }
        return mensaje;
    }

    private static String campo(MismatchedInputException ex) {
        return ex.getPath().stream()
                .map(ref -> ref.getFieldName() != null ? ref.getFieldName() : String.valueOf(ref.getIndex()))
                .collect(Collectors.joining("."));
    }

    private static String mensajeParaEstado(HttpStatusCode status) {
        return switch (status.value()) {
            case 404 -> "Recurso no encontrado";
            case 405 -> "Método no permitido para este recurso";
            case 406, 415 -> "Formato de contenido no soportado";
            default -> status.is4xxClientError()
                    ? "La solicitud no es válida"
                    : "Ocurrió un error inesperado. Intentá de nuevo más tarde.";
        };
    }

    private static <T extends Throwable> T causa(Throwable ex, Class<T> tipo) {
        for (Throwable actual = ex; actual != null; actual = actual.getCause()) {
            if (tipo.isInstance(actual)) {
                return tipo.cast(actual);
            }
            if (actual.getCause() == actual) {
                break;
            }
        }
        return null;
    }
}
