module com.martynas.obj_5_chat {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.opencsv;


    opens com.martynas.obj_5_chat to javafx.fxml;
    exports com.martynas.obj_5_chat;
}