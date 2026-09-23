package com.example.securitydemo;

import com.example.securitydemo.jwt.JwtUtils;
import com.example.securitydemo.jwt.LoginRequest;
import com.example.securitydemo.jwt.LoginResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
public class GreetingsController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtils jwtUtils;

    @GetMapping("/hello")
    public String sayHello() {
        return "Hello!";
    }

    @PreAuthorize("hasRole('USER')")
    @GetMapping("/user")
    public String userEndpoint() {
        return "Hello user!";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin")
    public String adminEndpoint() {
        return "Hello admin!";
    }

    @PostMapping("/signin")
    public ResponseEntity<?> authenticateUser(@RequestBody LoginRequest loginRequest) {
        Authentication authentication;
        try {
            /*
            authenticationManager.authenticate()
              → Spring Securityで渡された認証情報が正しいか確認し、認証結果を返すメソッド
            */
             authentication = authenticationManager.authenticate(
                     new UsernamePasswordAuthenticationToken(  // ユーザー名とパスワードによる認証情報を表す認証要求（Authentication）を作成
                             loginRequest.getUsername(),  // LoginRequestからログイン時に入力されたユーザー名を取得します。
                             loginRequest.getPassword()));  // LoginRequestからログイン時に入力されたパスワードを取得します。

        } catch (AuthenticationException exception) {   // authenticationManager.authenticate()で認証に失敗した場合に実行されます。
            Map<String, Object> map = new HashMap<>();
            map.put("message", "Bad credentials");
            map.put("status", false);
            return new ResponseEntity<Object>(map,HttpStatus.NOT_FOUND);
        }

        SecurityContextHolder.getContext().setAuthentication(authentication);
        /*
        Authenticationオブジェクトから、認証されたユーザーの情報（Principal）を取得します。
        getPrincipal()の戻り値はObject型なので、UserDetails型にキャストします。
        UserDetailsには、ユーザー名や権限（ROLE_USER、ROLE_ADMINなど）の情報が含まれています。

        */
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String jwtToken = jwtUtils.generateTokenFromUsername(userDetails);
        List<String> roles = userDetails.getAuthorities().stream()  // ユーザーが持っている権限の一覧を取得します。
                .map(item -> item.getAuthority())  // 各権限（GrantedAuthority）から、権限名の文字列を取得します。
                .collect(Collectors.toList());
        LoginResponse response = new LoginResponse(userDetails.getUsername(),
                jwtToken, roles);

        return ResponseEntity.ok(response);
    }
}
