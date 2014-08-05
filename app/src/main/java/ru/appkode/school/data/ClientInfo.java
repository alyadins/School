package ru.appkode.school.data;

import java.util.Random;

import ru.appkode.school.network.Connection;

/**
 * Created by lexer on 01.08.14.
 */
public class ClientInfo {
    public String clientId;
    public String name;
    public String lastName;
    public String group;

    public boolean isBlockedByOther = false;
    public boolean isBlocked;
    public boolean isChosen = false;

    public Connection connection;

    public ClientInfo(){};


    public ClientInfo(String name, String lastName, String group) {
        this.name = name;
        this.lastName = lastName;
        this.group = group;
    }
}
