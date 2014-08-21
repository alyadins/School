package ru.appkode.school.util;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import ru.appkode.school.data.ParcelableClientInfo;
import ru.appkode.school.data.ParcelableServerInfo;
import ru.appkode.school.network.ConnectionParams;
import ru.appkode.school.network.ServerConnection;

import static java.util.Map.*;

/**
 * Created by lexer on 10.08.14.
 */
public class JSONHelper implements ConnectionParams {

    /*
        Creators server
     */
//    public static String createConnectionMessage(int code, String message, String serverId) throws JSONException {
//        JSONObject json = new JSONObject();
//        json.put("code", code);
//        json.put("message", message);
//        json.put("server_id", serverId);
//        return json.toString();
//    }
//
//    public static String createServerInfo(ParcelableServerInfo info) throws JSONException {
//        JSONObject json = new JSONObject();
//        json.put("code", ServerConnection.INFO_CODE);
//        json.put("name", info.name);
//        json.put("second_name", info.secondName);
//        json.put("last_name", info.lastName);
//        json.put("subject", info.subject);
//        json.put("server_id", info.id);
//        return json.toString();
//    }
//
//    public static String createSimpleAnswer(int code, String message) throws JSONException {
//        JSONObject json = new JSONObject();
//        json.put("code", code);
//        json.put("message", message);
//        return json.toString();
//    }
//
//    public static String createServerDisconnect(String serverId) throws JSONException {
//        JSONObject json = new JSONObject();
//        json.put("server_id", serverId);
//        json.put("code", ServerConnection.DISCONNECT);
//        return json.toString();
//    }

    public static String createAppListJson(List<String> whiteList) {
        JSONArray array = new JSONArray(whiteList);
        return array.toString();
    }

    public static String createBlockJson(String serverId, String port, List<String> whiteList, List<String> blackList) throws JSONException {
        JSONObject json = new JSONObject();
        json.put(METHOD, BLOCK);
        json.put(ID, serverId);
        json.put(PORT, port);
        JSONArray whiteListJson = new JSONArray(whiteList);
        json.put(WHITE_LIST, whiteListJson);
        JSONArray blackListJson = new JSONArray(blackList);
        json.put(BLACK_LIST, blackListJson);

        return json.toString();
    }

    public static HashMap<String, String> parseBlockJson(String message, ArrayList<String> whiteList, ArrayList<String> blackList) throws JSONException {
        HashMap<String, String> params = new HashMap<String, String>();
        JSONObject json = new JSONObject(message);
        params.put(ID, json.getString(ID));
        params.put(PORT, json.getString(PORT));

        whiteList.clear();
        blackList.clear();

        whiteList.addAll(parseList(message, WHITE_LIST));
        blackList.addAll(parseList(message, BLACK_LIST));

        return params;
    }

    public static String createMessage(String method, HashMap<String, String> params) throws JSONException {
        JSONObject json = new JSONObject();
        json.put(METHOD, method);

        if (params != null) {
            JSONArray array = new JSONArray();
            for (Entry<String, String> entry : params.entrySet()) {
                JSONObject jsonParam = new JSONObject();
                String key = entry.getKey();
                String value = entry.getValue();
                jsonParam.put(KEY, key);
                jsonParam.put(VALUE, value);
                array.put(jsonParam);
            }
            json.put(PARAMS, array);
        }

        return json.toString();
    }

    public static String parseMethod(String message) throws JSONException {
        JSONObject json = new JSONObject(message);
        return json.getString(METHOD);
    }

    public static HashMap<String, String> parseParams(String message) throws JSONException {
        HashMap<String, String> params = new HashMap<String, String>();
        JSONObject json = new JSONObject(message);
        JSONArray array = json.getJSONArray(PARAMS);
        for (int i = 0; i < array.length(); i++) {
            JSONObject object = array.getJSONObject(i);
            String key = object.getString(KEY);
            String value = object.getString(VALUE);
            params.put(key, value);
        }

        return params;
    }

    /*
        to here
     */
//
//    public static String createClientConnect(ParcelableClientInfo info) throws JSONException {
//        JSONObject json = new JSONObject();
//        json.put("method", ServerConnection.CONNECT);
//        json.put("name", info.name);
//        json.put("last_name", info.lastName);
//        json.put("group", info.group);
//        json.put("id", info.id);
//        json.put("block", info.isBlocked);
//        json.put("block_by", info.blockedBy);
//        return json.toString();
//    }
//
//    public static String createClientDisconnect(ParcelableClientInfo info) throws JSONException {
//        JSONObject jsonObject = new JSONObject();
//        jsonObject.put("id", info.id);
//        jsonObject.put("method", ServerConnection.DISCONNECT);
//        return jsonObject.toString();
//    }
//
//    public static String createInfoRequest() throws JSONException {
//        JSONObject json = new JSONObject();
//        json.put("method", ServerConnection.INFO);
//        return json.toString();
//    }
//
    public static String createFavouriteJson(List<String> infos) throws JSONException {
        JSONArray array = new JSONArray(infos);
        return array.toString();
    }
//
//    /*
//        Parsers server
//     */
//    public static ParcelableClientInfo parseUserInfo(String message, String serverId) throws JSONException {
//        ParcelableClientInfo info = new ParcelableClientInfo();
//
//        JSONObject json = new JSONObject(message);
//        info.name = json.getString("name");
//        info.lastName = json.getString("last_name");
//        info.group = json.getString("group");
//        info.id = json.getString("id");
//        info.isBlocked = json.getBoolean("block");
//        info.blockedBy = json.getString("block_by");
//        if (info.isBlocked && !info.blockedBy.equals(serverId)) {
//            info.isBlockedByOther = true;
//        } else {
//            info.isBlockedByOther = false;
//        }
//
//        return info;
//    }
//
//    public static String parseClientId(String message) throws JSONException {
//        JSONObject json = new JSONObject(message);
//        return json.getString("id");
//    }
//
//
//
//    /*
//        Parsers client
//     */
//    public static int parseCode(String message) throws JSONException {
//        JSONObject json = new JSONObject(message);
//        return json.getInt("code");
//    }
//
//    public static ParcelableServerInfo parseServerInfo(String message) throws JSONException {
//        JSONObject object = new JSONObject(message);
//        ParcelableServerInfo info = new ParcelableServerInfo();
//        info.name = object.getString("name");
//        info.secondName = object.getString("second_name");
//        info.lastName = object.getString("last_name");
//        info.subject = object.getString("subject");
//        info.id = object.getString("server_id");
//        return info;
//    }
//
//    public static String parseServerId(String message) throws JSONException {
//        JSONObject json = new JSONObject(message);
//        return json.getString("server_id");
//    }
//
    public static ArrayList<String> parseList(String message, String name) throws JSONException {
        JSONObject json = new JSONObject(message);
        JSONArray array = json.getJSONArray(name);
        return parseStringList(array);
    }

    public static ArrayList<String> parseAppList(String result) throws JSONException {
        JSONArray array = new JSONArray(result);
        return parseStringList(array);
    }

    private static ArrayList<String> parseStringList(JSONArray array) throws JSONException {
        ArrayList<String> list = new ArrayList<String>();
        for (int i = 0; i < array.length(); i++) {
            list.add(array.getString(i));
        }

        return list;
    }

   public static ArrayList<String> parseFavouriteList(String s) throws JSONException {
       JSONArray array = new JSONArray(s);
       return parseStringList(array);
   }
}
