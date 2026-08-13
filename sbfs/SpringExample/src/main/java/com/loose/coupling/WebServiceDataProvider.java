package com.loose.coupling;

public class WebServiceDataProvider implements UserDataProvider{
    @Override
    public String getUserDetails() {
        // Implement the logic to fetch user details from a web service
        return "Fetch Data from Webservice";
    }
}
