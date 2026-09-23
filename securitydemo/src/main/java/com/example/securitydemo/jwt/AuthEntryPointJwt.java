package com.example.securitydemo.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
/*
 * AuthEntryPointJwt
 * → JWT認証で「認証されていないユーザー」が
 *   保護されたリソースへアクセスした場合の
 *   エラーレスポンスを処理するクラス
 *
 * AuthenticationEntryPoint
 * → 認証が必要なのにユーザーが認証されていない場合に、
 *   Spring Securityから呼び出される処理を定義するインターフェース
 */
public class AuthEntryPointJwt implements AuthenticationEntryPoint {
    private static final Logger logger = LoggerFactory.getLogger(AuthEntryPointJwt.class);

    /*
     * commence()
     * → AuthenticationEntryPoint インターフェースで定義されているメソッド。
     *
     * → Spring Securityが「認証されていないユーザーが、
     *   認証が必要なリソースへアクセスした」と判断した場合に呼び出される。
     *
     * HttpServletRequest request
     * → クライアントから送られてきたHTTPリクエスト。
     * → リクエストURLやHTTPメソッド、ヘッダーなどの情報を取得できる。
     *
     * HttpServletResponse response
     * → サーバーからクライアントへ返すHTTPレスポンス。
     * → HTTPステータス、レスポンスヘッダー、レスポンスボディなどを設定するために使用する。
     *
     * AuthenticationException authException
     * → 認証処理で発生した例外。
     * → 認証に失敗した原因やエラーメッセージを取得できる。
     *
     * throws IOException
     * → HTTPレスポンスへの書き込みなど、
     *   入出力処理で発生する例外を呼び出し元へ伝える。
     *
     * throws ServletException
     * → Servlet処理中に発生する例外を呼び出し元へ伝える。
     */
    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException, ServletException {
        logger.error("Unauthorized error: {}", authException.getMessage());
        /*
        response
          → クライアントへ返す HTTPレスポンス
        setContentType(...)
          → HTTPレスポンスの Content-Typeを設定するメソッド
        MediaType.APPLICATION_JSON_VALUE
         → "application/json" という文字列
        つまり、実際には「Content-Type: application/json」のようなHTTPヘッダーを設定しています。
         */
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        /*
         * response.setStatus(...)
         * → HTTPレスポンスのステータスコードを設定するメソッド。
         *
         * HttpServletResponse.SC_UNAUTHORIZED
         * → HTTPステータスコード「401 Unauthorized」を表す定数。
         */
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        final Map<String, Object> body = new HashMap<>();  // final を付けると、body という参照変数に、別のオブジェクトを再代入することは禁止
        body.put("status", HttpServletResponse.SC_UNAUTHORIZED);
        body.put("error", "Unauthorized");
        body.put("message", authException.getMessage());  // request.getServletPath() → HTTPリクエストからServletのパスを取得するメソッド。
        body.put("path", request.getServletPath());

        final ObjectMapper mapper = new ObjectMapper();  // ObjectMapper → JavaオブジェクトとJSON形式のデータを相互変換するためのJacksonライブラリのクラス。
        /*
         * mapper.writeValue(...)
         * → ObjectMapperのwriteValue()メソッドを使用して、
         *   JavaオブジェクトをJSON形式に変換し、
         *   指定した出力先へ書き込む。
         *
         * response.getOutputStream()
         * → HTTPレスポンスの出力ストリームを取得する。
         * → サーバーからクライアントへレスポンスボディを
         *   書き込むための出力先。
         *
         * body
         * → JSONに変換するJavaオブジェクト。
         * → このコードではMap<String, Object>型のMap。
         */
        mapper.writeValue(response.getOutputStream(), body);
    }
}
