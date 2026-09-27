package com.ecommerce.project.controller;

import com.ecommerce.project.model.AppRole;
import com.ecommerce.project.model.Role;
import com.ecommerce.project.model.User;
import com.ecommerce.project.repositories.RoleRepository;
import com.ecommerce.project.repositories.UserRepository;
import com.ecommerce.project.security.jwt.JwtUtils;
import com.ecommerce.project.security.request.LoginRequest;
import com.ecommerce.project.security.request.SignupRequest;
import com.ecommerce.project.security.response.MessageResponse;
import com.ecommerce.project.security.response.UserInfoResponse;
import com.ecommerce.project.security.services.UserDetailsImpl;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    UserRepository userRepository;

    @Autowired
    PasswordEncoder encoder;

    @Autowired
    RoleRepository roleRepository;

    @PostMapping("/signin")
    public ResponseEntity<?> authenticateUser(@RequestBody LoginRequest loginRequest) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(  // ユーザー名とパスワードによる認証情報を表す認証要求（Authentication）を作成
                            loginRequest.getUsername(),  // LoginRequestからログイン時に入力されたユーザー名を取得します。
                            loginRequest.getPassword()));  // LoginRequestからログイン時に入力されたパスワードを取得します。

        } catch (AuthenticationException exception) {   // authenticationManager.authenticate()で認証に失敗した場合に実行されます。
            Map<String, Object> map = new HashMap<>();
            map.put("message", "Bad credentials");
            map.put("status", false);
            return new ResponseEntity<Object>(map, HttpStatus.NOT_FOUND);
        }

        SecurityContextHolder.getContext().setAuthentication(authentication);

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        ResponseCookie jwtCookie = jwtUtils.generateJwtCookie(userDetails);

        List<String> roles = userDetails.getAuthorities().stream()  // ユーザーが持っている権限の一覧を取得します。
                .map(item -> item.getAuthority())  // 各権限（GrantedAuthority）から、権限名の文字列を取得します。
                .collect(Collectors.toList());

        UserInfoResponse response = new UserInfoResponse(userDetails.getId(),
                userDetails.getUsername(), roles, jwtCookie.toString());

        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE,  // HttpHeaders.SET_COOKIE：HTTPヘッダーの「Set-Cookie」を表す定数。
                jwtCookie.toString())  // ResponseCookieオブジェクトを文字列に変換して、HTTPレスポンスヘッダーに追加します。
                .body(response);  // HTTPレスポンスのボディ（本文）にresponseを設定する。
    }

    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@Valid @RequestBody SignupRequest signupRequest) {
        if (userRepository.existsByUserName(signupRequest.getUsername())) {
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("Error: Username is already taken!"));
        }

        if (userRepository.existsByEmail(signupRequest.getEmail())) {
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("Error: Email is already in use!"));
        }

        // Create new user's account
        User user = new User(signupRequest.getUsername(),
                signupRequest.getEmail(),
                encoder.encode(signupRequest.getPassword())
        );

        Set<String> strRoles = signupRequest.getRole();
        Set<Role> roles = new HashSet<>();

        if (strRoles == null) {
            Role userRole = roleRepository.findByRoleName(AppRole.ROLE_USER)
                    .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
            roles.add(userRole);
        } else {
            strRoles.forEach(role -> {
                switch (role) {
                    case "admin":
                        Role adminRole = roleRepository.findByRoleName(AppRole.ROLE_ADMIN)
                                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
                        roles.add(adminRole);
                        break;
                    case "seller":
                        Role sellerRole = roleRepository.findByRoleName(AppRole.ROLE_SELLER)
                                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
                        roles.add(sellerRole);
                        break;
                    default:
                        Role userRole = roleRepository.findByRoleName(AppRole.ROLE_USER)
                                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
                        roles.add(userRole);
                }
            });
        }
        user.setRoles(roles);
        userRepository.save(user);

        return ResponseEntity.ok(new MessageResponse("User registered successfully!"));
    }

    /*
    Authentication authentication は、HTTPリクエストのパラメータとしてクライアントから直接送られてくるものではありません。
    Spring MVC が Spring Security の SecurityContext から取得し、メソッド引数に渡します。
    このアプリでは、次の流れで設定されます。
    1. /api/auth/signin でログインすると、JWT を含む Cookie がレスポンスに設定されます。
    2. 次回以降のリクエストでブラウザがその Cookie を送ると、AuthTokenFilter が JWT を検証します。
    3. 検証に成功すると、フィルターが SecurityContextHolder に Authentication を設定します。
    4. /api/auth/username の処理時に、Spring MVC がその Authentication を currentUserName に渡します。
       authentication.getName() は認証されたユーザー名を返します。
     */
    @GetMapping("/username")
    public String currentUserName(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            return authentication.getName();
        } else {
            return "";
        }
    }

    @GetMapping("/user")
    public ResponseEntity<?> getUserDetails(Authentication authentication) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        List<String> roles = userDetails.getAuthorities().stream()  // ユーザーが持っている権限の一覧を取得します。
                .map(item -> item.getAuthority())  // 各権限（GrantedAuthority）から、権限名の文字列を取得します。
                .collect(Collectors.toList());

        UserInfoResponse response = new UserInfoResponse(userDetails.getId(),
                userDetails.getUsername(), roles);

        return ResponseEntity.ok().body(response);
    }

    // ログイン中のユーザーをログアウトさせるために、JWTを保存しているCookieを削除するメソッド
    @PostMapping("/signout")
    public ResponseEntity<?> signoutUser() {
        ResponseCookie cookie = jwtUtils.getCleanJwtCookie();
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE,
                        cookie.toString())
                .body(new MessageResponse("You've been signed out!"));
    }
}
