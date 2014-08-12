package ru.appkode.school.util;

/**
 * Created by lexer on 12.08.14.
 */
public class ServiceType {

    public static final int SERVER  = 0;
    public static final int CLIENT = 1;
    public  static final int UNKNOWN = 2;

    public static int getTypeOfService(String id) {
        if (id.length() > 7) {
            if (id.substring(0, 4).equals("serv")) {
                return SERVER;
            }
            if (id.substring(0, 6).equals("client")) {
                return CLIENT;
            }
        }
        return UNKNOWN;
    }
}
