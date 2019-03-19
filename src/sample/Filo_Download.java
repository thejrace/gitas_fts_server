package sample;

import org.json.JSONException;

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
    private int orer_download_frekans        = 120,
                mesaj_download_frekans       = 120,
                iys_download_frekans         = 120,
                pdks_download_frekans        = 120,
                hiz_download_frekans         = 120,
                eski_veri_silme_frekans      = 120,
                otobus_aktif_durum_download_frekans = 60;
    private String aktif_tarih = "INIT";

    private int orer_download_durum = 0,
                    pdks_download_durum = 0,
                    iys_download_durum = 0,
                    mesaj_download_durum = 0,
                    hiz_download_durum = 0,
                    eski_veri_silme_durum = 0,
                    otobus_aktif_durum_download_durum = 0,
                    hiz_limit = 70;

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
                        hiz_limit = res.getInt("hiz_limit");

                        eski_veri_silme_durum = res.getInt("eski_veri_silme_durum");
                        eski_veri_silme_frekans = res.getInt("eski_veri_silme_frekans");

                        otobus_aktif_durum_download_frekans = res.getInt("otobus_aktif_durum_download_frekans");
                        otobus_aktif_durum_download_durum = res.getInt("otobus_aktif_durum_download_durum");

                        //System.out.println("filo_giriş_frekans => " + login_frekans );
                        System.out.println("orer_download_durum => " + orer_download_durum + " ( "+orer_download_frekans+" ) " );
                        System.out.println("oadd_download_durum => " + otobus_aktif_durum_download_durum + " ( "+otobus_aktif_durum_download_frekans+" ) " );
                        System.out.println("mesaj_download_durum => " + mesaj_download_durum + " ( "+mesaj_download_frekans+" ) " );
                        System.out.println("iys_download_durum => " + iys_download_durum + " ( "+iys_download_frekans+" ) " );
                        System.out.println("pdks_download_durum => " + pdks_download_durum + " ( "+pdks_download_frekans+" ) " );
                        System.out.println("hiz_download_durum => " + hiz_download_durum + " ( "+hiz_download_frekans+" ) ( hiz limit : "+hiz_limit+" )" );
                        System.out.println("eski_veri_silme_durum => " + eski_veri_silme_durum + " ( "+eski_veri_silme_frekans+" )" );
                        res.close();
                        pst.close();
                        con.close();
                    } catch( SQLException e ){
                        e.printStackTrace();
                    }
                    try {
                        Thread.sleep(60000);
                    } catch( InterruptedException e ){
                        Common.exception_db_kayit("Filo_Download.java - main_thread", e);
                        e.printStackTrace();
                    }
                }
            }
        });
        main_thread.setDaemon(true);
        main_thread.start();

        Thread eski_veri_silme_thread = new Thread(new Runnable() {
            private int frekans;
            @Override
            public void run() {
                Connection con;
                PreparedStatement pst;
                ResultSet res;
                while( true ){
                    if( eski_veri_silme_durum ==  0 ){
                        try {
                            System.out.println("ESKİ VERİ SİLME DURDURULMUŞ!");
                            Thread.sleep(20000);
                            continue;
                        } catch( InterruptedException e ){
                            e.printStackTrace();
                        }
                    }
                    frekans = eski_veri_silme_frekans * 1000;
                    try {
                        con = DBC.getInstance().getConnection();
                        pst = con.prepareStatement("SELECT kod, sunucu_kayit FROM " + GitasDBT.OTOBUSLER + " WHERE durum = ?");
                        pst.setInt(1, 1);
                        res = pst.executeQuery();
                        while (res.next()) {
                            // orer silme
                            if( res.getInt("sunucu_kayit") == 0 ){
                                // dünden önceki kayitlari uçur
                                pst = con.prepareStatement("DELETE FROM " + GitasDBT.ORER_KAYIT + " WHERE oto = ? && tarih <= ?");
                                pst.setString(1, res.getString("kod") );
                                pst.setString(2, Common.get_yesterday_date() );
                                pst.executeUpdate();
                                // pdks verilerini sil
                                pst = con.prepareStatement("DELETE FROM " + GitasDBT.PDKS_KAYIT + " WHERE oto = ? && tarih <= ?");
                                pst.setString(1, res.getString("kod"));
                                pst.setString(2, Common.get_yesterday_date() );
                                pst.executeUpdate();
                                // mesaj verilerini sil
                                pst = con.prepareStatement("DELETE FROM " + GitasDBT.FILO_MESAJLAR + " WHERE oto = ? && tarih <= ?");
                                pst.setString(1, res.getString("kod"));
                                pst.setString(2, Common.get_yesterday_date() );
                                pst.executeUpdate();
                                // giden mesaj verilerini sil
                                pst = con.prepareStatement("DELETE FROM " + GitasDBT.FILO_MESAJLAR_GIDEN + " WHERE oto = ? && tarih <= ?");
                                pst.setString(1, res.getString("kod"));
                                pst.setString(2, Common.get_yesterday_date() );
                                pst.executeUpdate();
                                // iys kayitlarini sil
                                pst = con.prepareStatement("DELETE FROM " + GitasDBT.IYS_KAYIT + " WHERE oto = ? && tarih <= ?");
                                pst.setString(1, res.getString("kod"));
                                pst.setString(2, Common.get_yesterday_date() );
                                pst.executeUpdate();
                                // hiz kayitlarini sil
                                pst = con.prepareStatement("DELETE FROM " + GitasDBT.FILO_HIZ_KAYITLARI + " WHERE oto = ? && tarih <= ?");
                                pst.setString(1, res.getString("kod"));
                                pst.setString(2, Common.get_yesterday_date() + " 00:00:00");
                                pst.executeUpdate();
                            }
                            // alarm verilerini sil
                            pst = con.prepareStatement("DELETE FROM " + GitasDBT.OTOBUS_ALARM_DATA + " WHERE oto = ? && tarih <= ?");
                            pst.setString(1, res.getString("kod"));
                            pst.setString(2, Common.get_yesterday_date() + " 00:00:00");
                            pst.executeUpdate();
                            // alarm gorulme verilerini sil
                            pst = con.prepareStatement("DELETE FROM " + GitasDBT.OTOBUS_ALARM_DATA_GORENLER + " WHERE tarih <= ?");
                            pst.setString(1, Common.get_yesterday_date() + " 00:00:00");
                            pst.executeUpdate();
                        }
                        pst = con.prepareStatement( "UPDATE " + GitasDBT.SUNUCU_APP_CONFIG + " SET eski_veri_silme_timestamp = ? WHERE id = ?");
                        pst.setString(1, Common.get_current_datetime_db());
                        pst.setInt(2,1);
                        pst.execute();
                        res.close();
                        pst.close();
                        con.close();
                        System.out.println("ESKİ VERİ SİLME THREAD BITTI");
                    } catch( SQLException e ){
                        e.printStackTrace();
                    }
                    try {
                        Thread.sleep(frekans);
                    } catch( InterruptedException e ){
                        Common.exception_db_kayit("Filo_Download.java - eski_veri_silme_thread", e);
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
                        Common.exception_db_kayit("Filo_Download.java - orer_download_thread", e );
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
                        Common.exception_db_kayit("Filo_Download.java - pdks_download_thread", e );
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
                            hiz_download_task = new Hiz_Download_Task(res.getString("kod"), hiz_limit  );
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
                        Common.exception_db_kayit("Filo_Download.java - hiz_download_thread", e );
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

                            pst = con.prepareStatement( "UPDATE " + GitasDBT.SUNUCU_APP_CONFIG + " SET mesaj_download_timestamp = ? WHERE id = ?");
                            pst.setString(1, Common.get_current_datetime_db());
                            pst.setInt(2,1);
                            pst.execute();

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
                        Common.exception_db_kayit("Filo_Download.java - mesaj_download_thread", e);
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
                        Common.exception_db_kayit("Filo_Download.java - iys_download_thread", e);
                        e.printStackTrace();
                    }
                }
            }
        });
        iys_download_thread.setDaemon(true);
        iys_download_thread.start();

        Thread otobus_aktif_durum_download_thread = new Thread(new Runnable() {
            private int frekans;
            private Orer_Task orer_task;
            @Override
            public void run() {
                while( true ){
                    if( otobus_aktif_durum_download_durum == 0 ){
                        try {
                            System.out.println("OADD DOWNLOAD DURDURULMUŞ!");
                            Thread.sleep(20000);
                            continue;
                        } catch( InterruptedException e ){
                            e.printStackTrace();
                        }
                    }
                    if( aktif_tarih.equals("BEKLEMEDE") ){
                        System.out.println("OADD DOWNLOAD BEKLEMEDE");
                        frekans = 1800000;
                    } else if( aktif_tarih.equals("INIT") ){
                        System.out.println("AKTIF TARIH YOK");
                        frekans = 10000;
                    } else {
                        frekans = otobus_aktif_durum_download_frekans * 1000;
                        Connection con;
                        PreparedStatement pst;
                        ResultSet res;
                        try {
                            con = DBC.getInstance().getConnection();
                            pst = con.prepareStatement( "SELECT kod FROM " + GitasDBT.OTOBUSLER + " WHERE durum = ?" );
                            pst.setInt(1, 1);
                            res = pst.executeQuery();
                            while( res.next() ){
                                orer_task = new Orer_Task(res.getString("kod"), cookies.get(res.getString("kod").substring(0, 1)), aktif_tarih);
                                orer_task.set_mobil_flag();
                                orer_task.yap();
                            }
                            pst = con.prepareStatement( "UPDATE " + GitasDBT.SUNUCU_APP_CONFIG + " SET otobus_aktif_durum_download_timestamp = ? WHERE id = ?");
                            pst.setString(1, Common.get_current_datetime_db());
                            pst.setInt(2,1);
                            pst.execute();
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
                        Common.exception_db_kayit("Filo_Download.java - OADD_thread", e );
                        e.printStackTrace();
                    }
                }
            }
        });
        otobus_aktif_durum_download_thread.setDaemon(true);
        otobus_aktif_durum_download_thread.start();
    }

    private void init_oto_orer_download( final String _oto, final int index ){

        Orer_Task orer_task = new Orer_Task(_oto, cookies.get(_oto.substring(0, 1)), aktif_tarih);
        Thread th = new Thread(new Runnable() {
            private int frekans;
            @Override
            public void run() {
                try {
                    Thread.sleep( index * 3000 );
                } catch( InterruptedException e ){
                    e.printStackTrace();
                }
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
                    }
                    orer_task.yap();
                    try {
                        Thread.sleep( frekans );
                    } catch( InterruptedException e ){
                        e.printStackTrace();
                    }
                }
            }
        });
        th.setDaemon(true);
        th.start();
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