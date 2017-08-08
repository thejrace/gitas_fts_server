package sample;

import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by Jeppe on 30.04.2017.
 */
public interface Filo_Download_Listener {

    void on_orer_finish(JSONObject seferler, JSONObject aktif_sefer_verileri );
    void on_mesaj_finish();
    void on_iys_finish();
    void on_pdks_finish();
    void on_login_finish();

}
