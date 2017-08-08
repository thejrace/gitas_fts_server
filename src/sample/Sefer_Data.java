package sample;

import org.json.JSONObject;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Created by Obarey on 17.01.2017.
 */
public class Sefer_Data {
    private String no;
    private String hat;
    private String servis;
    private String guzergah;
    private String oto;
    private String surucu;
    private String surucu_sicil_no;
    private String surucu_telefon;
    private String gelis;
    private String orer;
    private String amir;
    private String gidis;
    private String tahmin;
    private String bitis;
    private String durum;
    private String durum_kodu;
    private String sure;
    private String plaka;
    private int gecerli;
    private int versiyon;


    public static String    DTAMAM = "T",
                            DBEKLEYEN = "B",
                            DAKTIF = "A",
                            DYARIM = "Y",
                            DIPTAL = "I";

    public Sefer_Data(
            String no,
            String hat,
            String servis,
            String guzergah,
            String oto,
            String surucu,
            String surucu_sicil_no,
            String surucu_telefon,
            String gelis,
            String orer,
            String sure,
            String amir,
            String gidis,
            String tahmin,
            String bitis,
            String durum,
            String durum_kodu
    ) {
        this.no = no;
        this.hat = hat;
        this.servis = servis;
        this.guzergah = guzergah;
        this.oto = oto;
        this.surucu = surucu;
        this.surucu_sicil_no = surucu_sicil_no;
        this.surucu_telefon = surucu_telefon;
        this.gelis = gelis;
        this.orer = orer;
        this.sure = sure;
        this.amir = amir;
        this.gidis = gidis;
        this.tahmin = tahmin;
        this.bitis = bitis;
        this.durum = durum;
        this.durum_kodu = durum_kodu;
    }

    public Sefer_Data(
            String no,
            String hat,
            String servis,
            String guzergah,
            String oto,
            String surucu,
            String surucu_sicil_no,
            String surucu_telefon,
            String gelis,
            String orer,
            String sure,
            String amir,
            String gidis,
            String tahmin,
            String bitis,
            String durum,
            String durum_kodu,
            String plaka,
            int gecerli,
            int versiyon
    ) {
        this.no = no;
        this.hat = hat;
        this.servis = servis;
        this.guzergah = guzergah;
        this.oto = oto;
        this.surucu = surucu;
        this.surucu_sicil_no = surucu_sicil_no;
        this.surucu_telefon = surucu_telefon;
        this.gelis = gelis;
        this.orer = orer;
        this.sure = sure;
        this.amir = amir;
        this.gidis = gidis;
        this.tahmin = tahmin;
        this.bitis = bitis;
        this.durum = durum;
        this.durum_kodu = durum_kodu;
        this.plaka = plaka;
        this.gecerli = gecerli;
        this.versiyon = versiyon;
    }

    public String get_no(){ return no; }
    public String get_hat(){ return hat; }
    public String get_servis(){ return servis; }
    public String get_guzergah(){ return guzergah; }
    public String get_oto(){ return oto; }
    public String get_surucu(){ return surucu; }
    public String get_surucu_sicil_no(){ return surucu_sicil_no; }
    public String get_surucu_telefon(){ return surucu_telefon; }
    public String get_gelis(){ return gelis; }
    public String get_orer(){ return orer; }
    public String get_sure(){ return sure; }
    public String get_amir(){ return amir; }
    public String get_gidis(){ return gidis; }
    public String get_tahmin(){ return tahmin; }
    public String get_bitis(){ return bitis; }
    public String get_durum(){ return durum; }
    public String get_durum_kodu(){ return durum_kodu; }
    public String get_plaka(){ return plaka; }
    public int get_gecerli(){ return gecerli; }
    public int get_versiyon(){ return versiyon; }

    public String stringify(){

        JSONObject jo = new JSONObject();
        jo.put("no", this.no);
        jo.put("hat", this.hat);
        jo.put("servis", this.servis);
        jo.put("guzergah", this.guzergah);
        jo.put("oto", this.oto);
        jo.put("surucu", this.surucu);
        jo.put("surucu_sicil_no", this.surucu_sicil_no);
        jo.put("surucu_tel", this.surucu_telefon);
        jo.put("gelis", this.gelis);
        jo.put("orer", this.orer);
        jo.put("sure", this.sure);
        jo.put("amir", this.amir);
        jo.put("gidis", this.gidis);
        jo.put("tahmin", this.tahmin);
        jo.put("bitis", this.bitis);
        jo.put("durum", this.durum);
        jo.put("durum_kodu", this.durum_kodu);

        return jo.toString();

    }

    public JSONObject tojson(){
        JSONObject jo = new JSONObject();
        jo.put("no", this.no);
        jo.put("hat", this.hat);
        jo.put("servis", this.servis);
        jo.put("guzergah", this.guzergah);
        jo.put("oto", this.oto);
        jo.put("surucu", this.surucu);
        jo.put("surucu_sicil_no", this.surucu_sicil_no);
        jo.put("surucu_tel", this.surucu_telefon);
        jo.put("gelis", this.gelis);
        jo.put("orer", this.orer);
        jo.put("sure", this.sure);
        jo.put("amir", this.amir);
        jo.put("gidis", this.gidis);
        jo.put("tahmin", this.tahmin);
        jo.put("bitis", this.bitis);
        jo.put("durum", this.durum);
        jo.put("durum_kodu", this.durum_kodu);
        jo.put("plaka", this.plaka);
        jo.put("versiyon", this.versiyon);
        jo.put("gecerli", this.gecerli);

        return jo;

    }

}
