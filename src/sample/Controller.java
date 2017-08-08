package sample;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import org.json.JSONObject;

import java.net.URL;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

public class Controller  implements Initializable {

    @FXML
    private Label lbl_orer;

    @FXML
    private Label lbl_iys;

    @FXML
    private Label lbl_mesaj;

    @FXML
    private Label lbl_pdks;

    @FXML
    private Label lbl_aktif_gun;

    @FXML
    private Label lbl_login;

    private Filo_Download filo_download;


    @Override
    public void initialize(URL fxmlFileLocation, ResourceBundle resources) {


        filo_download = new Filo_Download();
        filo_download.add_listener(new Filo_Download_Listener() {
            @Override
            public void on_orer_finish(JSONObject seferler, JSONObject aktif_sefer_verileri) {
                Platform.runLater(new Runnable(){ @Override public void run(){
                    lbl_orer.setText("SON ORER: " + Common.get_current_hmin() );
                    lbl_aktif_gun.setText("AKTİF GÜN: " + filo_download.get_aktif_tarih());
                }});
            }

            @Override
            public void on_mesaj_finish() {
                Platform.runLater(new Runnable(){ @Override public void run(){
                    lbl_mesaj.setText("SON MESAJ: " + Common.get_current_hmin() );
                    lbl_aktif_gun.setText("AKTİF GÜN: " + filo_download.get_aktif_tarih());
                }} );
            }

            @Override
            public void on_iys_finish() {
                Platform.runLater(new Runnable(){ @Override public void run(){
                    lbl_iys.setText("SON IYS: " + Common.get_current_hmin() );
                    lbl_aktif_gun.setText("AKTİF GÜN: " + filo_download.get_aktif_tarih());
                }} );
            }
            @Override
            public void on_pdks_finish() {
                Platform.runLater(new Runnable(){ @Override public void run(){
                    lbl_pdks.setText("SON PDKS: " + Common.get_current_hmin() );
                    lbl_aktif_gun.setText("AKTİF GÜN: " + filo_download.get_aktif_tarih());
                }} );
            }
            @Override
            public void on_login_finish(){
                Platform.runLater(new Runnable(){ @Override public void run(){
                    lbl_login.setText("SON GİRİŞ: " + Common.get_current_hmin() );
                }} );
            }
        });
        filo_download.start();


    }






}

