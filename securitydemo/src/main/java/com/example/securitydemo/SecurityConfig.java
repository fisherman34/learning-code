package com.example.securitydemo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.provisioning.JdbcUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;


import javax.sql.DataSource;

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
// // 例えば、以下のようなアノテーションを
// ServiceやControllerなどのメソッドに付けられるようになります。
//
// @PreAuthorize("hasRole('ADMIN')")
// public void deleteUser() {
//     ...
// }
//
// 上記の場合、
// 「ROLE_ADMIN」の権限を持っているユーザーだけが
// deleteUser()メソッドを実行できます。
//
// また、@PostAuthorize、@PreFilter、@PostFilterなどの
// メソッドセキュリティ用アノテーションも利用できます。
//
// @EnableMethodSecurityを指定しない場合、
// これらのメソッドセキュリティ機能は基本的に有効になりません。
//
// なお、@EnableWebSecurityが主に
// 「HTTPリクエストに対するセキュリティ設定」を有効にするのに対して、
// @EnableMethodSecurityは
// 「Javaメソッドに対するセキュリティ設定」を有効にします。
@EnableWebSecurity

// @EnableMethodSecurity:
// Spring Securityの「メソッドセキュリティ機能」を有効にします。
//
// メソッドセキュリティとは、
// HTTPリクエストのURL単位ではなく、
// Javaのメソッド単位で「誰が実行できるのか」という
// 認可（Authorization）のルールを設定する機能です。
@EnableMethodSecurity
public class SecurityConfig {

    @Autowired
    /*
    DataSourceは、データベースへの接続を取得するための
    インターフェースです。

    Spring Bootでは、application.propertiesなどに設定した
    データベース接続情報をもとに、通常Spring BootがDataSourceを
    Beanとして自動的に生成します。

    例えば、MySQLやPostgreSQLなどのデータベースを使用する場合、
    DataSourceを通してデータベースへの接続を取得できます。
     */
    DataSource dataSource;

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
        http.authorizeHttpRequests((requests) ->

                /*
                 requestMatchers("/h2-console/**")
                 → セキュリティの認可ルールを適用する対象となる
                 HTTPリクエストのURLパターンを指定します。

                 "/h2-console/**"
                 → H2 DatabaseのWebコンソールに関係する
                 すべてのURLを対象にします。

                 「**」は、/h2-console/以下にある
                 すべてのパスを表します。

                 例えば、以下のようなURLが対象になります。

                 /h2-console/
                 /h2-console/login.do
                 /h2-console/xxxx

                 つまり、H2 Consoleにアクセスするために必要な
                 HTTPリクエストをまとめて対象にしています。

                 requestMatchers()は、
                 「このURLに対して、どのような認可ルールを適用するか」
                 を指定するためのメソッドです。

                  permitAll()
                  → このURLパターンへのアクセスを
                  「すべてのユーザーに許可する」ことを意味します。
                 */
                requests.requestMatchers("/h2-console/**").permitAll()
                        .anyRequest().authenticated());
        // sessionManagement(...)
        // → Spring SecurityにおけるHTTPセッションの管理方法を設定します。
        //
        // sessionCreationPolicy(...)
        // → 「認証情報などを保持するためのHTTPセッションを、
        //    Spring Securityがどのように扱うか」を指定します。
        //
        // SessionCreationPolicy.STATELESS
        // → Spring SecurityがHTTPセッションを作成・利用しない設定です。
        // つまり、1回目のHTTPリクエストで認証した情報を
        // HTTPセッションに保存して、次回のリクエストで再利用することはしません。
        // そのため、各HTTPリクエストは基本的に独立して処理され、
        // リクエストごとに認証情報（このコードではHTTP Basic認証の
        // ユーザー名・パスワードなど）が送信される必要があります。
        // REST APIなど、サーバー側でログイン状態をセッション管理しない
        // ステートレスなアプリケーションでよく使用されます。
        http.sessionManagement(session
                -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
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

        /*
        HTTPレスポンスヘッダーに関するセキュリティ設定を行います。

        headers(...)
        → HTTPレスポンスに付与される
          セキュリティ関連のHTTPヘッダーを設定します。

        headers.frameOptions(...)
        → HTTPレスポンスの「X-Frame-Options」ヘッダーに関する
          設定を行います。

        X-Frame-Options
        → Webページを <iframe> の中に表示することを
          許可するかどうかを制御するHTTPレスポンスヘッダーです。

        デフォルトでは、Spring Securityによって
        iframe内への表示が制限される設定になっています。

        sameOrigin
        → 「同じオリジンからのiframe表示だけを許可する」
          という設定です。

        例えば、
        http://localhost:8080 で動作しているSpring Bootアプリケーションから
        http://localhost:8080/h2-console をiframeで表示する場合は許可されます。

        一方、異なるオリジンにあるWebサイトから
        H2 Consoleをiframeに埋め込むことは許可されません。

        HeadersConfigurer.FrameOptionsConfig::sameOrigin
        → sameOrigin()メソッドを「メソッド参照」で指定しています。

        以下のラムダ式とほぼ同じ意味です。

        headers.frameOptions(frameOptions ->
                frameOptions.sameOrigin());

        この設定は、H2 Consoleがiframeを使用して画面を表示するため、
        H2 Consoleを正常に利用できるようにする目的で設定しています。
         */
        http.headers(headers ->
                headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin));

