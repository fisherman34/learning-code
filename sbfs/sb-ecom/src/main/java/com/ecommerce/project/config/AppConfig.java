package com.ecommerce.project.config;

import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// @Configuration: 
// AppConfig を Springの設定クラスとして登録します。
@Configuration
public class AppConfig {

    // @Bean は、このメソッドが返すオブジェクトを
    // Springコンテナに「Bean」として登録することを指定します。
    //
    // この場合、new ModelMapper() で作成した ModelMapper オブジェクトが
    // Springによって管理されるようになります。
    //
    // Springコンテナに登録されたModelMapperは、
    // 他のクラスから@Autowiredなどを使って注入できます。
    @Bean
    public ModelMapper modelMapper() {
        return new ModelMapper();
    }
}
