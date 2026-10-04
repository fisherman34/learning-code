package com.packt.cardatabase;

import com.packt.cardatabase.domain.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Arrays;

/*
CommandLineRunner は、Spring Bootアプリケーションの起動が完了した直後に、指定した処理を
自動実行するためのインターフェースです。
 */

@SpringBootApplication
public class CardatabaseApplication implements CommandLineRunner {

    /*
    Logger:
    SLF4Jが提供するロギング用のインターフェースです。
    logger.info()、logger.error()などを使ってログを出力できます。

    LoggerFactory.getLogger(...):
    Loggerオブジェクトを生成・取得するためのメソッドです。

    CardatabaseApplication.class:
    CardatabaseApplicationクラスを表すClassオブジェクトを渡しています。
    これにより、このクラス用のLoggerを取得します。
     */
    private static final Logger logger = LoggerFactory.getLogger(
        CardatabaseApplication.class
    );

    private final CarRepository repository;
    private final OwnerRepository orepository;
    private final AppUserRepository urepository;

    public CardatabaseApplication(CarRepository repository,
                                  OwnerRepository orepository, AppUserRepository urepository) {
        this.repository = repository;
        this.orepository = orepository;
        this.urepository = urepository;
    }

    public static void main(String[] args) {
        SpringApplication.run(CardatabaseApplication.class, args);
    }

    /*
    public void run(String... args) throws Exception {
        // ...
    }
    　→　　CommandLineRunner の run() を使ってアプリケーション起動時に初期データを
          データベースへ登録しています。
     */
    @Override
    public void run(String... args) throws Exception {
        // Add owner objects and save these to db
        Owner owner1 = new Owner("John" , "Johnson");
        Owner owner2 = new Owner("Mary" , "Robinson");
        orepository.saveAll(Arrays.asList(owner1, owner2));

        repository.save(new Car("Ford", "Mustang", "Red",
                "ADF-1121", 2023, 59000, owner1));
        repository.save(new Car("Nissan", "Leaf", "White",
                "SSJ-3002", 2020, 29000, owner2));
        repository.save(new Car("Toyota", "Prius", "Silver",
                "KKO-0212", 2022, 39000, owner2));

        // Fetch all cars and log to console
        for (Car car : repository.findAll()) {
            logger.info("brand: {}, model: {}", car.getBrand(), car.getModel());
        }

        // Username: user, password: user
        urepository.save(new AppUser("user",
                "$2a$10$NVM0n8ElaRgg7zWO1CxUdei7vWoPg91Lz2aYavh9.f9q0e4bRadue",
                "USER"));
        // Username: admin, password: admin
        urepository.save(new AppUser("admin",
                "$2a$10$8cjz47bjbR4Mn8GMg9IZx.vyjhLXR/SKKMSZ9.mP9vpMu0ssKi8GW",
                "ADMIN"));
    }

}
