package com.ecommerce.project.service;

import com.ecommerce.project.model.Category;
import com.ecommerce.project.repositories.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// このクラスをSpringの「Service Bean」として登録する。
// SpringがCategoryServiceImplのインスタンスを自動的に生成・管理する。
// そのため、他のクラスから依存性注入（Dependency Injection）で利用できる
@Service
public class CategoryServiceImpl implements CategoryService{
    //private List<Category> categories = new ArrayList<>();
    private Long nextId = 1L;

    @Autowired
    private CategoryRepository categoryRepository;

    @Override
    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
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
        // category.setCategoryId(nextId++);
        categoryRepository.save(category);
    }

    @Override
    public String deleteCategory(Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));

        categoryRepository.delete(category);
        return "Category with categoryID: " + categoryId + " deleted successfully";
    }

    @Override
    public Category updateCategory(Category category, Long categoryId) {
        Category savedCategory = categoryRepository.findById(categoryId)
                .orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource not found"));

        category.setCategoryId(categoryId);
        // Categoryオブジェクトをデータベースに保存する。
        // Spring Data JPAのsave()メソッドを呼び出すことで、
        // CategoryエンティティをDBのcategoriesテーブルに保存する。
        //
        // 新規のCategoryの場合：
        // → INSERT文が実行され、DBに新しいレコードが追加される。
        //
        // 既存のCategoryの場合：
        // → UPDATE文が実行され、既存のレコードが更新される。
        //
        // save()の戻り値として、保存されたCategoryオブジェクトが返される。
        savedCategory = categoryRepository.save(category);
        return savedCategory;

    }
}
