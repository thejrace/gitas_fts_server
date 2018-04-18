package sample;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Jeppe on 30.04.2017.
 */
public class Filo_Download implements Runnable {


    public static String filo5_cookie = "INIT";
    private ArrayList<String> otobusler = new ArrayList<>();
    private Map<String, String> cookies = new HashMap<>();

    private Thread thread;
    private boolean aktif = true;
    private int orer_download_frekans        = 120,
                mesaj_download_frekans       = 120,
                iys_download_frekans         = 120,
                pdks_download_frekans        = 120,
                hiz_download_frekans         = 120;
    private String aktif_tarih = "INIT";

    private int orer_download_durum = 1,
                    pdks_download_durum = 1,
                    iys_download_durum = 1,
                    mesaj_download_durum = 1,
                    hiz_download_durum = 1;

    public void start(){
        if( thread == null ){
            thread = new Thread( this );
            thread.setDaemon(true);
            thread.start();
        }
    }
    public void run(){

        Thread main_thread = new Thread(new Runnable() {
            @Override
            public void run() {
                while ( true ){
                    Filo_Senkronizasyon.aktif_gun_hesapla();
                    aktif_tarih = Filo_Senkronizasyon.get_aktif_gun();
                    if( aktif_tarih.equals("BEKLEMEDE") ){
                        System.out.println("BEKLEMEDE");
                        continue;
                    }
                    Connection con;
                    PreparedStatement pst;
                    ResultSet res;
                    try {
                        con = DBC.getInstance().getConnection();
                        pst = con.prepareStatement( "SELECT * FROM " + GitasDBT.SUNUCU_APP_CONFIG );
                        res = pst.executeQuery();
                        res.next();

                        //login_frekans = res.getInt("filo_giris_frekans");

                        orer_download_frekans = res.getInt("orer_download_frekans");
                        orer_download_durum = res.getInt("orer_download_durum");

                        mesaj_download_durum = res.getInt("mesaj_download_durum");
                        mesaj_download_frekans = res.getInt("mesaj_download_frekans");

                        iys_download_durum = res.getInt("iys_download_durum");
                        iys_download_frekans = res.getInt("iys_download_frekans");

                        pdks_download_durum = res.getInt("pdks_download_durum");
                        pdks_download_frekans = res.getInt("pdks_download_frekans");

                        hiz_download_durum = res.getInt("hiz_download_durum");
                        hiz_download_frekans = res.getInt("hiz_download_frekans");

                        //System.out.println("filo_giriş_frekans => " + login_frekans );
                        System.out.println("orer_download_durum => " + orer_download_durum + " ( "+orer_download_frekans+" ) " );
                        System.out.println("mesaj_download_durum => " + mesaj_download_durum + " ( "+mesaj_download_frekans+" ) " );
                        System.out.println("iys_download_durum => " + iys_download_durum + " ( "+iys_download_frekans+" ) " );
                        System.out.println("pdks_download_durum => " + pdks_download_durum + " ( "+pdks_download_frekans+" ) " );
                        System.out.println("hiz_download_durum => " + hiz_download_durum + " ( "+hiz_download_frekans+" ) " );

                        res.close();
                        pst.close();
                        con.close();

                    } catch( SQLException e ){
                        e.printStackTrace();
                    }
                    try {
                        Thread.sleep(60000);
                    } catch( InterruptedException e ){
                        e.printStackTrace();
                    }
                }

            }
        });
        main_thread.setDaemon(true);
        main_thread.start();

        Thread eski_veri_silme_thread = new Thread(new Runnable() {
            @Override
            public void run() {
                Connection con;
                PreparedStatement pst;
                ResultSet res;
                while( true ){

                    if( aktif_tarih.equals("BEKLEMEDE") && !aktif_tarih.equals("INIT") ){
                        try {
                            con = DBC.getInstance().getConnection();
                            pst = con.prepareStatement("SELECT kod FROM " + GitasDBT.OTOBUSLER + " WHERE durum = ? && sunucu_kayit = ?");
                            pst.setInt(1, 1);
                            pst.setInt(2, 0);
                            res = pst.executeQuery();
                            while (res.next()) {
                                pst = con.prepareStatement("DELETE FROM " + GitasDBT.ORER_KAYIT + " WHERE oto = ?");
                                pst.setString(1, res.getString("kod") );
                                pst.executeUpdate();
                            }
                            res.close();
                            pst.close();
                            con.close();
                        } catch( SQLException e ){
                            e.printStackTrace();
                        }
                    }
                    try {
                        Thread.sleep(3600000);
                    } catch( InterruptedException e ){
                        e.printStackTrace();
                    }
                }
            }
        });
        eski_veri_silme_thread.setDaemon(true);
        eski_veri_silme_thread.start();

        Thread orer_download_thread = new Thread(new Runnable() {
            private int frekans;
            @Override
            public void run() {
                Connection con;
                PreparedStatement pst;
                ResultSet res;
                Orer_Task orer_task;
                while( true ){
                    if( orer_download_durum ==  0 ){
                        try {
                            System.out.println("ORER DOWNLOAD DURDURULMUŞ!");
                            Thread.sleep(20000);
                            continue;
                        } catch( InterruptedException e ){
                            e.printStackTrace();
                        }
                    }
                    if( aktif_tarih.equals("BEKLEMEDE") ){
                        System.out.println("ORER DOWNLOAD BEKLEMEDE");
                        frekans = 1800000;
                    } else if( aktif_tarih.equals("INIT") ){
                        System.out.println("AKTIF TARIH YOK");
                        frekans = 10000;
                    } else {

                        frekans = orer_download_frekans * 1000;
                        try {
                            con = DBC.getInstance().getConnection();
                            pst = con.prepareStatement( "SELECT kod FROM " + GitasDBT.OTOBUSLER + " WHERE durum = ?" );
                            pst.setInt(1, 1);
                            res = pst.executeQuery();
                            while( res.next() ){
                                orer_task = new Orer_Task(res.getString("kod"), cookies.get(res.getString("kod").substring(0, 1)), aktif_tarih);
                                orer_task.yap();
                            }
                            try {
                                pst = con.prepareStatement( "UPDATE " + GitasDBT.SUNUCU_APP_CONFIG + " SET orer_download_timestamp = ? WHERE id = ?");
                                pst.setString(1, Common.get_current_datetime_db());
                                pst.setInt(2,1);
                                pst.execute();
                            } catch( SQLException e ){
                                e.printStackTrace();
                            }
                            res.close();
                            pst.close();
                            con.close();
                        } catch( SQLException e ){
                            e.printStackTrace();
                        }

                    }
                    try{
                        Thread.sleep( frekans );
                    } catch( InterruptedException e ){
                        e.printStackTrace();
                    }
                }

            }
        });
        orer_download_thread.setDaemon(true);
        orer_download_thread.start();

        Thread pdks_download_thread = new Thread(new Runnable() {
            private int frekans;
            @Override
            public void run() {
                Connection con;
                PreparedStatement pst;
                ResultSet res;
                PDKS_Task pdks_task;
                while( true ) {
                    if( pdks_download_durum ==  0 ){
                        try {
                            System.out.println("PDKS DOWNLOAD DURDURULMUŞ!");
                            Thread.sleep(20000);
                            continue;
                        } catch( InterruptedException e ){
                            e.printStackTrace();
                        }
                    }

                    if( aktif_tarih.equals("BEKLEMEDE") ){
                        System.out.println("PDKS DOWNLOAD BEKLEMEDE");
                        frekans = 1800000;
                    } else if( aktif_tarih.equals("INIT") ){
                        System.out.println("AKTIF TARIH YOK");
                        frekans = 10000;
                    } else {

                        frekans = pdks_download_frekans * 1000;
                        try {
                            con = DBC.getInstance().getConnection();
                            pst = con.prepareStatement("SELECT kod FROM " + GitasDBT.OTOBUSLER + " WHERE durum = ?");
                            pst.setInt(1, 1);
                            res = pst.executeQuery();
                            while (res.next()) {
                                pdks_task = new PDKS_Task(res.getString("kod"), cookies.get(res.getString("kod").substring(0, 1)), aktif_tarih);
                                pdks_task.yap();
                            }
                            try {
                                pst = con.prepareStatement( "UPDATE " + GitasDBT.SUNUCU_APP_CONFIG + " SET pdks_download_timestamp = ? WHERE id = ?");
                                pst.setString(1, Common.get_current_datetime_db());
                                pst.setInt(2,1);
                                pst.execute();
                            } catch( SQLException e ){
                                e.printStackTrace();
                            }
                            res.close();
                            pst.close();
                            con.close();
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }

                    }
                    try {
                        Thread.sleep(frekans);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        pdks_download_thread.setDaemon(true);
        pdks_download_thread.start();

        Thread hiz_download_thread = new Thread(new Runnable() {
            private int frekans;
            @Override
            public void run() {
                Connection con;
                PreparedStatement pst;
                ResultSet res;
                Hiz_Download_Task hiz_download_task;
                while( true ) {
                    if( hiz_download_durum ==  0 ){
                        try {
                            System.out.println("HIZ DOWNLOAD DURDURULMUŞ!");
                            Thread.sleep(20000);
                            continue;
                        } catch( InterruptedException e ){
                            e.printStackTrace();
                        }
                    }
                    frekans = hiz_download_frekans * 1000;
                    try {
                        con = DBC.getInstance().getConnection();
                        pst = con.prepareStatement("SELECT kod FROM " + GitasDBT.OTOBUSLER + " WHERE durum = ?");
                        pst.setInt(1, 1);
                        res = pst.executeQuery();
                        while (res.next()) {
                            hiz_download_task = new Hiz_Download_Task(res.getString("kod") );
                            hiz_download_task.yap();
                        }
                        try {
                            pst = con.prepareStatement( "UPDATE " + GitasDBT.SUNUCU_APP_CONFIG + " SET hiz_download_timestamp = ? WHERE id = ?");
                            pst.setString(1, Common.get_current_datetime_db());
                            pst.setInt(2,1);
                            pst.execute();
                        } catch( SQLException e ){
                            e.printStackTrace();
                        }
                        res.close();
                        pst.close();
                        con.close();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                    try {
                        Thread.sleep(frekans);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        hiz_download_thread.setDaemon(true);
        hiz_download_thread.start();

        Thread mesaj_download_thread = new Thread(new Runnable() {
            private int frekans;
            @Override
            public void run() {
                Connection con;
                PreparedStatement pst;
                ResultSet res;
                Mesaj_Task mesaj_task;
                while( true ){
                    if( mesaj_download_durum == 0 ){
                        try {
                            System.out.println("MESAJ DOWNLOAD DURDURULMUŞ!");
                            Thread.sleep(20000);
                            continue;
                        } catch( InterruptedException e ){
                            e.printStackTrace();
                        }
                    }

                    if( aktif_tarih.equals("BEKLEMEDE") ){
                        System.out.println("MESAJ DOWNLOAD BEKLEMEDE");
                        frekans = 1800000;
                    } else if( aktif_tarih.equals("INIT") ){
                        System.out.println("AKTIF TARIH YOK");
                        frekans = 10000;
                    } else {

                        frekans = mesaj_download_frekans * 1000;
                        try {
                            con = DBC.getInstance().getConnection();
                            pst = con.prepareStatement("SELECT kod FROM " + GitasDBT.OTOBUSLER + " WHERE durum = ?");
                            pst.setInt(1, 1);
                            res = pst.executeQuery();
                            while (res.next()) {
                                mesaj_task = new Mesaj_Task( res.getString("kod"), cookies.get(res.getString("kod").substring(0,1) ), aktif_tarih );
                                mesaj_task.yap();
                            }
                            try {
                                pst = con.prepareStatement( "UPDATE " + GitasDBT.SUNUCU_APP_CONFIG + " SET mesaj_download_timestamp = ? WHERE id = ?");
                                pst.setString(1, Common.get_current_datetime_db());
                                pst.setInt(2,1);
                                pst.execute();
                            } catch( SQLException e ){
                                e.printStackTrace();
                            }
                            res.close();
                            pst.close();
                            con.close();
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }

                    }
                    try {
                        Thread.sleep(frekans);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        mesaj_download_thread.setDaemon(true);
        mesaj_download_thread.start();

        Thread iys_download_thread = new Thread(new Runnable() {
            private int frekans;
            @Override
            public void run() {
                Connection con;
                PreparedStatement pst;
                ResultSet res;
                IYS_Task iys_task;
                while( true ){
                    if( iys_download_durum == 0 ){
                        try {
                            System.out.println("IYS DOWNLOAD DURDURULMUŞ!");
                            Thread.sleep(20000);
                            continue;
                        } catch( InterruptedException e ){
                            e.printStackTrace();
                        }
                    }
                    if( aktif_tarih.equals("BEKLEMEDE") ){
                        System.out.println("IYS DOWNLOAD BEKLEMEDE");
                        frekans = 1800000;
                    } else if( aktif_tarih.equals("INIT") ){
                        System.out.println("AKTIF TARIH YOK");
                        frekans = 10000;
                    } else {

                        frekans = iys_download_frekans * 1000;
                        try {
                            con = DBC.getInstance().getConnection();
                            pst = con.prepareStatement("SELECT kod FROM " + GitasDBT.OTOBUSLER + " WHERE durum = ?");
                            pst.setInt(1, 1);
                            res = pst.executeQuery();
                            otobusler = new ArrayList<>();
                            while( res.next() ) otobusler.add( res.getString("kod"));
                            iys_task = new IYS_Task(otobusler, cookies, aktif_tarih);
                            iys_task.yap();

                            try {
                                pst = con.prepareStatement( "UPDATE " + GitasDBT.SUNUCU_APP_CONFIG + " SET iys_download_timestamp = ? WHERE id = ?");
                                pst.setString(1, Common.get_current_datetime_db());
                                pst.setInt(2,1);
                                pst.execute();
                            } catch( SQLException e ){
                                e.printStackTrace();
                            }

                            res.close();
                            pst.close();
                            con.close();
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }

                    }

                    try {
                        Thread.sleep(frekans);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        iys_download_thread.setDaemon(true);
        iys_download_thread.start();


    }

    public void stop(){
        aktif = false;
    }

}














/*       24.11.2017 - captcha sonrasi çöp oldu
        Thread login_thread = new Thread(new Runnable() {
            private int frekans;
            @Override
            public void run() {
                Login_Task login_task;
                Connection con;
                PreparedStatement pst;
                while( true ){

                    if( aktif_tarih.equals("BEKLEMEDE") ) {
                        System.out.println("FILO GIRIS BEKLEMEDE");
                        frekans = 1800000;
                    } else if( aktif_tarih.equals("INIT") ){
                        System.out.println("AKTIF TARIH YOK");
                        frekans = 10000;
                    } else {
                        if( !init_data_ok ) {
                            frekans = 10000;
                        } else {
                            frekans = login_frekans * 1000;
                            login_ok = false;
                            login_task = new Login_Task();
                            login_task.yap();
                            cookies = login_task.get_cookies();
                            try {
                                con = DBC.getInstance().getConnection();
                                pst = con.prepareStatement( "UPDATE " + GitasDBT.SUNUCU_APP_CONFIG + " SET filo_giris_timestamp = ? WHERE id = ?");
                                pst.setString(1, Common.get_current_datetime_db());
                                pst.setInt(2,1);
                                pst.execute();

                                pst.close();
                                con.close();
                                login_ok = true;
                            } catch( SQLException e ){
                                e.printStackTrace();
                            }
                        }
                    }
                    try{
                        Thread.sleep( frekans );
                    } catch( InterruptedException e ){
                        e.printStackTrace();
                    }
                }
            }
        });
        login_thread.setDaemon(true);
        login_thread.start();
*/