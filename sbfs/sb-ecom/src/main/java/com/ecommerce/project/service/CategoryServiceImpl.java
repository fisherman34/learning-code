package com.ecommerce.project.service;

import com.ecommerce.project.exceptions.APIException;
import com.ecommerce.project.exceptions.ResourceNotFoundException;
import com.ecommerce.project.model.Category;
import com.ecommerce.project.repositories.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

// このクラスをSpringの「Service Bean」として登録する。
// SpringがCategoryServiceImplのインスタンスを自動的に生成・管理する。
// そのため、他のクラスから依存性注入（Dependency Injection）で利用できる
@Service
public class CategoryServiceImpl implements CategoryService{

    @Autowired
    private CategoryRepository categoryRepository;

    @Override
    public List<Category> getAllCategories() {
        List<Category> categories = categoryRepository.findAll();
        if (categories.isEmpty()) {
            throw new APIException("No category created now!");
        }
        return categories;
    }

    @Override
    public void createCategory(Category category) {
        Category savedCategory = categoryRepository.findByCategoryName(category.getCategoryName());
        if (savedCategory != null) {
            /*
            CategoryServiceImpl.createCategory() does: throw new APIException("...")

            CategoryController.createCategory() calls: categoryService.createCategory(category);

            No try/catch in that controller method
            The exception bubbles up through the Spring MVC request pipeline
            Spring sees it and matches it to: @ExceptionHandler(APIException.class)
            inside MyGlobalExceptionHandler

            That handler returns:
            • new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
            So the client receives an HTTP 400 response with the exception message.
            Important distinction:
            • The controller does not “catch” it manually
            • But Spring’s exception handling mechanism catches it automatically at the framework level
            */
            throw new APIException("Category with name '" + category.getCategoryName() + "' already exists!");
        }
        categoryRepository.save(category);
    }

    @Override
    public String deleteCategory(Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
                // orElseThrow() に渡しているのは例外を生成するための Supplier（ラムダ式）
                .orElseThrow(() -> new ResourceNotFoundException("Category", "categoryId", categoryId));

        categoryRepository.delete(category);
        return "Category with categoryID: " + categoryId + " deleted successfully";
    }

    @Override
    public Category updateCategory(Category category, Long categoryId) {
        Category savedCategory = categoryRepository.findById(categoryId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Category", "categoryId", categoryId));

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
