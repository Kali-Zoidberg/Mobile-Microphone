package threads;

import javafx.application.Application;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import javafx.stage.Stage;

public class  MobileMicGUI extends Application {

//    public static void main(String[] args) {
//        launch(args);
//    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Mobile Mic GUI");
        Text txt = new Text();
        txt.setText("Enter Server IP");

        Button startServerBtn= new Button();
        Button stopServerBtn = new Button();

        startServerBtn.setText("Start Server");
        stopServerBtn.setText("Stop Server");
        startServerBtn.setOnAction(new EventHandler<ActionEvent>() {
           @Override
            public void handle(ActionEvent event) {
             System.out.println("Hello world");
             System.out.println(event);
           };
        });
        GridPane root = new GridPane();
        int col = 0, row = 0, colspan = 1, rowspan = 2;
        root.add(txt, col++, row++, colspan++, rowspan++);
        root.add(startServerBtn, col,row++, colspan++,rowspan++);
        root.add(stopServerBtn,col,row++,colspan,rowspan);

        primaryStage.setScene(new Scene(root, 300, 250));
        primaryStage.show();
    }
}
