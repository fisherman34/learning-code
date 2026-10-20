package com.ecommerce.project.repositories;

import com.ecommerce.project.model.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    @Query("SELECT ci FROM CartItem ci WHERE ci.cart.cartId = ?1 AND ci.product.productId = ?2")
    CartItem findCartItemByProductIdAndCartId(Long cartId, Long productId);

    /*
    @Modifying：
    @Queryで指定したSQL（JPQL）がデータを変更する処理であることをSpring Data JPAに伝える。
    この例では、DELETE文を実行して、指定したカートIDと商品IDに一致するCartItemを削除する。
    @Modifyingがない場合、Spring Data JPAは通常の検索クエリとして扱うため、
    DELETE文を正しく実行できない。
     */
    @Modifying
    @Query("DELETE FROM CartItem ci WHERE ci.cart.cartId = ?1 AND ci.product.productId = ?2")
    void deleteCartItemByProductIdAndCartId(Long cartId, Long productId);
}
