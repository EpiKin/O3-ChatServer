package com.essi.chatserver;

import com.sun.net.httpserver.BasicAuthenticator;

import java.sql.SQLException;
import java.util.Hashtable;
import java.util.Map;

public class ChatAuthenticator extends BasicAuthenticator {

    private Map<String, User> users = null;

    public ChatAuthenticator() {
        super("chat");
        users = new Hashtable<String, User>();
    }

    @Override
    public boolean checkCredentials(String username, String password) {
        ChatDatabase chatdatabase = ChatDatabase.getInstance();
        if (chatdatabase.checkUserCredentials(username, password)) {
            return true;
        } else {
            return false;
        }
    }

    public boolean addUser(String username, User user) throws SQLException {

        ChatDatabase chatdatabase = ChatDatabase.getInstance();
        if (chatdatabase.checkAddUser(username, user)) {
            return true;
        } else {
            return false;
        }
    }
}
