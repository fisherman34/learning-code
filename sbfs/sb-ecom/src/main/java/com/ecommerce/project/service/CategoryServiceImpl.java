package com.ecommerce.project.service;

import com.ecommerce.project.exceptions.APIException;
import com.ecommerce.project.exceptions.ResourceNotFoundException;
import com.ecommerce.project.model.Category;
import com.ecommerce.project.payload.CategoryDTO;
import com.ecommerce.project.payload.CategoryResponse;
import com.ecommerce.project.repositories.CategoryRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

// このクラスをSpringの「Service Bean」として登録する。
// SpringがCategoryServiceImplのインスタンスを自動的に生成・管理する。
// そのため、他のクラスから依存性注入（Dependency Injection）で利用できる
@Service
public class CategoryServiceImpl implements CategoryService{

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ModelMapper modelMapper;

    @Override
    public CategoryResponse getAllCategories(Integer pageNumber, Integer pageSize) {

        // ページネーションの設定情報（Pageable）を作成する。
        // PageRequest.of()を使用して、取得するページ番号と1ページあたりの件数を指定する。
        Pageable pageDetails = PageRequest.of(pageNumber, pageSize);
        // findAll(pageDetails) は、Pageable型の pageDetails に指定された
        // 「ページ番号」と「1ページあたりの取得件数」に従って、
        // データベースからCategoryを取得する。
        //
        // Page<Category>は、単なるCategoryのリストではなく、
        // 取得したCategoryのデータに加えて、
        // ページネーションに関する情報も保持している。
        Page<Category> categoryPage = categoryRepository.findAll(pageDetails);
        // getContent()は、Pageオブジェクトに含まれている
        // 「現在のページのデータ」をListとして返すメソッド。
        List<Category> categories = categoryPage.getContent();
        if (categories.isEmpty()) {
            throw new APIException("No category created now!");
        }

        // categories（List<Category>）をStream<Category>に変換する。
        //
        // もともとのcategoriesは、
        //
        // List<Category>
        //
        // という「Categoryオブジェクトのリスト」。
        //
        // stream()を呼び出すことで、
        //
        // List<Category>
        //      ↓
        // Stream<Category>
        //
        // となり、各Categoryに対して処理を行えるようになる。
        List<CategoryDTO> categoryDTOs = categories.stream()
                // Stream内の各CategoryオブジェクトをCategoryDTOに変換する。
                //
                // map()は、Streamの各要素に対して指定した処理を行い、
                // 処理結果から新しいStreamを作成するメソッド。
                //
                // category
                //   ↓
                // modelMapper.map(category, CategoryDTO.class)
                //   ↓
                // CategoryDTO
                //
                // ModelMapperがCategoryのフィールド値をCategoryDTOへコピーする。
                .map(category -> modelMapper.map(category, CategoryDTO.class))
                // Stream<CategoryDTO>をList<CategoryDTO>に変換する。
                //
                // 最終的にcategoryDTOsには、
                // categoriesに含まれていたCategoryを
                // CategoryDTOへ変換したリストが格納される。
                .toList();
        CategoryResponse categoryResponse = new CategoryResponse();
        // setContent()はCategoryResponseクラスに定義されているsetterメソッド
        categoryResponse.setContent(categoryDTOs);
        return categoryResponse;
    }

    @Override
    public CategoryDTO createCategory(CategoryDTO categoryDTO) {
        Category category = modelMapper.map(categoryDTO, Category.class);

        Category categoryFromDb = categoryRepository.findByCategoryName(category.getCategoryName());
        if (categoryFromDb != null) {
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
        Category savedCategory = categoryRepository.save(category);
        return modelMapper.map(savedCategory, CategoryDTO.class);
    }

    @Override
    public CategoryDTO deleteCategory(Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
                // orElseThrow() に渡しているのは例外を生成するための Supplier（ラムダ式）
                .orElseThrow(() -> new ResourceNotFoundException("Category", "categoryId", categoryId));

        categoryRepository.delete(category);
        return modelMapper.map(category, CategoryDTO.class);
    }

    @Override
    public CategoryDTO updateCategory(CategoryDTO categoryDTO, Long categoryId) {
        Category savedCategory = categoryRepository.findById(categoryId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Category", "categoryId", categoryId));

        Category category = modelMapper.map(categoryDTO, Category.class);
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
        return modelMapper.map(savedCategory, CategoryDTO.class);

    }
}
