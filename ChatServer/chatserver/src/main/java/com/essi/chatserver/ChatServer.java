package com.essi.chatserver;

import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;
import com.sun.net.httpserver.HttpsParameters;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.time.LocalDateTime;
import java.io.Console;
import java.util.concurrent.Executors;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManagerFactory;

public class ChatServer {

    public static void main(String[] args) throws Exception {

        try {
            log("Launching chatserver..");
            log("Initializing database..");

            if (args.length != 3) {
                log("Usage java -jar jar-file.jar dbname.db cert.jks c3rt-p4ssw0rd");
                return;
            }

            boolean running = true;
            ChatDatabase database = ChatDatabase.getInstance();
            database.open(args[0]); // first startup param is the db file name.

            HttpsServer server = HttpsServer.create(new InetSocketAddress(8001), 0);
            SSLContext sslContext = chatServerSSLContext();
            server.setHttpsConfigurator(new HttpsConfigurator(sslContext) {
                @Override
                public void configure(HttpsParameters params) {
                    InetSocketAddress remote = params.getClientAddress();
                    SSLContext c = getSSLContext();
                    SSLParameters sslparams = c.getDefaultSSLParameters();
                    params.setSSLParameters(sslparams);
                }
            });

            ChatAuthenticator auth = new ChatAuthenticator();
            HttpContext chatContext = server.createContext("/chat", new ChatHandler());
            chatContext.setAuthenticator(auth);
            server.createContext("/registration", new RegistrationHandler(auth));
            server.setExecutor(Executors.newCachedThreadPool());
            
            log("Starting chatserver!");
            server.start();

            Console console = System.console();

            while (running == true) {
                String quitStr = console.readLine();
                System.out.println(quitStr);

                if (quitStr.equals("/quit")) {
                    running = false;
                    server.stop(3);
                    System.out.println("Server stopped");
                    database.close();
                    System.out.println("Database closed");
                }
            }

        } catch (Exception e) {
            System.out.println("Exception");
            e.printStackTrace();
        }
    }

    public static void log(String message) {
        System.out.println(LocalDateTime.now() + " " + message);
    }

    private static SSLContext chatServerSSLContext()
            throws KeyStoreException, NoSuchAlgorithmException, CertificateException, FileNotFoundException,
            IOException, UnrecoverableKeyException, KeyManagementException {

        try {
            char[] passphrase = "salasana".toCharArray();
            KeyStore ks = KeyStore.getInstance("JKS");
            ks.load(new FileInputStream("keystore.jks"), passphrase);

            KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
            kmf.init(ks, passphrase);

            TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
            tmf.init(ks);

            SSLContext ssl = SSLContext.getInstance("TLSv1.2");
            ssl.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
            return ssl;

        } catch (FileNotFoundException e) {
            // Certificate file not found!
            System.out.println("Certificate not found!");
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;

    }

}
