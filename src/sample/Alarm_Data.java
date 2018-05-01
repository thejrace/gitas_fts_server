package sample;

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
            MESAJ_SURUCU_COK_CALISTI = "%%ISIM%% çalışma süre limitini aştı.";

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

