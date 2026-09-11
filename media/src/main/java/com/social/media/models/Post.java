package com.social.media.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
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

    // @JsonIgnore は、このフィールドをJSONに変換するときに
    // 無視（JSONに含めない）することを指定するアノテーション
    //
    // ここでは、PostからSocialUserへの参照をJSONに含めないようにしている
    //
    // 例えば、PostをJSONに変換した場合、通常なら
    //
    // {
    //     "id": 1,
    //     "socialUser": {
    //         "id": 10,
    //         ...
    //     }
    // }
    //
    // のようにsocialUserもJSONに含まれる可能性がある
    //
    // @JsonIgnoreを付けることで、socialUserはJSONレスポンスから除外される
    //
    // これは、SocialUser → posts → SocialUser → posts ... のような
    // 循環参照（無限ループ）がJSON変換時に発生することを防ぐためにも使用される
    @JsonIgnore
    private SocialUser socialUser;
}
