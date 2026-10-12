package com.ecommerce.project.repositories;

import com.ecommerce.project.model.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CartRepository extends JpaRepository<Cart, Long> {

    /*
    @Query は、データベースを検索するためのクエリを指定するアノテーション。

    FROM Cart c は、「Cart エンティティを c という名前で参照する」と宣言しています。

    SELECT cの　c は先ほど FROM Cart c で定義した別名です。
    したがって、SELECT c は 検索条件に一致した Cart エンティティを検索結果として取得するという意味になります。

     = ?1 は、メソッドの第1引数である email に対応する。

     */
    @Query("SELECT c FROM Cart c WHERE c.user.email = ?1")
    Cart findCartByEmail(String email);
}
