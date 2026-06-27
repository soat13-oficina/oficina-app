package br.com.oficina.common.infrastructure.web;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import br.com.oficina.pecainsumo.domain.model.CategoriaPeca;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import br.com.oficina.common.domain.exception.ConflitoDeRecursoException;
import br.com.oficina.common.domain.exception.RecursoNaoEncontradoException;
import br.com.oficina.common.domain.exception.RegraDeNegocioException;

@RestControllerAdvice
public class ApiExceptionHandler {
    private static final String MENSAGEM_REQUISICAO_INVALIDA = "A requisicao enviada e invalida.";
    private static final String MENSAGEM_CORPO_INVALIDO = "Os dados enviados sao invalidos. Revise o corpo da requisicao.";
    private static final String MENSAGEM_VALIDACAO = "Erro de validação nos campos informados.";

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(RecursoNaoEncontradoException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(Instant.now(), HttpStatus.NOT_FOUND.value(), exception.getMessage()));
    }

    @ExceptionHandler(RegraDeNegocioException.class)
    public ResponseEntity<ErrorResponse> handleBusinessRule(RegraDeNegocioException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(Instant.now(), HttpStatus.BAD_REQUEST.value(), exception.getMessage()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(Instant.now(), HttpStatus.CONFLICT.value(),
                        "A operacao viola uma restricao de integridade de dados (registro duplicado ou vinculado)."));
    }

    @ExceptionHandler(ConflitoDeRecursoException.class)
    public ResponseEntity<ErrorResponse> handleConflict(ConflitoDeRecursoException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(Instant.now(), HttpStatus.CONFLICT.value(), exception.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> handleValidationErrors(MethodArgumentNotValidException exception) {
        List<CampoErro> erros = exception.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> new CampoErro(fieldError.getField(), fieldError.getDefaultMessage()))
                .toList();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ValidationErrorResponse(Instant.now(), HttpStatus.BAD_REQUEST.value(), MENSAGEM_VALIDACAO, erros));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleInvalidRequestBody(HttpMessageNotReadableException exception) {
        String mensagem = MENSAGEM_CORPO_INVALIDO;

        Throwable cause = exception.getCause();
        if (cause != null && cause.getMessage() != null && cause.getMessage().contains("CategoriaPeca")) {
            String categoriasAceitas = String.join(", ",
                    Arrays.stream(CategoriaPeca.values()).map(Enum::name).toList());
            mensagem = "Categoria inválida. Os valores aceitos são: " + categoriasAceitas + ".";
        }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(Instant.now(), HttpStatus.BAD_REQUEST.value(), mensagem));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException exception) {
        if (exception.getRequiredType() != null && exception.getRequiredType().equals(CategoriaPeca.class)) {
            String categoriasAceitas = String.join(", ",
                    Arrays.stream(CategoriaPeca.values()).map(Enum::name).toList());
            String mensagem = "Categoria inválida: '" + exception.getValue()
                    + "'. Os valores aceitos são: " + categoriasAceitas + ".";
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(Instant.now(), HttpStatus.BAD_REQUEST.value(), mensagem));
        }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(Instant.now(), HttpStatus.BAD_REQUEST.value(), MENSAGEM_REQUISICAO_INVALIDA));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(Instant.now(), HttpStatus.BAD_REQUEST.value(), MENSAGEM_REQUISICAO_INVALIDA));
    }

    record ErrorResponse(Instant timestamp, int status, String message) {
    }

    record ValidationErrorResponse(Instant timestamp, int status, String message, List<CampoErro> erros) {
    }

    record CampoErro(String campo, String mensagem) {
    }
}
