package ru.appkode.school.network;

/**
 * Created by lexer on 20.08.14.
 */
public interface ConnectionParams {

    public static final String METHOD = "method";
    public static final String PARAMS = "params";
    public static final String KEY = "key";
    public static final String VALUE = "value";
    //methods
    public static final String CONNECT = "connect";
    public static final String CONNECT_CALLBACK = "connect_callback";
    public static final String DISCONNECT = "disconnect";
    public static final String DISCONNECT_CALLBACK = "disconnect_callback";
    public static final String DISCONNECT_FROM = "disconnect_from";
    public static final String INFO = "info";
    public static final String INFO_CALLBACK = "info_callback";
    public static final String BLOCK = "block";
    public static final String UNBLOCK = "unblock";

    //params
    public static final String ID = "id";
    public static final String NAME = "name";
    public static final String SECOND_NAME = "second_name";
    public static final String LAST_NAME = "last_name";
    public static final String SUBJECT = "subject";
    public static final String GROUP = "group";
    public static final String BLOCK_BY = "block_by";
    public static final String ADDRESS = "address";
    public static final String PORT = "port";
    public static final String WHITE_LIST = "white_list";
    public static final String BLACK_LIST = "black_list";
    public static final String FROM = "from";


    //End of connection
    public static final String END = "END";
}
