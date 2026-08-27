package com.ecommerce.project.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

/**
 * このクラスをJPAのエンティティとして扱うことを指定する。
 *
 * name属性で、JPQL上で使用するエンティティ名を「categories」に指定する。
 */
@Entity(name = "categories")
public class Category {
    /**
     * このフィールドをエンティティの主キー（Primary Key）として指定する。
     * DB上では、このフィールドに対応するカラムが主キーになる。
     */
    @Id
    /**
     * 主キーの値を自動生成する方法を指定する。
     *
     * GenerationType.IDENTITYを指定すると、
     * IDの生成をデータベース側の自動採番機能に任せる。
     *
     * そのため、Categoryオブジェクトを保存するときに
     * categoryIdを自分で設定する必要がない。
     */
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long categoryId;
    private String categoryName;

    public Category(Long categoryId, String categoryName) {
        this.categoryId = categoryId;
        this.categoryName = categoryName;
    }

    public Category() {
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }
}
