package com.martynas.obj_5_chat;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.Objects;

public class ClientMain extends Application {

    @Override
    public void start(Stage stage){
        try {
            Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("login.fxml")));
            stage.setTitle("Chat communications system");
            stage.setScene(new Scene(root));
            stage.setResizable(false);
            stage.show();
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch();
    }
}
