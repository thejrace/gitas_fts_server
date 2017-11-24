package sample;

import javafx.fxml.Initializable;
import java.net.URL;
import java.util.ResourceBundle;

public class Controller  implements Initializable {
    @Override
    public void initialize(URL fxmlFileLocation, ResourceBundle resources) {

        Filo_Download filo_download;
        filo_download = new Filo_Download();
        filo_download.start();


    }






}