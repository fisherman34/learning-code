package com.social.media.models;

import jakarta.persistence.*;

@Entity
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne

    // @JoinColumn は、エンティティ間のリレーションを結び付けるための
    // 外部キーを格納するカラムを指定するアノテーション
    //
    // name = "user_id" は、Postテーブルに作成する
    // 外部キーカラムの名前を「user_id」と指定している
    //
    // このuser_idによって、PostとSocialUserを関連付ける
    @JoinColumn(name = "user_id")
    private SocialUser socialUser;
}
