package com.ecommerce.project.repositories;

import com.ecommerce.project.model.Category;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.data.jpa.repository.JpaRepository;

// CategoryRepository というinterfaceを定義する。
//
// extends：
// interfaceが別のinterfaceを継承するときに使用する。
//
// JpaRepository<Category, Long>：
// JpaRepositoryを継承する。
//
// Category：
// このRepositoryがデータベース操作の対象とするEntityクラス。
// 今回はCategory Entityが対象。
//
// Long：
// Category Entityの主キー（@Id）の型。
// CategoryのidがLong型なのでLongを指定する。
public interface CategoryRepository extends JpaRepository<Category, Long> {
    // findBy + Entityのフィールド名という命名規則を使うことで、
    // Spring Data JPAが検索処理を自動生成してくれる
    //
    // findBy：
    // Spring Data JPAが「検索処理を行うメソッド」として認識する。
    //
    // CategoryName：
    // Category Entityに存在する「categoryName」フィールドを指定する。
    // Spring Data JPAはメソッド名を解析して、
    // categoryNameを検索条件として使用する。
    //
    // 例：
    // findByCategoryName("Electronics")
    //
    // ↓ Spring Data JPAがメソッド名を解析
    //
    // SELECT * FROM categories
    // WHERE category_name = 'Electronics';
    //
    // ※ 実際のSQLはJPA/Hibernateが自動的に生成する。
    Category findByCategoryName(String categoryName);
}
