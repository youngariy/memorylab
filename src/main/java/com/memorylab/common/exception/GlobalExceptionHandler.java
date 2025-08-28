package com.memorylab.common.exception;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice //예외처리 애노테이션(전역)
public class GlobalExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)//컨트롤러 내부에서 발생한 예외만 처리
    public ResponseEntity<?> handleValid(MethodArgumentNotValidException e){
        var msg = e.getBindingResult().getFieldErrors()
                .stream().findFirst().map(fe -> fe.getField()+": "+fe.getDefaultMessage())
                .orElse("Validation error");
        return ResponseEntity.badRequest().body(msg);
    }
}
