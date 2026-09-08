package com.social.media.models;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// @Entity は、このクラスをJPAのエンティティ（DBのテーブルと対応するクラス）
// として扱うことを指定するアノテーション
@Entity
public class SocialUser {
    // @Id は、このフィールドをエンティティの主キー（Primary Key）として
    // 扱うことを指定するアノテーション
    @Id
    // @GeneratedValue は、主キー（id）の値を自動的に生成することを指定するアノテーション
    // strategy = GenerationType.IDENTITY は、DB側の自動採番機能（IDENTITY）を利用して
    // idの値を自動的に採番することを指定する
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(mappedBy = "user")
//    @JoinColumn(name = "social_profile_id")
    private SocialProfile socialProfile;

    @OneToMany(mappedBy = "socialUser")
    private List<Post> posts = new ArrayList<>();

    @ManyToMany
    @JoinTable(
        name = "user_group", // 中間テーブルの名前を指定
        joinColumns = @JoinColumn(name = "user_id"), // SocialUser側の外部キーを指定
        inverseJoinColumns = @JoinColumn(name = "group_id") // Group側の外部キーを指定
    )
    private Set<SocialGroup> groups = new HashSet<>();
}
