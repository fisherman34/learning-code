package com.embarkx.fisrtspring;

import org.springframework.web.bind.annotation.*;

// @RestController:
// このクラスをSpring MVCのRESTコントローラーとして登録する。
// SpringがこのクラスのインスタンスをBeanとして管理し、
// HTTPリクエストを処理できるようにする。
// また、メソッドの戻り値を基本的にHTTPレスポンスの本文として返す。
// @RestController は実質的に @Controller + @ResponseBody の役割を持っている
// これによって return "Hello World!" がHTMLのビュー名ではなく、HTTPレスポンス本文そのものとして返される
@RestController
public class HelloController {

    // @GetMapping("/hello/{name}"):
    // HTTP GETリクエストのURL「/hello/{name}」とこのメソッドを関連づける。
    //
    // {name} は「パス変数（Path Variable）」を表している。
    // URLの {name} の部分には、実際の値が入る。
    //
    // 例えば、
    //
    // http://localhost:8080/hello/Sam
    //
    // にアクセスした場合、
    // URLの「{name}」の部分に「Sam」が入る。
    //
    // その値は、@PathVariable String name によって
    // JavaのString型の変数「name」に渡される。
    //
    // つまり、
    //
    // /hello/{name}
    //        ↑
    //        URLに実際の値が入る
    //
    // /hello/Sam
    //        ↓
    // name = "Sam"
    //
    @GetMapping("/hello/{name}")

    // @PathVariable String name:
    // URLのパスに含まれている「{name}」の値を取得し、
    // JavaのString型の変数「name」に格納する。
    //
    // @PathVariableの「name」と、
    // @GetMapping("/hello/{name}")の「{name}」が対応している。
    public  HelloResponse hello(@PathVariable String name) {
        return new HelloResponse("Hello, " + name + "!");
    }

    // @GetMapping("/hello"):
    // HTTP GETリクエストのURL「/hello」とこのメソッドを関連づける。
    // 例えば、ブラウザから http://localhost:8080/hello にアクセスすると、
    // このhello()メソッドが実行される。
    @GetMapping("/hello")
    public  HelloResponse hello() {
        return new HelloResponse("Hello World!");
    }

    // @PostMapping("/hello"):
    // HTTP POSTリクエストのURL「/hello」とこのメソッドを関連づける。
    // GETではなくPOSTで /hello にリクエストを送信すると、
    // このhelloPost()メソッドが実行される。
    //
    // 例えば、Postmanなどから以下のようなHTTPリクエストを送信する：
    //
    // POST http://localhost:8080/hello
    // Content-Type: text/plain
    //
    // Sam
    //
    // この場合、リクエストの本文（Request Body）である「Sam」が
    // helloPost()メソッドのname引数に渡される。
    @PostMapping("/hello")
    // @RequestBody String name:
    // HTTPリクエストの「ボディ（Request Body）」に含まれているデータを
    // JavaのString型として受け取り、name変数に格納する。
    //
    // 例えば、HTTPリクエストのボディが：
    //
    // Sam
    //
    // だった場合、
    //
    // String name = "Sam";
    //
    // のようなイメージでnameに値が入る。
    //
    // @RequestBodyは、URLの一部やURLパラメータではなく、
    // HTTPリクエストの本文からデータを取得するためのアノテーション。
    public  HelloResponse helloPost(@RequestBody String name) {
        return new HelloResponse("Hello, " + name + "!");
    }
}
