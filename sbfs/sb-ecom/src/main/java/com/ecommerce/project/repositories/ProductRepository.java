package com.ecommerce.project.repositories;

import com.ecommerce.project.model.Category;
import com.ecommerce.project.model.Product;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    /*
    Spring Data JPAのクエリメソッドの命名規則

    よく使うキーワード
    キーワード	意味	        例
    findBy	～を条件に検索	    findByProductName
    And	    AND条件	        findByNameAndPrice
    Or	    OR条件	        findByNameOrPrice
    Like	LIKE検索	        findByNameLike
    IgnoreCase	大文字・小文字を無視	    findByNameIgnoreCase
    OrderBy	並び順を指定	    findByNameOrderByPriceAsc
    Asc	    昇順	            OrderByPriceAsc
    Desc	降順	            OrderByPriceDesc
     */

    // methodを定義するだけで、実装はSpring Data JPAが自動で行う。
    Page<Product> findByCategoryOrderByPriceAsc(Category category, Pageable pageDetails);

    // findBy         ：検索条件を指定する
    // ProductName    ：ProductエンティティのproductNameフィールドを対象にする
    // Like           ：LIKE検索を行う（部分一致）
    // IgnoreCase     ：大文字・小文字を区別しない
    Page<Product> findByProductNameLikeIgnoreCase(String keyword, Pageable pageDetails);
}
