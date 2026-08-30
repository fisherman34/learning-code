package com.ecommerce.project.exceptions;

// ResourceNotFoundException は RuntimeException を継承する
public class ResourceNotFoundException extends RuntimeException {
    String resourceName;
    String field;
    String fieldName;
    Long fieldId;

    public ResourceNotFoundException() {
    }

    public ResourceNotFoundException(String resourceName, String field, String fieldName) {
        // RuntimeException の親クラスのコンストラクタを呼び出し、
        // 例外発生時に表示するエラーメッセージを設定する。
        //
        // String.format() を使って、以下のような文字列を生成する。
        // 例：
        // resourceName = "Category"
        // field        = "categoryName"
        // fieldName    = "Books"
        //
        // → "Category not found with categoryName : 'Books'"
        super(String.format("%s not found with %s : '%s'", resourceName, field, fieldName));
        this.resourceName = resourceName;
        this.field = field;
        this.fieldName = fieldName;
    }

    public ResourceNotFoundException(String resourceName, String field, Long fieldId) {
        // %s → resourceName の値を文字列として埋め込む
        // %s → field の値を文字列として埋め込む
        // %d → fieldId の値を整数として埋め込む
        super(String.format("%s not found with %s : '%d'", resourceName, field, fieldId));
        this.resourceName = resourceName;
        this.field = field;
        this.fieldId = fieldId;
    }
}
