package com.ecommerce.project.controller;

import com.ecommerce.project.config.AppConstants;
import com.ecommerce.project.payload.CategoryDTO;
import com.ecommerce.project.payload.CategoryResponse;
import com.ecommerce.project.service.CategoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// @RestController
// → このクラスをSpring MVCのREST API用Controllerとして登録する。
// → Spring BootがこのクラスをControllerとして認識し、
//    HTTPリクエストを受け取れるようになる。
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

    // @GetMapping("/public/categories")
    // → HTTP GETリクエストを「/public/categories」というパスに
    //    マッピング（割り当て）する。
    //
    // → つまり、クライアントから
    //
    //    GET /api/public/categories
    //
    //    というリクエストが送られると、
    //    下の getAllCategories() メソッドが実行される。
    @GetMapping("/public/categories")
    public ResponseEntity<CategoryResponse> getAllCategories(

        // @RequestParam
        // → HTTPリクエストのURLに付いている「クエリパラメータ」を
        //    Javaのメソッド引数として受け取るためのアノテーション。
        //
        // 例えば、以下のURLでリクエストされた場合：
        //
        // GET /api/public/categories?pageNumber=1&pageSize=10
        //
        // pageNumber の値「1」が pageNumber に、
        // pageSize の値「10」が pageSize に渡される。

        // name = "pageNumber"
        // → URLで使用するパラメータ名を「pageNumber」に指定する。
        //
        // defaultValue = AppConstants.PAGE_NUMBER
        // → pageNumber が指定されなかった場合に使用するデフォルト値。
        //
        // required = false
        // → pageNumber の指定を必須にしない。
        //    URLにpageNumberがなくてもエラーにならない。
        @RequestParam(name = "pageNumber", defaultValue = AppConstants.PAGE_NUMBER, required = false) Integer pageNumber,
        @RequestParam(name = "pageSize", defaultValue = AppConstants.PAGE_SIZE, required = false) Integer pageSize,
        @RequestParam(name = "sortBy", defaultValue = AppConstants.SORT_CATEGORIES_BY, required = false) String sortBy,
        @RequestParam(name = "sortOrder", defaultValue = AppConstants.SORT_DIR, required = false) String sortOrder
    ) {
        CategoryResponse categoryResponse = categoryService.getAllCategories(pageNumber, pageSize, sortBy, sortOrder);
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
    // 
    // @RequestBody は「HTTPリクエストのボディをJavaオブジェクトとして受け取る」
    // → HTTPリクエストの「ボディ（Request Body）」に含まれるデータを
    //    Javaオブジェクト（ここではCategoryDTO）に変換する。
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
