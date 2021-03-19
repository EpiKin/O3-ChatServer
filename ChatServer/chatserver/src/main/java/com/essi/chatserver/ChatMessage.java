package com.essi.chatserver;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

public class ChatMessage {
    LocalDateTime sent;
    private String nick;
    private String message;

   
    public ChatMessage(String nick, String message, LocalDateTime sent){
        this.nick = nick;
        this.message = message;
        this.sent = sent;
    }

    public String getNick(){
        return nick;
    }

    public String getMessage(){
        return message;
    }

    public LocalDateTime getSent(){
        return sent;
    }

    long dateAsInt() {
        return sent.toInstant(ZoneOffset.UTC).toEpochMilli();
    }

    void setSent(long epoch) {
        sent = LocalDateTime.ofInstant(Instant.ofEpochMilli(epoch), ZoneOffset.UTC);
        }
}