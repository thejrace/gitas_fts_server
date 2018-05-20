package sample;

import java.sql.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by Obarey on 01.02.2017.
 */
public class Alarm_Data {

    private int index = 0;
    private String kod, mesaj;
    private boolean goruldu = false;
    private String sefer_no = "0"; // iys ve plaka icin
    private int type;
    private int oncelik;
    private String tarih;

    public static int   SEFER_IPTAL = 0,
            SEFER_YARIM = 1,
            SEFERLER_DUZELTILDI = 2,
            SURUCU_DEGISIMI = 3,
            BELIRSIZ_SURUCU = 4,
            GEC_KALMA = 5,
            SEFER_BASLAMADI = 6,
            AMIR_SAAT_ATADI = 7,
            NOT_VAR = 8,
            NOT_TAMAMLANDI = 9,
            YENI_NOT_BILDIRIMI = 10,
            IYS_UYARISI_VAR = 11,
            SURUCU_COK_CALISTI = 12;


    public static String    MESAJ_IPTAL = "Sefer iptalleri var!",
            MESAJ_YARIM = "Sefer yarım kaldı!",
            MESAJ_GEC_KALMA = "Bir sonraki seferine yetişemeyebilir!",
            MESAJ_SEFER_BASLAMADI = "Saati gelen seferine başlamadı!",
            MESAJ_AMIR_SAAT_ATADI = "Bir sonraki seferine amir saat atadı!",
            MESAJ_SEFERLER_DUZELTILDI = "Seferler düzeltildi!",
            MESAJ_BELIRSIZ_SURUCU = "Sürücü bilgisi yok!",
            MESAJ_SURUCU_DEGISTI = "Sürücü değişimi oldu!",
            MESAJ_IYS_UYARISI_VAR = "IYS uyarısı var!",
            MESAJ_NOT_VAR = "Yeni not var!",
            MESAJ_NOT_TAMAMLANDI = "Not tamamlandı!",
            MESAJ_NOT_BILDIRIMI = "Yeni not bildirimi var!",
            MESAJ_SURUCU_COK_CALISTI = "%%ISIM%% çalışma süre limitini(%%SAAT%% saat) aştı.";

    public static int   KIRMIZI = 1,
            TURUNCU = 2,
            MAVI = 3,
            YESIL = 4,
            SURUCU_FLIP_FLOP = 5,
            MAVI_FLIP_FLOP = 6;



    public Alarm_Data( int type, int oncelik, String kod, String mesaj, String sefer_no ){
        this.kod = kod;
        this.mesaj = mesaj;
        this.type = type;
        this.sefer_no = sefer_no;
        this.oncelik = oncelik;

        DateFormat dateFormat = new SimpleDateFormat("HH:mm");
        Date date = new Date();
        this.tarih = dateFormat.format(date);
    }

    public static void db_insert( Alarm_Data alarm_data, String oto, String aktif_tarih ){
        try {
            Connection con = DBC.getInstance().getConnection();
            PreparedStatement pst_2 = null;
            ResultSet res;
            PreparedStatement pst = con.prepareStatement("SELECT * FROM " + GitasDBT.OTOBUS_ALARM_DATA + " WHERE oto = ? && alarm_tipi = ? && sefer_no = ? && tarih >= ? ");
            pst.setString(1, oto);
            pst.setInt(2, alarm_data.get_type());
            pst.setInt(3, Integer.valueOf(alarm_data.get_sefer_no()));
            pst.setString(4, aktif_tarih + " 05:00:00");
            res = pst.executeQuery();
            if( !res.next() ){
                pst_2 = con.prepareStatement("INSERT INTO " + GitasDBT.OTOBUS_ALARM_DATA + "( oto, alarm_tipi, alarm_mesaj, sefer_no, tarih ) VALUES ( ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS );
                pst_2.setString(1, oto);
                pst_2.setInt(2, alarm_data.get_type());
                pst_2.setString(3, alarm_data.get_mesaj());
                pst_2.setInt(4, Integer.valueOf(alarm_data.get_sefer_no()) );
                pst_2.setString(5, Common.get_current_datetime_db());
                pst_2.executeUpdate();
                ResultSet last_inserted_res = pst_2.getGeneratedKeys();
                last_inserted_res.next();
                PreparedStatement pst_4 = con.prepareStatement("SELECT eposta FROM " + GitasDBT.APP_KULLANICILAR + " WHERE durum = ?");
                pst_4.setInt(1, 1);
                ResultSet res_2 = pst_4.executeQuery();
                PreparedStatement pst_3 = null;
                // alarm gorenler performans icin ters calisiyor
                // - yeni alarm eklendiginde, her kullanici icin "gormedi kaydı" olusturuyoruz
                // - kullanici alarmlari indiririken alarmlar tablosunu degil, alarm_gorenler tablosunu tariyacak
                // - yeni alarm varsa onu indirecek, o alarmin gorulmedi kaydi tablodan silinecek
                while( res_2.next() ){
                    pst_3 = con.prepareStatement("INSERT INTO " + GitasDBT.OTOBUS_ALARM_DATA_GORENLER + " ( kullanici, alarm_id, tarih ) VALUES ( ?, ?, ? )" );
                    pst_3.setString(1, res_2.getString("eposta"));
                    pst_3.setInt(2, last_inserted_res.getInt(1));
                    pst_3.setString(3, Common.get_current_datetime_db());
                    pst_3.executeUpdate();
                }
                pst_3.close();
                res_2.close();
                pst_4.close();
                last_inserted_res.close();
                res.close();
                pst_2.close();
            }
            pst.close();
            con.close();
        } catch( SQLException e ){
            e.printStackTrace();
        }

    }

    public boolean aktif_mi( Alarm_Data farkli_data ){
        return farkli_data.get_sefer_no().equals(this.sefer_no) && farkli_data.get_type() == this.type;
    }

    public void set_index( int index ){
        this.index = index;
    }
    public int get_index(){
        return this.index;
    }

    public String get_key(){
        return type+"-"+sefer_no;
    }

    public String get_mesaj(){
        return this.mesaj;
    }

    public String get_kod(){
        return this.kod;
    }

    public String get_tarih(){
        return this.tarih;
    }

    public String get_sefer_no(){
        return this.sefer_no;
    }

    public int get_type(){
        return this.type;
    }

    public int get_oncelik(){
        return this.oncelik;
    }

    public void goruldu( boolean  flag ){
        this.goruldu = flag;
    }

    public boolean get_goruldu(){
        return this.goruldu;
    }

}

