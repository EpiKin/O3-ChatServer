package com.essi.chatserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import org.json.JSONException;
import org.json.JSONObject;

public class RegistrationHandler implements HttpHandler {

    ChatAuthenticator auth = null;

    RegistrationHandler(ChatAuthenticator authenticator) {
        this.auth = authenticator;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {

        int code = 200;
        String responseBody = "";
        String username = "";
        String password = "";
        String email = "";

        try {
            if (exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                Headers headers = exchange.getRequestHeaders();
                int contentLength = 0;
                String contentType = "";

                if (headers.containsKey("Content-Length")) {
                    contentLength = Integer.parseInt(headers.get("Content-Length").get(0));
                } else {
                    code = 411;
                    responseBody = "Content-Length error";
                }
                if (headers.containsKey("Content-Type")) {
                    contentType = headers.get("Content-Type").get(0);
                } else {
                    code = 400;
                    responseBody = "No content type in request";
                }
                if (contentType.equalsIgnoreCase("application/json")) {
                    InputStream input = exchange.getRequestBody();
                    String text = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8)).lines()
                            .collect(Collectors.joining("\n"));
                    input.close();
                    try {
                        JSONObject registrationMsg = new JSONObject(text);
                        username = registrationMsg.getString("username");
                        password = registrationMsg.getString("password");
                        email = registrationMsg.getString("email");
                        
                        if (!username.isBlank() || !password.isBlank() || !email.isBlank()) {
                            User user = new User(username, password, email);
                            if (auth.addUser(username, user)) {
                                responseBody = "Account created";
                                exchange.sendResponseHeaders(code, -1);
                            } else {
                                code = 400;
                                responseBody = "User already exists";
                            }
                        } else {
                            code = 400;
                            responseBody = "Invalid user credentials";
                        }
                    } catch (JSONException e) {
                        code = 400;
                        responseBody = "error in JSON, error in file or file does not exist";
                    }
                } else {
                    code = 411;
                    responseBody = "Content-Type must be application/json";
                }
            } else {
                code = 400;
                responseBody = "Not supported";
            }
        } catch (IOException e) {
            code = 500;
            responseBody = "Error in handling the request: " + e.getMessage();

        } catch (Exception e) {
            code = 500;
            responseBody = "Server error in RegistrationHandler: " + e.getMessage();
        }
        if (code < 400) {
            ChatServer.log("*** Error in /chat: " + code + " " + responseBody);
            byte[] bytes = responseBody.getBytes("UTF-8");
            exchange.sendResponseHeaders(code, bytes.length);
            OutputStream stream = exchange.getResponseBody();

            stream.write(bytes);
            stream.close();
        }
    }
}


