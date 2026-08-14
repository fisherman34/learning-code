package com.example.autowired.annotation;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

// このクラスをSpringの「設定クラス（Configuration Class）」として登録する。
//
// Springはこのクラスをアプリケーションの設定情報として扱う。
@Configuration

// Component Scanを実行するパッケージを指定する。
//
// 指定したパッケージ
//
//     com.example.componentscan.annotation
//
// と、そのサブパッケージをSpringがスキャンし、
//
//     @Component
//     @Service
//     @Repository
//     @Controller
//
// などが付いているクラスを自動的に検出して、
// Spring ContainerのBeanとして登録する。
//
// XMLで書く場合の、
//
//     <context:component-scan
//         base-package="com.example.componentscan.annotation"/>
//
// に相当する。
@ComponentScan(basePackages = "com.example.autowired.annotation")
public class AppConfig {
}
