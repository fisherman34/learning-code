package com.packt.cardatabase;

import com.packt.cardatabase.service.UserDetailsServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

//  disable in-memory users
//    @Bean
//    public InMemoryUserDetailsManager userDetailsService() {
//        UserDetails user = User.builder().username("user").
//                password(passwordEncoder().encode("password"))
//                .roles("USER").build();
//        return new InMemoryUserDetailsManager(user);
//    }

    private final UserDetailsServiceImpl userDetailsService;

    public SecurityConfig(UserDetailsServiceImpl userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    /*
    configureGlobal() の役割
    　　→　Spring Securityの認証に使用する「ユーザー情報の取得方法」と「パスワードの検証方法」を設定する
     */
    public void configureGlobal (AuthenticationManagerBuilder auth)
            throws Exception {
        auth.userDetailsService(userDetailsService)  // 「ログインしようとしたユーザーの情報は UserDetailsServiceImpl に問い合わせてください」 と Spring Security に設定しています。
                .passwordEncoder(new BCryptPasswordEncoder());  // 「パスワードのハッシュ化・検証には BCrypt を使ってください」と指定しています。
    }


    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
