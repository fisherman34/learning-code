package com.ecommerce.project.service;

import com.ecommerce.project.model.Category;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

// このクラスをSpringの「Service Bean」として登録する。
// SpringがCategoryServiceImplのインスタンスを自動的に生成・管理する。
// そのため、他のクラスから依存性注入（Dependency Injection）で利用できる
@Service
public class CategoryServiceImpl implements CategoryService{
    private List<Category> categories = new ArrayList<>();
    private Long nextId = 1L;

    @Override
    public List<Category> getAllCategories() {
        return categories;
    }

    @Override
    public void createCategory(Category category) {
        // CategoryオブジェクトのcategoryIdに、現在のnextIdの値を設定する。
        // nextId++ は「後置インクリメント」なので、
        // まず現在の値を使用してから、nextIdを1増やす。
        //
        // 例：
        // nextId = 1 の場合
        // → category.setCategoryId(1);
        // → その後 nextId は 2 になる。
        //
        // 次にcreateCategory()を呼び出すと、
        // → category.setCategoryId(2);
        // → その後 nextId は 3 になる。
        category.setCategoryId(nextId++);
        categories.add(category);
    }

    @Override
    public String deleteCategory(Long categoryId) {
        // Stream APIを使って、指定された categoryId を持つ
        // Category をリストから検索して取得している
        Category category = categories.stream()
                .filter(c -> c.getCategoryId().equals(categoryId))
                // ↑
                // c は categories の中にある「現在処理しているCategoryオブジェクト」
                //
                // c.getCategoryId()
                // → 現在のCategoryのcategoryIdを取得する
                //
                // .equals(categoryId)
                // → 取得したcategoryIdと、メソッドの引数categoryIdを比較する
                //
                // 一致する場合 → true
                // 一致しない場合 → false
                .findFirst()
                // findFirst()：
                // Streamの中から条件に一致する最初の要素を取得する。
                //
                // ただし、戻り値は Category ではなく、
                // Optional<Category> になる。
                //
                // Categoryが見つかった場合：
                // → Optional<Category> にCategoryが格納される。
                //
                // Categoryが見つからなかった場合：
                // → Optional.empty() になる。
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource not found"));
                // orElseThrow()：
                // Optionalの中に値が存在する場合は、
                // その値（Category）を取得して返す。
                //
                // Optionalが空の場合は、
                // 指定した例外をスローする。
                //
                // () -> new ResponseStatusException(...)
                // ↑
                // Categoryが見つからなかった場合に実行される
                // Lambda式。
                //
                // new ResponseStatusException(...)
                // → HTTPエラーを表す例外オブジェクトを生成する。
                //
                // HttpStatus.NOT_FOUND
                // → HTTPステータスコード「404 Not Found」。
                //    「指定されたリソースが存在しない」という意味。
                //
                // "Resource not found"
                // → クライアントに返すエラーメッセージ。
                //    「リソースが見つかりません」という意味。
                //
                // つまり、
                // 指定されたcategoryIdのCategoryが存在する
                //     → Categoryをcategory変数に代入
                //
                // 存在しない
                //     → HTTP 404 Not Found例外をスローする

        categories.remove(category);
        return "Category with categoryID: " + categoryId + " deleted successfully";
    }
}
