
package com.martynas.obj_5_chat;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.net.Socket;
import java.util.Objects;

public class LoginController {

    @FXML
    private TextField usernameField;

    @FXML
    void logIn(ActionEvent event) {
        try {
            String username = usernameField.getText();

            System.out.println("Client started");
            Socket socket = new Socket("localhost", 8008);


            ClientController clientController = new ClientController(socket, username);
            FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(getClass().getResource("client.fxml")));
            loader.setController(clientController);
            Parent root = loader.load();
            Scene scene = new Scene(root);


            Stage stage = (Stage)((Node)event.getSource()).getScene().getWindow();
            stage.setScene(scene);
            stage.show();
        }catch(Exception e){
            e.printStackTrace();
        }
    }

}
