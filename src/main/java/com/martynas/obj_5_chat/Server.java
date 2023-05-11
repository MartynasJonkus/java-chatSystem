package com.martynas.obj_5_chat;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class Server {

    private final ServerSocket serverSocket;
    static DataManager dataManager = DataManager.getInstance();

    public Server(ServerSocket serverSocket) {
        this.serverSocket = serverSocket;
    }

    public void startServer(){
        try{
            while(!serverSocket.isClosed()){
                Socket socket = serverSocket.accept();
                System.out.println("A new client has connected.");
                ClientHandler clientHandler = new ClientHandler(socket, dataManager);

                Thread thread = new Thread(clientHandler);
                thread.start();
            }
        }catch(IOException e){
            closeServerSocket();
        }
    }

    public void closeServerSocket(){
        try{
            if(serverSocket != null)
                serverSocket.close();
        }catch(IOException e){
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException{
        dataManager.importData("new_chats.csv");
        ArrayList<String> users = dataManager.getUsers();
        ArrayList<String> rooms = dataManager.getRooms();
        System.out.println(users);
        System.out.println(rooms);

        ServerSocket serverSocket = new ServerSocket(8008);
        Server server = new Server(serverSocket);
        server.startServer();
    }
}
