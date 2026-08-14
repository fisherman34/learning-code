package com.example.componentscan;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class App {
    public static void main(String[] args) {
        ApplicationContext context = new ClassPathXmlApplicationContext(
                "componentScanDemo.xml"
        );

        // Spring Container（ApplicationContext）から、
        // Bean名が "employee" のBeanを取得する。
        //
        // getBean("employee", Employee.class) の意味：
        //
        //   "employee"
        //       → Spring Containerに登録されているBean名
        //
        //   Employee.class
        //       → 取得したいBeanの型
        //
        // Springは "employee" という名前のBeanを探し、
        // Employee型のオブジェクトを返す。
        //
        // その結果、employee変数には
        // Springが生成・管理しているEmployee Beanへの参照が格納される。
        Employee employee = context.getBean("employee", Employee.class);
        System.out.println(employee.toString());
    }
}
