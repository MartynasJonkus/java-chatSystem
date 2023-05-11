package com.martynas.obj_5_chat;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;

import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;

public class ClientController implements Initializable {
    @FXML
    private Label nameLabel;
    @FXML
    private Label receiverLabel;
    @FXML
    private TextField roomNameField;
    @FXML
    private TextField messageField;
    @FXML
    private ScrollPane scrollPaneGroups;
    @FXML
    private ScrollPane scrollPaneMain;
    @FXML
    private ScrollPane scrollPaneUsers;
    @FXML
    private VBox vboxGroups;
    @FXML
    private VBox vboxMessages;
    @FXML
    private VBox vboxUsers;

    private Socket socket;
    private String username;
    private ObjectOutputStream output;
    private ObjectInputStream input;

    private DataManager dataManager;
    private String currentRoom;
    private String currentRoomType;
    private boolean socketIsOpen;

    public ClientController(Socket socket, String username){
        try{
            this.socket = socket;
            this.socketIsOpen = true;
            this.username = username;
            this.output = new ObjectOutputStream(socket.getOutputStream());
            this.input = new ObjectInputStream(socket.getInputStream());
        }catch(IOException e){
            closeEverything(socket, output, input);
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        nameLabel.setText(username);

        //Set vBoxes to scroll to the bottom automatically
        vboxMessages.heightProperty().addListener((observable, oldValue, newValue) -> scrollPaneMain.setVvalue((Double) newValue));
        vboxGroups.heightProperty().addListener((observable, oldValue, newValue) -> scrollPaneGroups.setVvalue((Double) newValue));
        vboxUsers.heightProperty().addListener((observable, oldValue, newValue) -> scrollPaneUsers.setVvalue((Double) newValue));

        //Send username to the ClientHandler and get the dataManager
        try {
            output.writeObject(username);
            output.flush();

            dataManager = (DataManager) input.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

        //Set up the rooms and users
        ArrayList<String> users = dataManager.getUsers();
        ArrayList<String> rooms = dataManager.getRooms();
        for(String room : rooms){
            createButton(vboxGroups, room);
        }
        for(String user : users){
            if(!user.equals(username))
                createButton(vboxUsers, user);
        }
        changeRoom(rooms.get(0));

        listenForMessage();
    }

    @FXML
    public void sendMessage(KeyEvent keyEvent) {
        try {
            if (socket.isConnected() && keyEvent.getCode() == KeyCode.ENTER) {
                String messageToSend = messageField.getText();
                if (!messageToSend.isEmpty()) {
                    output.writeObject(new Message(username, currentRoomType, currentRoom, messageToSend));
                    output.flush();
                    displayMessageOut(messageToSend);

                    messageField.clear();
                }
            }
        }catch (IOException e){
            closeEverything(socket, output, input);
        }
    }
    @FXML
    void createRoom() {
        try{
            String roomName = roomNameField.getText();
            if(!roomName.isEmpty() && !dataManager.getRooms().contains(roomName) && !dataManager.getUsers().contains(roomName)){
                createButton(vboxGroups, roomName);
                dataManager.getRooms().add(roomName);

                output.writeObject(new Room(roomName));
                output.flush();

                roomNameField.clear();
            }
        } catch (IOException e) {
            closeEverything(socket, output, input);
        }
    }

    void changeRoom(String roomName){
        currentRoom = roomName;
        currentRoomType = "Room";
        Platform.runLater(() -> {
            receiverLabel.setText("[Room] " + roomName);
            vboxMessages.getChildren().clear();
        });
        loadMessages();
    }
    void changeUser(String userName){
        System.out.println("Changing room to " + userName + ", with type: User");
        currentRoom = userName;
        currentRoomType = "User";
        Platform.runLater(() -> {
            receiverLabel.setText("[User] " + userName);
            vboxMessages.getChildren().clear();
        });
        loadMessages();
    }

    void loadMessages(){
        try {
            output.writeObject("Load messages");
            output.flush();
        }catch (IOException e){
            closeEverything(socket, output, input);
        }
    }

    @FXML
    void exit(ActionEvent event) {
        closeEverything(socket, output, input);
        ((Stage)((Node)event.getSource()).getScene().getWindow()).close();
    }

    public void listenForMessage(){
        new Thread(() -> {
            while(socketIsOpen){
                try{
                    Object receivedObject = input.readObject();

                    if (receivedObject instanceof Message message) {
                        if(message.receiverType().equals("Room") && currentRoom.equals(message.receiver())){
                            if(username.equals(message.sender()))
                                displayMessageOut(message.text());
                            else
                                displayMessageIn(message.sender(), message.text());
                        }
                        else if(message.receiverType().equals("User")){
                            if(currentRoom.equals(message.sender()) && username.equals(message.receiver()))
                                displayMessageIn(message.sender(), message.text());
                            else if (currentRoom.equals(message.receiver()) && username.equals(message.sender()))
                                displayMessageOut(message.text());
                        }

                    } else if (receivedObject instanceof Room room) {
                        if(!dataManager.getRooms().contains(room.room())) {
                            createButton(vboxGroups, room.room());
                            dataManager.getRooms().add(room.room());
                        }

                    } else if (receivedObject instanceof User user){
                        if(!dataManager.getUsers().contains(user.user())) {
                            createButton(vboxUsers, user.user());
                            dataManager.getUsers().add(user.user());
                        }

                    } else if (receivedObject instanceof DataManager data){
                        dataManager = data;
                    }
                }catch (IOException | ClassNotFoundException e){
                    closeEverything(socket, output, input);
                }
            }
        }).start();
    }

    private void displayUsername(String message){
        Platform.runLater(() -> {
            HBox hbox = new HBox();
            hbox.setAlignment(Pos.CENTER_LEFT);
            hbox.setPadding(new Insets(0, 0, 0, 10));

            hbox.getChildren().add(new Text(message));
            vboxMessages.getChildren().add(hbox);
        });
    }
    private void displayMessageIn(String sender, String message){
        displayUsername(sender);
        Platform.runLater(() -> {
            HBox hbox = new HBox();
            hbox.setAlignment(Pos.CENTER_LEFT);
            hbox.setPadding(new Insets(5, 5, 5, 10));

            Text text = new Text(message);
            TextFlow textFlow = new TextFlow(text);
            textFlow.setStyle("-fx-color: rgb(0, 0, 0);" +
                    "-fx-background-color: rgb(212, 207, 207);" +
                    "-fx-background-radius: 20px");
            textFlow.setPadding(new Insets(5, 10, 5, 10));
            text.setFill(Color.color(0, 0, 0));

            hbox.getChildren().add(textFlow);
            vboxMessages.getChildren().add(hbox);
        });
    }
    private void displayMessageOut(String message){
        Platform.runLater(() -> {
            HBox hbox = new HBox();
            hbox.setAlignment(Pos.CENTER_RIGHT);
            hbox.setPadding(new Insets(5, 5, 5, 10));

            Text text = new Text(message);
            TextFlow textFlow = new TextFlow(text);
            textFlow.setStyle("-fx-color: rgb(239, 242, 255);" +
                    "-fx-background-color: rgb(15, 125, 242);" +
                    "-fx-background-radius: 20px");
            textFlow.setPadding(new Insets(5, 10, 5, 10));
            text.setFill(Color.color(0.934, 0.945, 0.996));

            hbox.getChildren().add(textFlow);
            vboxMessages.getChildren().add(hbox);
        });
    }
    private void createButton(VBox vbox, String name){
        Platform.runLater(() -> {
            Button button = new Button(name);
            button.setOnAction(event -> {
                if (vbox.equals(vboxGroups))
                    changeRoom(name);
                if (vbox.equals(vboxUsers))
                    changeUser(name);
            });
            button.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(button, Priority.ALWAYS);
            HBox hbox = new HBox();
            hbox.getChildren().add(button);
            vbox.getChildren().add(hbox);
        });
    }

    public void closeEverything(Socket socket, ObjectOutputStream output, ObjectInputStream input){
        try{
            if (output != null)
                output.close();
            if (input != null)
                input.close();
            if (socket != null)
                socket.close();
            socketIsOpen = false;
        }catch(IOException e){
            e.printStackTrace();
        }
    }
}
