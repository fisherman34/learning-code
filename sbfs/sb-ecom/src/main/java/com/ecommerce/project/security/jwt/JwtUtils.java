package com.ecommerce.project.security.jwt;

import com.ecommerce.project.security.services.UserDetailsImpl;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.util.WebUtils;

import javax.crypto.SecretKey;
import java.security.Key;
import java.util.Date;

@Component
public class JwtUtils {

    private static final Logger logger = LoggerFactory.getLogger(JwtUtils.class);

    @Value("${spring.app.jwtSecret}")
    private String jwtSecret;

    @Value("${spring.app.jwtExpirationMs}")
    private int jwtExpirationMs;

    @Value("${spring.ecom.app.jwtCookieName}")
    private String jwtCookie;

//    public String getJwtFromHeader(HttpServletRequest request) {
//
//        String bearerToken = request.getHeader("Authorization");
//
//        logger.debug("Authorization header: {}", bearerToken);
//        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
//
//            return bearerToken.substring(7);
//        }
//        return null;
//
//    }

    // HttpServletRequest は、クライアント（ブラウザなど）からサーバーへ送られてきた「HTTPリクエスト」をJavaで扱うためのオブジェクトです。
    public String getJwtFromCookies(HttpServletRequest request) {
        /*
        jwtCookie：取得したいCookieの名前（application.propertiesなどで設定）
        WebUtils.getCookie(...)：HTTPリクエストの中から、jwtCookieという名前のCookieを検索する
        Cookieが存在する場合は、そのCookieオブジェクトを返す
         */
        Cookie cookie = WebUtils.getCookie(request, jwtCookie);

        if (cookie != null) {
//            System.out.println("Cookie: " + cookie.getValue());
            return cookie.getValue();
        } else {
            return null;
        }
    }

    public ResponseCookie generateJwtCookie(UserDetailsImpl userPrincipal) {
        String jwt = generateTokenFromUsername(userPrincipal.getUsername());
        /*
         * ResponseCookie：
         * サーバーからクライアント（ブラウザ）へ送信するCookieを表すオブジェクト。
         *
         * ResponseCookie.from(jwtCookie, jwt)：
         * Cookieの「名前」と「値」を指定して、ResponseCookieを作成する。
         *
         * jwtCookie：
         * Cookieの名前。
         * application.propertiesなどで設定したCookie名が入っている。
         *
         * jwt：
         * Cookieに保存するJWT（JSON Web Token）。
         *
         * 例えば、
         * jwtCookie = "jwtCookie"
         * jwt = "eyJhbGciOiJIUzI1NiJ9..."
         *
         * の場合、
         *
         * jwtCookie=eyJhbGciOiJIUzI1NiJ9...
         *
         * というCookieを作成する。
         */
        ResponseCookie cookie = ResponseCookie.from(jwtCookie, jwt)
                .path("/api")  // Cookieを送信する対象のURLパスを「/api」に設定する。
                .maxAge(24 * 60 * 60)  // Cookieの有効期限を1日（秒単位）に設定
                .httpOnly(false)  // false：JavaScriptからこのCookieを読み取ることができる
                .build();  // ここまでに設定したCookieの内容を確定し、ResponseCookieオブジェクトを生成する。
        return cookie;
    }

    // 現在ブラウザに保存されているJWT Cookieを削除するためのCookieを作成する
    public ResponseCookie getCleanJwtCookie() {
        ResponseCookie cookie = ResponseCookie.from(jwtCookie, null)
                .path("/api")  // Cookieを送信する対象のURLパスを「/api」に設定する。
                .build();  // ここまでに設定したCookieの内容を確定し、ResponseCookieオブジェクトを生成する。
        return cookie;
    }

    public String generateTokenFromUsername(String username) {
        return Jwts.builder()
                .subject(username)
                .issuedAt(new Date())
                .expiration(new Date((new Date()).getTime() + jwtExpirationMs))
                .signWith(key())
                .compact();
    }

    public String getUsernameFromJwtToken(String token) {
        return Jwts.parser()
                .verifyWith((SecretKey) key())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();

    }

    public Key key() {

        return Keys.hmacShaKeyFor(

                Decoders.BASE64.decode(jwtSecret)
        );
    }

    public boolean validateJwtToken(String authToken) {
        try {
            System.out.println("Validate");
            Jwts.parser()
                    .verifyWith((SecretKey) key())
                    .build()
                    .parseSignedClaims(authToken);

            return true;
        } catch (MalformedJwtException e) {
            logger.error("Invalid JWT token: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            logger.error("JWT token is expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            logger.error("JWT token is unsupported: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            logger.error("JWT claims string is empty: {}", e.getMessage());
        }
        return false;
    }

}
