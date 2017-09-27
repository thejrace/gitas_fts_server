package sample;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception{
        Parent root = FXMLLoader.load(getClass().getResource("sample.fxml"));
        primaryStage.setTitle("Gitaş FTS Sunucu Versiyon");
        primaryStage.setScene(new Scene(root, 300, 275));
        //primaryStage.show();

        /*try {
            System.setOut(new PrintStream(new File("C:\\output.txt")));
        } catch (Exception e) {
            e.printStackTrace();
        }*/

    }


    public static void main(String[] args) {
        launch(args);
    }
}
