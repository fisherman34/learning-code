package com.example.securitydemo.jwt;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.security.Key;
import java.util.Date;

/*
JwtUtils は、Spring Security アプリケーションにおいて JSON Web Token（JWT）を扱うためのヘルパークラスです。
簡単に言うと、主に次の 4 つの役割を担っています。

HTTP リクエストヘッダーから JWT を読み取る
    通常は Authorization: Bearer <token> から取得します
トークンを生成する
    ユーザー情報（ユーザー名など）を受け取り、秘密鍵で署名します
トークンからユーザーの身元情報を取り出す
    JWT のペイロードからユーザー名を読み取ります
トークンを検証する
    トークンが正しく署名されているか確認します
    期限切れ、不正な形式（malformed）、非対応（unsupported）などでないかをチェックします
*/

/*
@Component
 → Springに対して、
   「このJwtUtilsクラスをSpringの管理対象（Bean）として登録してください」
   と指定するアノテーションです。

   @Componentを付けることで、Spring Bootの起動時に
   JwtUtilsのインスタンスがSpringによって生成・管理されます。

   そのため、他のクラスからJwtUtilsを使用するときに、
   自分でnew JwtUtils()する必要がありません。

   例えば、以下のようにDI（依存性注入）によって
   JwtUtilsを受け取ることができます。

   private final JwtUtils jwtUtils;

   public JwtAuthenticationFilter(JwtUtils jwtUtils) {
       this.jwtUtils = jwtUtils;
   }

   SpringがJwtUtilsのインスタンスを作成し、
   JwtAuthenticationFilterに渡してくれます。
*/
@Component
public class JwtUtils {
    /*
    static インスタンスではなくクラスに属する
    Loggerは通常、JwtUtils クラスについてログを出力するためのオブジェクトです。
    logger は JwtUtils の各インスタンスごとに別々に存在する必要がありません。
    クラスで1つのLoggerを共有する形が一般的です。

    Logger
     → SLF4Jが提供するロギング用のインターフェースです。
       logger.info()、logger.error()などを使用して、
       アプリケーションのログを出力できます。

    LoggerFactory.getLogger(JwtUtils.class)
     → LoggerFactoryに対して、
       「JwtUtilsクラス用のLoggerを取得してください」
       と指定しています。
     ※　LoggerFactoryはLoggerオブジェクトを作って取得するため
     　　の「工場（Factory）」です。
    */
    private static final Logger logger = LoggerFactory.getLogger(JwtUtils.class);

    /*
    @Value("${spring.app.jwtSecret}")
     → Spring Bootの設定ファイルから(application.properties)、
       「spring.app.jwtSecret」という名前の設定値を取得し、
       jwtSecretフィールドに代入します。
    */
    @Value("${spring.app.jwtSecret}")
    private String jwtSecret;

    @Value("${spring.app.jwtExpirationMs}")
    private int jwtExpirationMs;

