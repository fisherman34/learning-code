package com.social.media.models;

import jakarta.persistence.*;

@Entity
public class SocialProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // @OneToOne は、UserエンティティとProfileエンティティが
    // 1対1（One-to-One）の関係であることを指定するアノテーション

    // mappedBy = "socialProfile" は、
    // 「このリレーションの管理者（所有側）はSocialUser側にあり、
    //  SocialUserクラスのsocialProfileフィールドによって
    //  この関連付けが管理されている」ことを指定する
    @OneToOne()

    // @JoinColumn は、エンティティ間のリレーションを結び付けるための
    // 外部キーを格納するカラムを指定するアノテーション
    // name = "social_user" は、SocialProfileテーブルに作成する
    // 外部キーカラムの名前を「social_user」と指定している
    @JoinColumn(name = "social_user")
    private SocialUser user;
}
