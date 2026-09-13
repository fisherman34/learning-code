package com.social.media.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.*;

// @Entity は、このクラスをJPAのエンティティ（DBのテーブルと対応するクラス）
// として扱うことを指定するアノテーション
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SocialUser {
    // @Id は、このフィールドをエンティティの主キー（Primary Key）として
    // 扱うことを指定するアノテーション
    @Id
    // @GeneratedValue は、主キー（id）の値を自動的に生成することを指定するアノテーション
    // strategy = GenerationType.IDENTITY は、DB側の自動採番機能（IDENTITY）を利用して
    // idの値を自動的に採番することを指定する
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // cascade = { ... }
    // → SocialUserに対して行った操作を、関連するSocialProfileにも
    // 連鎖（cascade）させることを指定する
    //
    // CascadeType.PERSIST
    // → SocialUserを新規登録（persist）したとき、
    //   関連するSocialProfileも一緒にDBへ登録する
    //
    // CascadeType.MERGE
    // → SocialUserを更新（merge）したとき、
    //   関連するSocialProfileも一緒に更新する
    //
    // CascadeType.REMOVE
    // → SocialUserを削除したとき、
    //   関連するSocialProfileも一緒に削除する
    @OneToOne(mappedBy = "user", cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE})
    private SocialProfile socialProfile;

    @OneToMany(mappedBy = "socialUser")
    private List<Post> posts = new ArrayList<>();

    // fetch = FetchType.EAGER
    // → SocialUserをDBから取得したときに、
    //   関連するSocialGroup（groups）も同時に取得することを指定する
    //
    //   例えば、
    //   socialUserRepository.findById(id)
    //   でSocialUserを取得すると、
    //   groupsも同時にロードされる
    //
    //   EAGER = 「すぐに取得する（即時ロード）」
    //
    //   反対に、FetchType.LAZYの場合は、
    //   groupsが実際に必要になったタイミングで取得する
    //
    // 注意：@ManyToManyのデフォルトのfetchはLAZYだが、
    //       ここでは明示的にEAGERを指定している
    @ManyToMany(fetch = FetchType.EAGER)
    // @JoinTable は、多対多（Many-to-Many）の関連で使用する中間テーブルを定義するアノテーション
    @JoinTable(
        // DB上に「user_group」というテーブルが作成される
        name = "user_group",

        // 中間テーブルにおける「SocialUser側」の外部キーを指定する
        // user_group.user_id → SocialUser.id
        // joinColumns は、このエンティティ（SocialUser）のIDを指す
        joinColumns = @JoinColumn(name = "user_id"),

        // 中間テーブルにおける「SocialGroup側」の外部キーを指定する
        // user_group.group_id → SocialGroup.id
        // inverseJoinColumns は、相手側のエンティティ（SocialGroup）のIDを指す
        inverseJoinColumns = @JoinColumn(name = "group_id")
    )
    private Set<SocialGroup> groups = new HashSet<>();

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    // SocialUserとSocialProfileの関連付けを設定するためのセッターメソッド
    //
    // 引数 socialProfile には、このSocialUserに関連付ける
    // SocialProfileオブジェクトを指定する
    public void setSocialProfile(SocialProfile socialProfile) {
        
        // SocialProfile側のuserフィールドに、このSocialUser自身を設定する
        //
        // 「this」は現在のSocialUserオブジェクト自身を意味する
        //
        // これによって、
        //
        // SocialProfile.user → SocialUser
        //
        // という関連付けが設定される
        socialProfile.setUser(this);

         // SocialUser側のsocialProfileフィールドに、
        // SocialProfileオブジェクトを設定する
        //
        // これによって、
        //
        // SocialUser.socialProfile → SocialProfile
        //
        // という関連付けが設定される
        this.socialProfile = socialProfile;
    }
}
