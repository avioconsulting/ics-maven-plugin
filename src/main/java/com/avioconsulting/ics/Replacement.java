package com.avioconsulting.ics;

public class Replacement {
    private String token;
    private String value;


    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String toString(){
        return getToken() + "|" + getValue();
    }
}
