package com.packt.cardatabase.service;

import com.packt.cardatabase.domain.AppUser;
import com.packt.cardatabase.domain.AppUserRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;


/*
UserDetailsServiceImpl を作る目的は、
Spring Security に「ログインしようとしているユーザーの情報を、どこから取得するか」を教えることです。
例えばユーザーが、

username = "john"
password = "123456"
でログインするとします。

Spring Securityは、

「johnというユーザーは存在するのか？」
「パスワードは何か？」
「このユーザーにはどんなRoleがあるのか？」
を確認する必要があります。

UserDetailsServiceImplを作って、Spring Securityとデータベースの間をつなぎます。

今回の構成を図にすると、こうなります。

ユーザー
   │
   │ username / password
   ↓
Spring Security
   │
   │ "このusernameのユーザー情報をください"
   ↓
UserDetailsServiceImpl
   │
   │ repository.findByUsername(username)
   ↓
AppUserRepository
   │
   ↓
Database
   │
   ↓
AppUser
   │
   │ username / password / role
   ↓
UserDetailsServiceImpl
   │
   │ UserDetailsに変換
   ↓
Spring Security
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final AppUserRepository repository;

    public UserDetailsServiceImpl(AppUserRepository repository) {
        this.repository = repository;
    }

    /*
    Spring Securityはユーザーを認証するときに、loadUserByUsername()メソッドを利用します。
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws
            UsernameNotFoundException {
        Optional<AppUser> user = repository.findByUsername(username);

        /*
        あなたのアプリケーションでは、AppUserという独自のEntityを使っています。
        一方、Spring Securityが認証に使用するのは、UserDetailsです。

        したがって、
        AppUser
           ↓
        UserDetails
        という変換が必要です。下記コードはSpring Security用の UserDetails を作っています。

        注解：
        UserDetailsService は「ユーザー情報を取得する人」
        UserDetails は「取得されたユーザー情報そのもの」です。
        UserDetailsService は「Spring Securityとアプリケーション側のユーザー情報源を
        つなぐための共通インターフェース」

         */
        User.UserBuilder builder = null;
        if (user.isPresent()) {
            AppUser currentUser = user.get();
            builder = org.springframework.security.core.userdetails.
                    User.withUsername(username);
            builder.password(currentUser.getPassword());
            builder.roles(currentUser.getRole());
        } else {
            throw new UsernameNotFoundException("User not found.");
        }
        return builder.build();
    }
}
