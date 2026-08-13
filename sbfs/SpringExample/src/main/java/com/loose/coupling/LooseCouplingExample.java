package com.loose.coupling;

public class LooseCouplingExample {
    public static void main(String[] args) {
        UserDataProvider dataProvider = new UserDatabaseProvider();
        UserManager userManagerWithDB = new UserManager(dataProvider);
        String userInfo = userManagerWithDB.getUserInfo();
        System.out.println(userInfo);

        UserDataProvider webServiceDataProvider = new WebServiceDataProvider();
        UserManager userManagerWithWS = new UserManager(webServiceDataProvider);
        String userInfoWS = userManagerWithWS.getUserInfo();
        System.out.println(userInfoWS);

        UserDataProvider newDatabaseProvider = new NewDatabaseProvider();
        UserManager userManagerWithNewDB = new UserManager(newDatabaseProvider);
        String userInfoNewDB = userManagerWithNewDB.getUserInfo();
        System.out.println(userInfoNewDB);
    }
}
