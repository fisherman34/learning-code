package com.example.componentscan.annotation;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class App {
    public static void main(String[] args) {

        // AppConfig.classをSpringの設定クラスとして読み込み、
        // ApplicationContext（Spring Container）を生成する。
        //
        // AnnotationConfigApplicationContextは、
        // XMLファイルではなくJavaクラスのConfigurationを使って
        // Spring Containerを構築するためのApplicationContext。
        //
        // AppConfigには、例えば:
        //
        //     @Configuration
        //     @ComponentScan(
        //         basePackages = "com.example.componentscan.annotation"
        //     )
        //
        // が付いているため、SpringはAppConfigを読み込んだ後、
        // 指定されたパッケージをComponent Scanする。
        //
        // その結果、@Componentが付いているEmployeeなどのクラスが
        // Spring Beanとして登録される。
        //
        // XMLを使用する場合の
        //
        //     new ClassPathXmlApplicationContext("application.xml")
        //
        // に相当するJava Configuration版。
        ApplicationContext context = new AnnotationConfigApplicationContext(
                AppConfig.class
        );
        Employee employee = context.getBean("employee", Employee.class);
        System.out.println(employee.toString());
    }
}
