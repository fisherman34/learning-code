package com.ecommerce.project.controller;

import com.ecommerce.project.model.Category;
import com.ecommerce.project.service.CategoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
// このコントローラーが提供するすべてのAPIの共通URL（ベースパス）を「/api」に設定する
// そのため、各メソッドの@RequestMappingや@GetMappingなどで指定したパスの先頭に「/api」が付く
// 例：@RequestMapping("/public/categories")
//     → 実際のURLは「/api/public/categories」
@RequestMapping("/api")
public class CategoryController {

    private CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    //@GetMapping("/public/categories")
    @RequestMapping(value = "/public/categories", method = RequestMethod.GET)
    // List<Category>は Categoryオブジェクトを複数格納するList
    public ResponseEntity<List<Category>> getAllCategories() {
        List<Category> categories = categoryService.getAllCategories();
        return new ResponseEntity<>(categories, HttpStatus.OK);
    }

    //@PostMapping("/public/categories")
    @RequestMapping(value = "/public/categories", method = RequestMethod.POST)
    // @Valid
    // → @RequestBodyで受け取ったCategoryオブジェクトに対して、
    //    Categoryクラスに設定されたBean Validationを実行する。
    //    バリデーションエラーがある場合、通常はメソッドの処理を実行せず、
    //    Spring Bootがエラーレスポンスを返す。
    // @RequestBody の入力値をバリデーションする場合、@Valid はController層に置くのが一般的
    public ResponseEntity<String> createCategory(@Valid @RequestBody Category category) {
        categoryService.createCategory(category);
        // ダイヤモンド演算子
        return new ResponseEntity<>("Category added successfully", HttpStatus.CREATED);
    }

    @DeleteMapping("/admin/categories/{categoryId}")
    public ResponseEntity<String> deleteCategory(@PathVariable Long categoryId) {
        String status = categoryService.deleteCategory(categoryId);
        //return  new ResponseEntity<>(status, HttpStatus.OK);
        //return ResponseEntity.ok(status);
        // HTTPステータスコード「200 OK」を設定する
        // HTTPレスポンスのボディ（本文）にstatusの値を設定する
        return ResponseEntity.status(HttpStatus.OK).body(status);
    }

    @PutMapping("/public/categories/{categoryId}")
    public ResponseEntity<String> updateCategory(@Valid @RequestBody Category category,
                                                 @PathVariable Long categoryId) {
        Category savedCategory = categoryService.updateCategory(category, categoryId);
        return new ResponseEntity<>("Category with ID " + categoryId + " updated successfully", HttpStatus.OK);
    }
}
