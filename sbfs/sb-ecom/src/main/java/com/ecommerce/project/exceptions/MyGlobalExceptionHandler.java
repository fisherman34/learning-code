package com.ecommerce.project.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

// @RestControllerAdvice はすべてのREST Controllerで
// 共通して使う例外処理クラス
@RestControllerAdvice
public class MyGlobalExceptionHandler {

    // MethodArgumentNotValidException が発生した場合に、
    // このメソッドを例外ハンドラーとして実行する。
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> myMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        Map<String, String> response = new HashMap<>();

        // e.getBindingResult()
        // → MethodArgumentNotValidException に含まれている
        //   バリデーション結果（BindingResult）を取得する。
        //
        // .getAllErrors()
        // → BindingResult に含まれているすべてのエラーを取得する。
        //
        // .forEach(err -> { ... })
        // → 取得したエラーを1件ずつ取り出して、
        //   {} 内の処理を実行する。
        e.getBindingResult().getAllErrors().forEach(err -> {
            // err は ObjectError 型として取得されるため、
            // フィールド単位のエラーを表す FieldError 型にキャストする。
            //
            // .getField()
            // → バリデーションエラーが発生したフィールド名を取得する。
            //   例：「name」「price」「description」など。
            String fieldName = ((FieldError) err).getField();
            // getDefaultMessage()
            // → エラーに設定されたデフォルトのエラーメッセージを取得する。
            String errorMessage = err.getDefaultMessage();
            response.put(fieldName, errorMessage);
        });
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);

    }
}

