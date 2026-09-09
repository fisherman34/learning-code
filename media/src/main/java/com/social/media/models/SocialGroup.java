package com.social.media.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SocialGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // @ManyToMany は、SocialUserエンティティとGroupエンティティが
    // 多対多（Many-to-Many）の関係であることを指定するアノテーション
    //
    // mappedBy = "groups" は、
    // 「このリレーションの所有側はSocialUser側にあり、
    //  SocialUserクラスのgroupsフィールドによって
    //  この関連付けが管理されている」ことを指定する
    //
    // "groups" はDBのテーブル名やカラム名ではなく、
    // SocialUserクラスに定義されているフィールド名を指定する
    @ManyToMany(mappedBy = "groups")
    private Set<SocialUser> socialUsers = new HashSet<>();

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
