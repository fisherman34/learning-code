package com.example.autowired.annotation;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class Manager {

    // Springに、このフィールドへ依存するBeanを自動注入させる。
    //
    // @Autowiredは基本的に「型（Type）」を基準にして
    // 注入するBeanを探す。
    //
    // この場合、
    //
    //     Employee employee;
    //              ↓
    //     Employee型のBeanを探す
    //
    // という動作になる。
    @Autowired

    // Employee型のBeanが複数存在する場合に、
    // どのBeanを注入するかをBean名で指定する。
    //
    // "employee" はSpring Containerに登録されている
    // Beanの名前。
    //
    // 例えばEmployeeクラスが、
    //
    //     @Component("employee")
    //
    // となっていれば、そのBeanを注入する。
    //
    // つまり、
    //
    //     @Autowired
    //         ↓
    //     Employee型のBeanを探す
    //         ↓
    //     @Qualifier("employee")
    //         ↓
    //     Bean名が "employee" のものを選択
    @Qualifier("employee")
    private Employee employee;

//    @Autowired
//    public Manager(Employee employee) {
//        this.employee = employee;
//    }

    @Override
    public String toString() {
        return "Manager{" +
                "employee=" + employee +
                '}';
    }
}
