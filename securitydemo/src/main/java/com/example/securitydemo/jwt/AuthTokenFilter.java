package com.example.securitydemo.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/*
OncePerRequestFilter
 →　Spring Frameworkが提供するフィルターの基底クラスです。
　　毎回のHTTPリクエストに対して、通常1回だけフィルター処理を実行するためのクラス
　　doFilterInternal() をオーバーライドして、実際のJWT認証処理を実装します。

一般的に言えば、AuthTokenFilter は「毎回のHTTPリクエストを見て、JWTがあればそのユーザーをSpring Securityの
認証情報としてセットする」役割を持つフィルターです。
ざっくり流れはこうです。
 • リクエストが来る
 • AuthTokenFilter が動く
 • Authorization: Bearer ... 形式のJWTを取り出す
 • JwtUtils.validateJwtToken() で署名・期限チェックをする
 • 問題なければ、JWTからユーザー名を取り出す
 • UserDetailsService からそのユーザーの情報を取得する
 • UsernamePasswordAuthenticationToken を作って
 • SecurityContextHolder に入れる
 • その後のAPIやControllerで「このユーザーは認証済みだ」と認識される
つまり、これは「ログイン済みのJWTを受け取り、Spring Securityがそのユーザーとしてアクセスを許可できるようにする入口」です。
AuthTokenFilter は本体の認証ロジックではなく、「JWTを見て認証状態を埋め込む役割」を担っています。
 JwtUtils が JWT の生成/検証を担当し、AuthTokenFilter がその結果を Spring Security に反映させる感じです。
*/
@Component
public class AuthTokenFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private UserDetailsService userDetailsService;

    private static final Logger logger = LoggerFactory.getLogger(AuthTokenFilter.class);


    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        /*
        request.getRequestURI()
         → request は現在処理しているHTTPリクエストです。
           getRequestURI() は、そのHTTPリクエストのURI部分を取得します。
        */
        logger.debug("AuthTokenFilter called for URI: {}", request.getRequestURI());
        try {
            String jwt = parseJwt(request);
            if(jwt !=null && jwtUtils.validateJwtToken(jwt)) {
                String username = jwtUtils.getUsernameFromJwtToken(jwt);
                /*
                userDetailsService
                  → ユーザー情報を取得するためのUserDetailsServiceオブジェクト

                loadUserByUsername(username)
                  → usernameを検索条件としてユーザー情報を取得する
                      ユーザー名、パスワード、権限（Authorities）などを保持します。
                */
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                /*
                UsernamePasswordAuthenticationToken
                    → Spring Securityが認証情報を表現するために使用するクラスです。
                      「このユーザーは誰か」「認証情報は何か」「どの権限を持っているか」
                      などを保持します。

                new UsernamePasswordAuthenticationToken(...)
                    → 認証情報を保持するAuthenticationオブジェクトを生成します。
                */
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,  // JWT認証を行うため、パスワードは不要なのでnullを指定
                        userDetails.getAuthorities()  // ユーザーの権限情報（Authorities）を設定 例えばROLE_USERやROLE_ADMINなど
                );
                // setDetails(...)
                //      → Authenticationオブジェクトに、
                //      現在のHTTPリクエストに関する詳細情報（Details）を設定します。
                authentication.setDetails(
                        /*
                        new WebAuthenticationDetailsSource()
                            → 現在のHTTPリクエストから、
                              Spring Securityで利用する認証関連の詳細情報を作成するためのクラスです。

                        .buildDetails(request)
                            → request（現在のHTTPリクエスト）を渡して、
                              WebAuthenticationDetailsオブジェクトを生成します。

                            このDetailsには、例えば以下のような情報が含まれます。
                              ・リモートアドレス（クライアントのIPアドレス）
                              ・セッションID（存在する場合）

                            ※ JWT認証そのものに必要な情報ではありませんが、
                               Spring Securityが現在のリクエストに関する追加情報を
                               Authenticationに保持できるようにします。
                        */
                        new WebAuthenticationDetailsSource().buildDetails(request));
                /*
                SecurityContextHolder
                    → Spring Securityが現在のHTTPリクエストにおける
                      セキュリティ情報（SecurityContext）を管理するためのクラスです。

                    Spring Securityは、このSecurityContextに登録された
                    Authenticationオブジェクトを参照して、
                    「現在のリクエストは誰として認証されているか」
                    を判断します。


                .getContext()
                    → 現在のSecurityContextを取得します。

                      SecurityContext
                        → 現在のリクエストにおける認証情報を保持する場所です。
                      イメージ：
                        SecurityContext
                          └── Authentication
                                ├── ユーザー情報
                                ├── 認証状態
                                └── Authorities（権限）


                .setAuthentication(authentication)
                    → 先ほど作成したAuthenticationオブジェクトを
                      SecurityContextに設定します。
                      これによってSpring Securityは、
                      「このリクエストは、このユーザーとして認証されている」
                      と認識できるようになります。
                */
                SecurityContextHolder.getContext().setAuthentication(authentication);
                logger.debug("Roles from JWT: {}", userDetails.getAuthorities());
            }
        } catch (Exception e) {
            logger.error("Cannot set user authentication: {}", e.getMessage());
        }
        /*
        filterChain
            → 現在のHTTPリクエストを処理する、
              Spring Security / Servletのフィルターの連鎖（Filter Chain）です。

            HTTPリクエストは1つのフィルターだけで処理されるのではなく、
            複数のフィルターを順番に通過します。

        .doFilter(request, response)
            → 現在のフィルターの処理を終了して、
              リクエストとレスポンスをフィルターチェーンの
              次のフィルターへ渡します。

            request
                → 現在処理しているHTTPリクエスト

            response
                → 現在処理しているHTTPレスポンス
        重要：
            このメソッドを呼ばないと、リクエストが後続のフィルターへ
            進まなくなり、Controllerなどの後続処理も実行されません。
        */
        filterChain.doFilter(request, response);
    }

    private String parseJwt(HttpServletRequest request) {
        String jwt = jwtUtils.getJwtFromHeader(request);
        logger.debug("AuthTokenFilter.java : {}", jwt);
        return jwt;
    }
}