        /*
        CSRF（Cross-Site Request Forgery）
        → 「クロスサイト・リクエスト・フォージェリ」と呼ばれる
          Webアプリケーションに対する攻撃手法です。

        例えば、ユーザーがWebサイトAにログインしている状態で、
        悪意のあるWebサイトBを開いた場合、

        WebサイトBからWebサイトAに対して
        ユーザー本人が意図していないリクエストを
        送信させられる可能性があります。

        このような攻撃を防ぐために、
        Spring SecurityではCSRF対策が用意されています。


        csrf(...)
        → Spring SecurityのCSRF対策に関する設定を行います。


        AbstractHttpConfigurer::disable
        → 「disable」というメソッドを
          メソッド参照として指定しています。

        以下のようなラムダ式とほぼ同じ意味です。

        csrf(csrf ->
                csrf.disable());

        つまり、

        「Spring SecurityのCSRF保護機能を無効にする」

        という設定です。


        このコードでは、主に学習用のSpring Bootアプリケーションとして
        CSRF保護を無効にしています。

        特にREST APIを作成する場合など、
        セッションベースの認証を使用せず、
        HTTP Basic認証やJWTなどを使用する
        ステートレスな構成では、
        CSRF保護を無効にする構成がよくあります。

        ただし、CSRF保護を無効にすることが
        常に安全というわけではありません。

        CookieやHTTPセッションを利用して認証状態を管理する
        Webアプリケーションでは、
        CSRF対策を適切に行う必要があります。
        */
        http.csrf(AbstractHttpConfigurer::disable);

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

