package com.ecommerce.project.repositories;

import com.ecommerce.project.model.Category;
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
}
