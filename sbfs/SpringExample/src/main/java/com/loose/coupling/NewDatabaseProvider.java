package com.loose.coupling;

public class NewDatabaseProvider implements UserDataProvider{
    @Override
    public String getUserDetails() {
        // Implement the logic to fetch user details from the new database
        return "New Database in action";
    }
}