    @Bean
    public UserDetailsService userDetailsService() {

        // UserDetails
        // → Spring Securityが認証処理で使用する「ユーザー情報」を表すインターフェースです。
        //
        // UserDetailsには、主に以下のような認証・認可に必要な情報が含まれます。
        // ・ユーザー名（username）
        // ・パスワード（password）
        // ・権限（Authorities / Roles）
        // ・アカウントが有効かどうか
        // ・アカウントがロックされているかどうか
        // ・パスワードの有効期限
        //
        // ここでは、Userクラスを使用してUserDetailsオブジェクトを作成しています。

        // withUsername("user1")
        // → ユーザー名を「user1」に設定します。
        //
        // User.withUsername()は、Spring Securityが提供する
        // Userオブジェクトの生成用メソッドです。
        //
        // このメソッドを呼び出すと、User.UserBuilderが返され、
        // そのBuilderに対してパスワードやロールなどを設定していきます。
        UserDetails user1 = User.withUsername("user1")
                // password("...")
                // → ユーザーのパスワードを設定します。
                //
                // 「{noop}」は、Spring Securityに対して
                // 「このパスワードにはPasswordEncoderによる暗号化・ハッシュ化を
                // 行わず、そのまま扱う」ということを示す
                // PasswordEncoderの識別子です。
                //
                // したがって、ここではパスワードとして
                // 「password1」を使用しています。 //
                // ※ {noop} は学習・テスト用には便利ですが、
                // 本番環境で平文パスワードを使用することは推奨されません。
                .password("{noop}password1")

                // roles("USER")
                // → このユーザーに「USER」というロールを付与します。
                //
                // Spring Securityでは、roles("USER")と指定すると、
                // 内部的には「ROLE_USER」という権限として扱われます。
                //
                // 例えば、
                // hasRole("USER")
                // と設定した場合、このユーザーはアクセスできます。
                //
                // roles("USER")では「ROLE_」を自分で付ける必要はありません。
                // Spring Securityが自動的に「ROLE_」を付けます。
                .roles("USER")
                // build()
                // → これまでBuilderに設定した
                // ・ユーザー名
                // ・パスワード
                // ・ロール
                // などの情報を使用して、UserDetailsオブジェクトを完成させます。
                //
                // build()の戻り値がUserDetails型なので、
                // 「UserDetails user1」に代入できます。
                .build();

        UserDetails admin = User.withUsername("admin")
                .password("{noop}adminPass") // {noop}はパスワードを平文で扱うことを示す
                .roles("ADMIN")
                .build();

        /*
        JdbcUserDetailsManager
        → Spring Securityが提供しているUserDetailsServiceの実装クラスです。

        InMemoryUserDetailsManagerとは異なり、
        ユーザー情報をメモリ上ではなくデータベースに保存・取得します。

        そのため、Spring Securityの認証時に、
        データベースに保存されているユーザー名・パスワード・権限などを
        JDBCを使用して検索できます。

        new JdbcUserDetailsManager(dataSource)
        → JdbcUserDetailsManagerのオブジェクトを新しく作成します。

        dataSource
        → データベースへの接続を取得するためのDataSourceオブジェクトです。

        前の部分で、SpringによってDataSourceが自動的に注入されています。

        ここでDataSourceをJdbcUserDetailsManagerに渡すことで、
        JdbcUserDetailsManagerがそのDataSourceを使用して
        データベースへアクセスできるようになります。

         */
        JdbcUserDetailsManager userDetailsManager = new JdbcUserDetailsManager(dataSource);

        /*
        createUser(...)
        → UserDetailsとして渡されたユーザー情報を
        データベースに新規登録するためのメソッドです。

        user1
        → 先ほどUser.withUsername("user1")などを使用して作成した
        UserDetailsオブジェクトです。

        user1には、主に以下の情報が含まれています。

        ・ユーザー名：user1
        ・パスワード：password1
        ・ロール：ROLE_USER

        このcreateUser()を実行すると、
        JdbcUserDetailsManagerがDataSourceを使用してDBへ接続し、
        user1のユーザー情報をSpring Security用のテーブルに登録します。
        */
        userDetailsManager.createUser(user1);
        userDetailsManager.createUser(admin);
        return userDetailsManager;

        // InMemoryUserDetailsManager
        // → UserDetailsをメモリ上で管理するUserDetailsServiceの実装クラスです。
        //
        // ここでは、先ほど作成した
        // 「user1」と「admin」の2人のユーザー情報をメモリ上に登録しています。
        // そのため、データベースを使用せずにSpring Securityの認証機能をテストできます。
//        return new InMemoryUserDetailsManager(user1, admin);
    }
}