    // Getting jwt from header
    /*
    HttpServletRequest request
     → HTTPリクエストを表すオブジェクトを引数として受け取ります。

    HttpServletRequestには、
    HTTPリクエストのヘッダー、URL、HTTPメソッド、
    パラメータなどの情報が含まれています。
     */
    public String getJwtFromHeader(HttpServletRequest request) {
        /*
        request.getHeader("Authorization")
        → HTTPリクエストの「Authorization」ヘッダーの値を取得します。

        Authorizationヘッダーには、通常、以下のような形式で
        JWTが含まれています。

        Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...

        getHeader("Authorization")
        → 「Authorization」という名前のHTTPヘッダーを検索し、
        その値である

        「Bearer eyJhbGciOiJIUzI1NiJ9...」

        という文字列を取得します。

        取得した文字列をbearerToken変数に代入します。
        */
        String bearerToken = request.getHeader("Authorization");

        /*
        logger.debug(...)
         → DEBUGレベルのログを出力します。

           DEBUGは、プログラムの動作を詳しく確認したいときに使用する
           ログレベルです。

        "Authorization header: {}"
         → ログに出力するメッセージのテンプレートです。

           {} はプレースホルダー（値を埋め込む場所）です。

        bearerToken
         → {} の部分に実際のbearerTokenの値が入ります。
        */
        logger.debug("Authorization header: {}", bearerToken);
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            /*
            bearerToken.substring(7)
             → bearerTokenの7文字目以降を切り出します。
            */
            return bearerToken.substring(7);
        }
        return null;

    }
    // Generating token from username
    /*
    UserDetailsには、認証されたユーザーの
    ユーザー名、パスワード、権限（Authorities）などの
    ユーザー情報が含まれています。
    */
    public String generateTokenFromUsername(UserDetails userDetails) {
        String username = userDetails.getUsername();
        return Jwts.builder() //Jwts → JJWT（Java JWT）ライブラリが提供するクラス、　builder()→ JWTに必要な情報を設定ためのBuilderオブジェクト
                .subject(username)  // → JWTの「Subject（sub）」に、usernameの値を設定します。
                .issuedAt(new Date())  // → JWTの「Issued At（iat）」に、現在の日時を設定します。 new Date() → Javaの現在日時を表すDateオブジェクトを生成
                .expiration(new Date((new Date()).getTime() + jwtExpirationMs))  //  getTime() → Unix Epochから何ミリ秒経過したかという数値（long型）を取得
                .signWith(key())  // JWTの署名に使用する秘密鍵を設定します。
                .compact();  // ここまでBuilderに設定した情報から、最終的なJWT（JSON Web Token）を文字列として生成します。
    }

    // getting username from jwt token
    public String getUsernameFromJwtToken(String token) {
        return Jwts.parser()  // Jwts.parser() → JWTを読み取り、 JWTの署名やPayloadなどを解析・検証するための Parser Builderを取得するメソッド
                .verifyWith((SecretKey) key())  // verifyWith(...)→ JWTの署名を検証するための検証鍵を設定するメソッドです。
                .build()  // ここまで設定した内容から、 実際にJWTを解析できるParserオブジェクトを完成させます。
                .parseSignedClaims(token)  // 署名付きJWTを解析し、署名の検証を行ったうえで、 JWTに含まれているClaims（Payload）を取得します。
                .getPayload()  // getPayload() → JWTのPayloadに含まれているClaimsを取得します。
                .getSubject();  // getSubject()→ JWTの標準Claimである「sub（Subject）」の値をString型で取得します。

    }
    // generate signing key

    public Key key() {
        /*
        Keys.hmacShaKeyFor(...)
         → JJWTが提供するメソッドです。

          Base64デコードされた秘密鍵のバイト配列を受け取り、
          HMAC方式のJWT署名に使用できる秘密鍵（SecretKey）を生成します。
        */
        return Keys.hmacShaKeyFor(
                /*
                jwtSecret
                 → JWTの署名に使用する秘密鍵を、
                   Base64形式の文字列として保持しています。

                Decoders.BASE64
                 → JJWTが提供するBase64デコーダーです。

                decode(jwtSecret)
                 → jwtSecretのBase64文字列をデコードし、
                   元のバイトデータ（byte[]）に変換します。
                */
                Decoders.BASE64.decode(jwtSecret)
        );
    }
    // validate jwt token
    // authToken = HTTPリクエストの Authorization: Bearer <JWT> から
    // Bearer を取り除いた、JWT本体の文字列
    public boolean validateJwtToken(String authToken) {
        try {
            System.out.println("Validate");
            Jwts.parser()  // JWTの解析を行うParserを取得します。
                    .verifyWith((SecretKey) key())  // JWTの署名を検証するための秘密鍵を設定します。
                    .build()  // Parserを構築します。
                    .parseSignedClaims(authToken);  // JWTを解析し、署名の検証を行います。

            return true;  // JWTが有効である場合はtrueを返します。
        } catch (MalformedJwtException e) {
            logger.error("Invalid JWT token: {}", e.getMessage());  // JWTが無効な場合はエラーログを出力します。
        } catch (ExpiredJwtException e) {
            logger.error("JWT token is expired: {}", e.getMessage());  // JWTが期限切れの場合はエラーログを出力します。
        } catch (UnsupportedJwtException e) {
            logger.error("JWT token is unsupported: {}", e.getMessage());  // JWTがサポートされていない場合はエラーログを出力します。
        } catch (IllegalArgumentException e) {
            logger.error("JWT claims string is empty: {}", e.getMessage());  // JWTのクレームが空の場合はエラーログを出力します。
        }
        return false;  // JWTが無効である場合はfalseを返します。
    }
}
