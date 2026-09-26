package com.ecommerce.project.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Data
@Table(name = "roles")
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "role_id")
    private Integer roleId;

    /*
    @Enumerated →　Javaのenumをデータベースに保存するときの方法を指定する
    EnumType.STRING　→　enumの名前を文字列としてDBに保存する

    EnumType.STRING を使わない場合
    enumの順番（番号）を保存します。
    DBには例えば：
    role_id | role_name
    --------+----------
    1       | 0
    2       | 1
    3       | 2
     */
    @ToString.Exclude
    @Enumerated(EnumType.STRING)
    @Column(length = 20, name = "role_name")
    private AppRole roleName;

    public Role(AppRole roleName) {
        this.roleName = roleName;
    }
}

