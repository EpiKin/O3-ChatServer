package com.essi.chatserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.stream.Collectors;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class ChatHandler implements HttpHandler {

    private String responseBody = "";

    private ArrayList<ChatMessage> messages = new ArrayList<ChatMessage>();

    @Override
    public void handle(HttpExchange exchange) throws IOException {

        System.out.println("Request handled in thread " + Thread.currentThread().getId());

        int code = 200;

        try {
            if (exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                code = handleChatMessageFromClient(exchange);
            } else if (exchange.getRequestMethod().equalsIgnoreCase("GET")) {
                code = handleGetRequestFromClient(exchange);
            } else {
                code = 400;
                responseBody = "Not supported";
            }
        } catch (IOException e) {
            code = 500;
            responseBody = "Server error in ChatHandler: " + e.getMessage();
        } catch (Exception e) {
            code = 500;
            responseBody = "Server error: " + e.getMessage();
        }
        if (code >= 400) {
            ChatServer.log("*** Error in /chat: " + code + " " + responseBody);
            byte[] bytes = responseBody.getBytes("UTF-8");
            exchange.sendResponseHeaders(code, bytes.length);
            OutputStream stream = exchange.getResponseBody();
            stream.write(bytes);
            stream.close();
        }

    }

    private int handleChatMessageFromClient(HttpExchange exchange) throws Exception {

        String message = "";
        String nick = "";
        String sent = "";
        int contentLength = 0;
        String contentType = "";
        int code = 200;
        Headers headers = exchange.getRequestHeaders();

        if (headers.containsKey("Content-Length")) {
            contentLength = Integer.parseInt(headers.get("Content-Length").get(0));
        } else {
            code = 411;
            return code;
        }
        if (headers.containsKey("Content-Type")) {
            contentType = headers.get("Content-Type").get(0);
        } else {
            code = 400;
            responseBody = "No content type";
            return code;
        }
        if (contentType.equalsIgnoreCase("application/json")) {

            InputStream input = exchange.getRequestBody();
            String text = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8)).lines()
                    .collect(Collectors.joining("\n"));
            input.close();

            try {
                JSONObject jsonObject = new JSONObject(text);
                ;
                nick = jsonObject.getString("user");
                message = jsonObject.getString("message");
                String dateStr = jsonObject.getString("sent");
                OffsetDateTime odt = OffsetDateTime.parse(dateStr);

                System.out.println("Trying to POST message");

                if (!nick.isBlank() || !message.isBlank() || !sent.isBlank()) {
                    ChatDatabase cdb = ChatDatabase.getInstance();
                    cdb.addMessageToDatabase(new ChatMessage(nick, message, odt.toLocalDateTime()));
                    exchange.sendResponseHeaders(code, -1);
                    System.out.println("POST succeeded");

                } else {
                    code = 400;
                    responseBody = "Error in nick, message or sent";
                }
            } catch (JSONException e) {
                code = 400;
                responseBody = "JSON error";
            }
        } else {
            code = 411;
            responseBody = "Content-Type must be application/json";
        }
        return code;
    }

    private int handleGetRequestFromClient(HttpExchange exchange) throws Exception {

        int code = 200;
        String ifModiSince = "";

        JSONArray responseMessages = new JSONArray();
        ArrayList<ChatMessage> messages = new ArrayList<ChatMessage>();
        ChatDatabase cdb = ChatDatabase.getInstance();
        Headers headers = exchange.getResponseHeaders();
        Headers requestHeaders = exchange.getRequestHeaders();

        if (requestHeaders.containsKey("If-Modified-Since")) {
            
            ifModiSince = requestHeaders.get("If-Modified-Since").get(0);
            ZonedDateTime zonedDate = ZonedDateTime.parse(ifModiSince);
            LocalDateTime fromWhichDate = zonedDate.toLocalDateTime();
            long messagesSince = -1;

            messagesSince = fromWhichDate.toInstant(ZoneOffset.UTC).toEpochMilli();
            messages = cdb.getMessagesSince(messagesSince);

        } else {
            System.out.println("100 Messages");
            messages = cdb.getMessages();
        }

        if (messages.isEmpty()) {
            code = 204;
            exchange.sendResponseHeaders(code, -1);
            return code;

        } else {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX");
            ZonedDateTime dateMax = null;

            for (ChatMessage message : messages) {

                System.out.println(message.getSent() + " " + message.getNick() + " " + message.getMessage());

                if (dateMax == null) {
                    dateMax = message.sent.atZone(ZoneId.of("UTC"));
                }

                ZonedDateTime zdt = message.sent.atZone(ZoneId.of("UTC"));
                String sentUTC = zdt.format(formatter);

                dateMax = zdt.isAfter(dateMax) ? zdt : dateMax;

                JSONObject jsonObject = new JSONObject();

                jsonObject.put("user", message.getNick());
                jsonObject.put("message", message.getMessage());
                jsonObject.put("sent", sentUTC);

                responseMessages.put(jsonObject);
            }

            String dateMaxString = dateMax.format(formatter);
            headers = exchange.getResponseHeaders();
            headers.add("Last-Modified", dateMaxString);

            byte[] bytes;
            bytes = responseMessages.toString().getBytes("UTF-8");
            exchange.sendResponseHeaders(code, bytes.length);
            OutputStream os = exchange.getResponseBody();
            os.write(bytes);
            os.close();

            return code;
        }
    }
}