package com.example.chat;

import android.util.Log;
import android.util.Pair;

import androidx.core.util.Consumer;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Server {
    WebSocketClient client;
    // MainActivity - потребитель сообщений
    private Consumer<Pair<String, String>> messageConsumer;

    private Map<Long, String> nameMap = new ConcurrentHashMap<>();

    public Server(Consumer<Pair<String, String>> messageConsumer) {
        this.messageConsumer = messageConsumer;
    }

    public void connect(){
        URI addr;
        try {
            addr = new URI("ws://00.210.000.210:8080/"); //запилить сервак же
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return;
        }

        client = new WebSocketClient(addr) {
            @Override
            public void onOpen(ServerHandshake handshakedata) {
                // При подключении
                Log.i("WSSERVER", "Connected to server");
            }

            @Override
            public void onMessage(String json) {
                int type = Protocol.getType(json);
                if (type == Protocol.MESSAGE){
                    onIncomingTextMessage(json);
                }
                if (type == Protocol.USER_STATUS){
                   onStatusUpdate(json);
                }
                Log.i("WSSERVER", "Receiver message:" + json);

            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                Log.i("WSSERVER", "Connection closed");

            }

            @Override
            public void onError(Exception ex) {
                Log.i("WSSERVER", "Error occured:" +ex.getMessage());

            }
        };
        client.connect();
    }
    private void onStatusUpdate(String json) {
        Protocol.UserStatus status = Protocol.unpackStatus(json);
        Protocol.User u = status.getUser();
        if (status.isConnected()){ //Новый пользователь подключился
                        nameMap.put(u.getId(), u.getName()); //положить имя в карту
        }
        else{ //отключился
            nameMap.remove(u.getId());
        }
    }

    private void onIncomingTextMessage(String json){
        Protocol.Message message = Protocol.unpackMessage(json);
        String name = nameMap.get(message.getSender());
        if (name == null) {
            name = "Unnamed";
        }
        messageConsumer.accept(new Pair<>(message.getEncodedText(),name));

    }
    public void sendMessage(String messageText){
        String json = Protocol.packMessage(
                new Protocol.Message(messageText)
        );
        if (client != null && client.isOpen()) {
            client.send(json);
        }
        client.send(json);
    }
    public void sendName(String name) {
        String json = Protocol.packName(
                new Protocol.UserName(name)
        );
        if (client != null && client.isOpen()) {
            client.send(json);
        }
    }
}
