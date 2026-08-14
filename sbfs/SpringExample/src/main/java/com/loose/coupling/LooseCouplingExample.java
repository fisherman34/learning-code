package com.loose.coupling;

import car.example.constructor.injection.Car;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class LooseCouplingExample {
    public static void main(String[] args) {
        ApplicationContext context = new ClassPathXmlApplicationContext("applicationLooseCouplingExample.xml");
        UserManager userManagerWithDB = (UserManager) context.getBean("userManagerWithNewDB");
        String userInfo = userManagerWithDB.getUserInfo();
        System.out.println(userInfo);

        UserManager userManagerWithWS = (UserManager) context.getBean("userManagerWithWS");
        String userInfoWS = userManagerWithWS.getUserInfo();
        System.out.println(userInfoWS);

        UserManager userManagerWithNewDB = (UserManager) context.getBean("userManagerWithNewDB");
        String userInfoNewDB = userManagerWithNewDB.getUserInfo();
        System.out.println(userInfoNewDB);
    }
}
