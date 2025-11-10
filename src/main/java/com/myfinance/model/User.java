package com.myfinance.model;

import java.io.Serializable;

public class User implements Serializable {
    private final String login;
    private final String password;

    public User(final String login, final String password) {
        this.login = login;
        this.password = password;
    }

    public String getLogin() {
        return login;
    }

    public String getPassword() {
        return password;
    }
}
