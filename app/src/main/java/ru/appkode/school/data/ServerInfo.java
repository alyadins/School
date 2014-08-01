package ru.appkode.school.data;

import ru.appkode.school.util.StringUtil;

/**
 * Created by lexer on 01.08.14.
 */
public class ServerInfo {
    public String name;
    public String subject;
    public boolean isFavourite;
    public boolean isConnected;

    public ServerInfo(String name, String subject, boolean isFavourite, boolean isConnected) {
        this.name = name;
        this.subject = subject;
        this.isFavourite = isFavourite;
        this.isConnected = isConnected;
    }
}
