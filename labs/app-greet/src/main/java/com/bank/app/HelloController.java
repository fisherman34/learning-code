package com.bank.app;

import org.springframework.web.bind.annotation.*;

@RestController
// @RequestMapping("/hello") は、クラスレベルに付いています。
// そのため、このクラス内の各エンドポイントに /hello が共通して付加されます。
@RequestMapping("/hello")
public class HelloController {
    @GetMapping
    public String sayHello() {
        return "Welcome to Hello Banking API!";
    }

    @PostMapping
    public String greetUser(@RequestBody String name) {
        return "Hello, " + name + "! Welcome to Hello Banking API!";
    }

}
