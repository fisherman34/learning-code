package com.ecommerce.project.controller;

import com.ecommerce.project.payload.CategoryDTO;
import com.ecommerce.project.payload.CategoryResponse;
import com.ecommerce.project.service.CategoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping("/public/categories")
    public ResponseEntity<CategoryResponse> getAllCategories() {
        CategoryResponse categoryResponse = categoryService.getAllCategories();
        // ResponseEntity は、Spring BootのControllerから、
        // HTTPレスポンスのステータス・ヘッダー・ボディを明示的に指定して返すためのクラス
        return new ResponseEntity<>(categoryResponse, HttpStatus.OK);
    }

    @PostMapping("/public/categories")
    // @Valid
    // → @RequestBodyで受け取ったCategoryオブジェクトに対して、
    //    Categoryクラスに設定されたBean Validationを実行する。
    //    バリデーションエラーがある場合、通常はメソッドの処理を実行せず、
    //    Spring Bootがエラーレスポンスを返す。
    // @RequestBody の入力値をバリデーションする場合、@Valid はController層に置くのが一般的
    public ResponseEntity<CategoryDTO> createCategory(@Valid @RequestBody CategoryDTO categoryDTO) {
        CategoryDTO savedCategoryDTO = categoryService.createCategory(categoryDTO);
        // ダイヤモンド演算子
        return new ResponseEntity<>(savedCategoryDTO, HttpStatus.CREATED);
    }

    @DeleteMapping("/admin/categories/{categoryId}")
    public ResponseEntity<CategoryDTO> deleteCategory(@PathVariable Long categoryId) {
        CategoryDTO deletedCategoryDTO = categoryService.deleteCategory(categoryId);
        return new ResponseEntity<>(deletedCategoryDTO, HttpStatus.OK);
    }

    @PutMapping("/public/categories/{categoryId}")
    public ResponseEntity<CategoryDTO> updateCategory(@Valid @RequestBody CategoryDTO categoryDTO,
                                                 @PathVariable Long categoryId) {
        CategoryDTO savedCategoryDTO = categoryService.updateCategory(categoryDTO, categoryId);
        return new ResponseEntity<>(savedCategoryDTO, HttpStatus.OK);
    }
}
