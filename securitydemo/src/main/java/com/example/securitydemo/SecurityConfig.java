package com.example.securitydemo;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration

// @EnableWebSecurity:Spring SecurityのWebセキュリティ機能を有効にします。
//
// これによって、Spring Securityのセキュリティフィルターなどが
// Webアプリケーションに適用され、HTTPリクエストに対する
// 認証・認可などのセキュリティ処理を行えるようになります。
//
// また、このクラスに記述したSecurityFilterChainなどの
// Spring Securityの設定を利用できるようにします。
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {

        // HTTPリクエストに対する「認可（Authorization）」のルールを設定します。
        //
        // authorizeHttpRequests(...)
        // → HTTPリクエストごとに、
        //   「誰がアクセスできるのか」というルールを設定します。
        //
        // (requests) -> ...
        // → ラムダ式です。
        //   authorizeHttpRequests()から渡されたrequestsオブジェクトに対して、
        //   アクセスルールを設定しています。
        //
        // anyRequest()
        // → アプリケーションへの「すべてのHTTPリクエスト」を対象にします。
        //
        // authenticated()
        // → 認証済み（ログイン済み）のユーザーだけアクセスを許可します。
        http.authorizeHttpRequests((requests) -> requests.anyRequest().authenticated());
        //http.formLogin(withDefaults());

        // HTTP Basic認証を有効にします。
        //
        // HTTP Basic認証では、クライアントがHTTPリクエストを送る際に
        // ユーザー名とパスワードを送信して認証します。
        //
        // withDefaults()
        // → Spring Securityが用意しているデフォルト設定を使用します。
        //
        // 例えば、認証されていないユーザーが
        // 保護されたURLへアクセスすると、Spring Securityは
        // HTTP Basic認証を要求します。
        http.httpBasic(withDefaults());

        // ここまで設定したSpring Securityの内容をもとに、
        // SecurityFilterChainを構築して返します。
        //
        // @Beanが付いているため、returnされたSecurityFilterChainは
        // SpringコンテナにBeanとして登録されます。
        //
        // Spring Securityは、このSecurityFilterChainを使用して
        // 実際のHTTPリクエストに対するセキュリティ処理を行います。
        return http.build();
    }
}
