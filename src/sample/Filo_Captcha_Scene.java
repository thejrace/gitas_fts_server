package sample;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.WindowEvent;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class Filo_Captcha_Scene extends Application {

    private VBox root;
    private Stage stage;
    private Filo_Captcha_Controller controller;
    @Override
    public void start( Stage primaryStage ) throws Exception{

        FXMLLoader fxmlLoader;
        try {
            fxmlLoader = new FXMLLoader(getClass().getResource("sample.fxml"));
            Parent root = fxmlLoader.load();
            controller = fxmlLoader.getController();
            primaryStage.setTitle("Gitaş Filo Takip");
            primaryStage.initStyle(StageStyle.DECORATED);
            primaryStage.setScene(new Scene(root, 500, 250));
            primaryStage.show();
            stage = primaryStage;
            primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
                @Override
                public void handle(WindowEvent e) {
                    Platform.exit();
                    System.exit(0);
                }
            });

            controller.add_finish_listener(new Refresh_Listener() {
                @Override
                public void on_refresh() {
                    try{
                        try {
                            Connection con =  DBC.getInstance().getConnection();
                            PreparedStatement pst = con.prepareStatement( "UPDATE " + GitasDBT.SUNUCU_APP_CONFIG + " SET filo_giris_timestamp = ? WHERE id = ?");
                            pst.setString(1, Common.get_current_datetime_db());
                            pst.setInt(2,1);
                            pst.execute();
                            pst.close();
                            con.close();
                        } catch( SQLException e ){
                            e.printStackTrace();
                        }
                        Filo_Download fd = new Filo_Download();
                        fd.start();
                        //Test test = new Test();
                        //test.init();
                        //stage.close();
                    } catch( Exception e ){
                        e.printStackTrace();
                    }
                }
            });

        } catch( Exception e ){
            e.printStackTrace();
        }

    }


    public static void main(String[] args) {
        launch(args);
    }






}
