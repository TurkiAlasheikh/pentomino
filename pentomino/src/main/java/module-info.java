module com.example.pentomino {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.example.pentomino to javafx.fxml;
    exports com.example.pentomino;
}