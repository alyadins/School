package ru.appkode.school.util;


import org.json.JSONException;
import org.json.JSONObject;

import ru.appkode.school.data.ParcelableClientInfo;
import ru.appkode.school.data.ParcelableServerInfo;
import ru.appkode.school.network.Server;

/**
 * Created by lexer on 10.08.14.
 */
public class JSONHelper {

    /*
        Creators server
     */
    public static String createConnectionMessage(int code, String message, String serverId) throws JSONException {
        JSONObject json = new JSONObject();
        json.put("code", code);
        json.put("message", message);
        json.put("server_id", serverId);
        return json.toString();
    }

    public static String createServerInfo(ParcelableServerInfo info) throws JSONException {
        JSONObject json = new JSONObject();
        json.put("code", Server.INFO_CODE);
        json.put("name", info.name);
        json.put("second_name", info.secondName);
        json.put("last_name", info.lastName);
        json.put("subject", info.subject);
        json.put("server_id", info.serverId);
        return json.toString();
    }

    public static String createSimpleAnswer(int code, String message) throws JSONException {
        JSONObject json = new JSONObject();
        json.put("code", code);
        json.put("message", message);
        return json.toString();
    }

    public static String createServerDisconnect(String serverId) throws JSONException {
        JSONObject json = new JSONObject();
        json.put("server_id", serverId);
        json.put("code", Server.DISCONNECTED);
        return json.toString();
    }

    /*
        Creators client
     */
    public static String createClientConnect(ParcelableClientInfo info) throws JSONException {
        JSONObject json = new JSONObject();
        json.put("method", Server.CONNECT);
        json.put("name", info.name);
        json.put("last_name", info.lastName);
        json.put("group", info.group);
        json.put("id", info.clientId);
        json.put("block", info.isBlocked);
        json.put("block_by", info.blockedBy);
        return json.toString();
    }

    public static String createClientDisconnect(ParcelableClientInfo info) throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("id", info.clientId);
        jsonObject.put("method", Server.DISCONNECT);
        return jsonObject.toString();
    }

    public static String createInfoRequest() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("method", Server.INFO);
        return json.toString();
    }

    /*
        Parsers server
     */
    public static ParcelableClientInfo parseUserInfo(String message, String serverId) throws JSONException {
        ParcelableClientInfo info = new ParcelableClientInfo();

        JSONObject json = new JSONObject(message);
        info.name = json.getString("name");
        info.lastName = json.getString("last_name");
        info.group = json.getString("group");
        info.clientId = json.getString("id");
        info.isBlocked = json.getBoolean("block");
        info.blockedBy = json.getString("block_by");
        if (info.isBlocked && !info.blockedBy.equals(serverId)) {
            info.isBlockedByOther = true;
        } else {
            info.isBlockedByOther = false;
        }

        return info;
    }

    public static String parseClientId(String message) throws JSONException {
        JSONObject json = new JSONObject(message);
        return json.getString("id");
    }

    public static int parseMethod(String message) throws JSONException {
        JSONObject json = new JSONObject(message);
        return json.getInt("method");
    }


    /*
        Parsers client
     */
    public static int parseCode(String message) throws JSONException {
        JSONObject json = new JSONObject(message);
        return json.getInt("code");
    }

    public static ParcelableServerInfo parseServerInfo(String message) throws JSONException {
        JSONObject object = new JSONObject(message);
        ParcelableServerInfo info = new ParcelableServerInfo();
        info.name = object.getString("name");
        info.secondName = object.getString("second_name");
        info.lastName = object.getString("last_name");
        info.subject = object.getString("subject");
        info.serverId = object.getString("server_id");
        return info;
    }

    public static String parseServerId(String message) throws JSONException {
        JSONObject json = new JSONObject(message);
        return json.getString("server_id");
    }
}
