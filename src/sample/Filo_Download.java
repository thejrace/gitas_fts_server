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
    private boolean init_data_ok = false,
                    login_ok = false;
    private Map<String, String> cookies = new HashMap<>();

    private Thread thread;
    private boolean aktif = true;
    private int orer_download_frekans        = 120,
                mesaj_download_frekans       = 120,
                iys_download_frekans         = 120,
                pdks_download_frekans        = 120,
                login_frekans       = 7200;
    private String aktif_tarih = "INIT";

    private int orer_download_durum = 1,
                    pdks_download_durum = 1,
                    iys_download_durum = 1,
                    mesaj_download_durum = 1;

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

                        init_data_ok = true;

                        //System.out.println("filo_giriş_frekans => " + login_frekans );
                        System.out.println("orer_download_durum => " + orer_download_durum );
                        System.out.println("mesaj_download_durum => " + mesaj_download_durum );
                        System.out.println("iys_download_durum => " + iys_download_durum );
                        System.out.println("pdks_download_durum => " + pdks_download_durum );

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
class Filo_Task {
    protected String oto, cookie, aktif_tarih, logprefix;
    protected org.jsoup.Connection.Response istek_yap( String url ){
        try {
            return Jsoup.connect(url + oto)
                    .cookie("PHPSESSID", Filo_Download.filo5_cookie )
                    .method(org.jsoup.Connection.Method.POST)
                    .timeout(40*1000)
                    .execute();
        } catch (IOException | NullPointerException e) {
            System.out.println( "["+Common.get_current_hmin() + "]  "+ aktif_tarih  + " " +  oto + " " + logprefix + "veri alım hatası. Tekrar deneniyor.");
            e.printStackTrace();
        }
        return null;
    }
    protected Document parse_html( org.jsoup.Connection.Response req ){
        try {
            return req.parse();
        } catch( IOException | NullPointerException e ){
            System.out.println(  "["+Common.get_current_hmin() + "]  "+ aktif_tarih  + " " +  oto + " "+ logprefix + " parse hatası. Tekrar deneniyor.");
        }
        return null;
    }
}

class Rotasyon_Task extends Filo_Task{

    private String bolge;
    private ArrayList<String> otobusler;
    private Map<String, String> cookies = new HashMap<>();
    public Rotasyon_Task( ArrayList<String> otobusler, Map<String, String> cookies ){
        this.cookies = cookies;
        this.otobusler = otobusler;
    }
    public void yap(){
        System.out.println( "["+Common.get_current_hmin() + "] Rotasyon download");
        org.jsoup.Connection.Response req = istek_yap("http://filo5.iett.gov.tr/_FYS/000/sorgu.php?konu=mesaj&mtip=PDKS&oto=");
        Document doc = parse_html( req );
        rotasyon_ayikla( doc );
    }
    private void rotasyon_ayikla( Document document ){
        try {
            if( document == null ){}
        } catch( NullPointerException e ){
            yap();
            e.printStackTrace();
        }
    }
}


class Login_Task {
    private Map<String, String> cookies = new HashMap<>();
    public void yap(){
        System.out.println("A Filo Giriş yapılıyor..");
        request("A", "dk_oasa", "oas145");
        System.out.println("B Filo Giriş yapılıyor..");
        request("B", "dk_oasb", "oas125");
        System.out.println("C Filo Giriş yapılıyor..");
        request("C", "dk_oasc", "oas165");

    }

    private void request( String bolge, String ka, String sifre ){
        try{
            org.jsoup.Connection.Response res = Jsoup.connect("http://filo5.iett.gov.tr/login.php?sayfa=/_FYS.php&aday=x")
                    .data("login", ka, "password", sifre )
                    .method(org.jsoup.Connection.Method.POST)
                    .execute();

            this.cookies.put(bolge, res.cookies().get("PHPSESSID") );
        } catch( IOException e ){
            System.out.println(bolge+ " Login hatası, tekrar deneniyor");
            request( bolge, ka, sifre );
        }
    }

    public Map<String, String> get_cookies(){
        return cookies;
    }
}
/**
 * Mesajlardan PDKS seçeneğinden otobüse kart basana sürücü bilgisini aliyoruz.
 * Eğer sürücü varsa DB de kaydi yoksa, filodaki noktaya istek yapip sürücünün
 * detaylarını alıyoru.
 * */
class PDKS_Task extends Filo_Task {
    public PDKS_Task( String oto, String cookie, String aktif_tarih ){
        this.oto = oto;
        this.cookie = cookie;
        this.aktif_tarih = aktif_tarih;
        this.logprefix = "Sürücü PDKS";
    }
    public void yap(){
        if( aktif_tarih.equals("BEKLEMEDE") ) return;
        System.out.println( "["+Common.get_current_hmin() + "]  "+ aktif_tarih  + " " +  "[ " + oto + " PDKS DOWNLOAD ]");
        org.jsoup.Connection.Response pdks_req = istek_yap("http://filo5.iett.gov.tr/_FYS/000/sorgu.php?konu=mesaj&mtip=PDKS&oto=");
        Document pdks_doc = parse_html( pdks_req );
        pdks_ayikla( pdks_doc );
    }
    private void pdks_ayikla( Document document ){
        try {
            if( document == null ){}
        } catch( NullPointerException e ){
            yap();
            e.printStackTrace();
        }

        Elements table = null;
        Elements rows = null;
        Element row = null;
        Elements cols = null;

        String kart_basma_col_text, sicil_no = "", isim = "";
        Surucu_Data surucu;
        int tip = 0;
        boolean kaydet = true;
        // otobus nesnesini her pdks kontrol de yeniden olusturuyoruz
        // guncel aktif plakayi alabilmek için
        Otobus otobus = new Otobus( oto );
        try {
            table = document.select("table");
            rows = table.select("tr");
            for (int i = 2; i < rows.size(); i++) {
                kaydet = true;
                row = rows.get(i);
                cols = row.select("td");
                kart_basma_col_text = cols.get(4).text();

                //System.out.println(kart_basma_col_text);
                try {
                    if (kart_basma_col_text.contains("PDKS_Kart Binen ")) {
                        tip = PDKS_Data.BINDI;
                        sicil_no = Common.regex_trim(cols.get(4).text()).substring(16, 22);
                        isim = Common.regex_trim(kart_basma_col_text.substring(25));
                    } else if ((kart_basma_col_text.contains("PDKS_Kart inen"))) {
                        tip = PDKS_Data.INDI;
                        sicil_no = Common.regex_trim(cols.get(4).text()).substring(15, 21);
                        isim = Common.regex_trim(kart_basma_col_text.substring(24));
                    } else {
                        kaydet = false;
                    }
                    //System.out.println(oto + " PDKS --> [" + tip + " " + sicil_no + "] [" + isim + "]");
                    if (kaydet) {
                        surucu = new Surucu_Data(sicil_no);

                        if (!surucu.kontrol()) {
                            // eger surucu kayitli degilse noktaya istek yapiyoruz
                            //surucu_noktaya_istek();
                            surucu.ekle( sicil_no, isim, "-1");

                        }
                        otobus.pdks_kaydi_ekle(aktif_tarih, rows.size()-i, sicil_no, Common.regex_trim(cols.get(3).getAllElements().get(0).text()), tip);
                    }
                } catch( NullPointerException | IndexOutOfBoundsException e ){
                    e.printStackTrace();
                }
                cols.clear();
            }
            rows.clear();
        } catch( NullPointerException e ){
            System.out.println( "["+Common.get_current_hmin() + "]  "+ aktif_tarih  + " " +  oto + " ORER sürücü PDKS ayıklama hatası. Tekrar deneniyor.");
            e.printStackTrace();
            yap();
        }
    }
    // noktaya istek, surucu isim ve telefon alma
    private void surucu_noktaya_istek(){
        org.jsoup.Connection.Response nokta_req = istek_yap("http://filo5.iett.gov.tr/_FYS/000/uyg.0.2.php?abc=1&talep=5&grup=0&hat=");
        Document nokta_doc;
        try {
            nokta_doc = nokta_parse_html( nokta_req );
            nokta_ayikla( nokta_doc );
        } catch( NullPointerException e ){
            // sürücü bilgisi yok noktada, bir keresinde veritabanı hatası falan vermişti onun için önlem
        }

    }
    private Document nokta_parse_html( org.jsoup.Connection.Response req ){
        Document doc;
        try {
            doc = req.parse();
            if( doc.select("body").text().contains("Database") ){
                System.out.println(  "["+Common.get_current_hmin() + "]  "+ aktif_tarih  + " " +  oto + " Sürücü detay, Veri yok");
                return null;
            } else {
                return doc;
            }
        } catch (IOException | NullPointerException e) {
            System.out.println( "["+Common.get_current_hmin() + "]  "+ aktif_tarih  + " " +  oto + "Surucu detay parse hatası. Tekrar deneniyor.");
            e.printStackTrace();
        }
        return null;
    }
    private void nokta_ayikla( Document document ){
        try {
            if( document == null ){}
        } catch( NullPointerException e ){
            e.printStackTrace();
            yap();
        }
        Elements table_sur = document.select("table");
        String surucu_string = table_sur.select("tr").get(1).getAllElements().get(2).text();
        surucu_string = surucu_string.substring(2);
        String[] surucu_split_data = surucu_string.split(" ");
        String surucu_ad = "";
        for (int j = 1; j < surucu_split_data.length - 1; j++) {
            if( j < surucu_split_data.length - 2 ){
                surucu_ad += surucu_split_data[j] + " ";
            } else {
                surucu_ad += surucu_split_data[j];
            }
        }
        if( !surucu_ad.equals("") && !surucu_ad.equals("-1")){
            Surucu_Data surucu = new Surucu_Data();
            surucu.ekle( Common.regex_trim(surucu_split_data[0]), surucu_ad, surucu_split_data[surucu_split_data.length - 1].substring(1, surucu_split_data[surucu_split_data.length - 1].length() - 1 ) );
            System.out.println(oto + " SÜrücü detay alindi -> [" + surucu_split_data[0] + "] " + surucu_ad );
        }
    }
}

/**
 * Otobüsün orer verisini aldigimiz class
 * Günlük seferlerini içeren liste dönüyoruz.
 * Eğer kaydet bayrağı varsa local db ye kayit yapiyoruz veya guncelliyoruz eski veriyi
 * */
class Orer_Task extends Filo_Task {
    private String aktif_sefer_verisi;
    private ArrayList<JSONObject> seferler;
    private boolean kaydet = false;
    public Orer_Task( String oto, String cookie, String aktif_tarih ){
        this.oto = oto;
        this.cookie = cookie;
        this.aktif_tarih = aktif_tarih;
        this.logprefix = "ORER";
    }
    public void set_kaydet( boolean bayrak ){
        this.kaydet = bayrak;
    }
    public void yap(){
        // veri yokken nullpointer yemeyek diye resetliyoruz başta
        if( aktif_tarih.equals("BEKLEMEDE") ) return;
        seferler = new ArrayList<>();
        aktif_sefer_verisi = "";
        System.out.println("ORER download [ "+ aktif_tarih +" ] [ " + oto + " ]");
        org.jsoup.Connection.Response sefer_verileri_req = istek_yap("http://filo5.iett.gov.tr/_FYS/000/sorgu.php?konum=ana&konu=sefer&otobus=");
        Document sefer_doc = parse_html( sefer_verileri_req );
        sefer_veri_ayikla( sefer_doc );

    }
    public void sefer_veri_ayikla( Document document ){
        try {
            if( document == null ){}
        } catch( NullPointerException e ){
            e.printStackTrace();
            //yap();
        }
        Elements table = null;
        Elements rows = null;
        Element row = null;
        Elements cols = null;

        ArrayList<PDKS_Data> surucu_data = new ArrayList<>();

        Otobus otobus = new Otobus( oto );
        try {
            table = document.select("table");
            rows = table.select("tr");

            if( rows.size() == 0 ){
                Common.orer_degisiklik_log_kaydet(oto, aktif_tarih,"HİÇ VERİ YOK", 0, 0, rows.size() );
                return;
            }

            if( rows.size() == 1  ){
                System.out.println(oto + " ORER Filo Veri Yok");
                return;
            }

            String hat = "", surucu_sicil_no = "", orer, plaka  ="";
            Sefer_Data tek_sefer_data;
            boolean hat_alindi = false;
            boolean surucu_data_alindi = false;
            boolean db_insert = false;
            boolean orer_degismis = false;

            int aktif_versiyon = 1; // default 1. versiyondan basliyoruz
            ArrayList<Integer> sefer_versiyonlari = otobus.sefer_versiyonlari_al( aktif_tarih );
            if( sefer_versiyonlari.size() == 0 ){
                // daha veritabanina orer eklenmemiş aktif versiyon = 1
                System.out.println( "["+Common.get_current_hmin() + "]  "+ aktif_tarih  + " " +  oto + " ORER verisi yok DB de" );
                db_insert = true;
            } else {
                //if( sefer_versiyonlari.size() > 1 ){
                    // birden fazla versiyon varsa, en son versiyonun sefer sayisi ile
                    // gelen veriyi karsilastiriyoruz
                    int son_versiyon = sefer_versiyonlari.size()-1; // arraylist indexi oldugu icin -1 islemi yapiyoruz ( en son versiyon en son indexte )
                    ArrayList<Sefer_Data> son_versiyon_seferler = otobus.seferleri_al( aktif_tarih, sefer_versiyonlari.get(son_versiyon) );
                    if( son_versiyon_seferler.size() == rows.size() - 1 ){

                        // eger veri sayilari tutuyorsa orer saatlerinin uyumuna bakiyoruz
                        for( int i = 1; i < rows.size(); i++ ){
                            row = rows.get(i);
                            orer = Common.regex_trim(row.select("td").get(7).getAllElements().get(2).text());
                            if( !orer.equals(son_versiyon_seferler.get(i-1).get_orer()) ){
                                orer_degismis = true;
                                break;
                            }
                        }
                        // orerlerde farklilik varsa yeni verileri yeni versiyon olarak kaydediyoruz
                        if( orer_degismis ){
                            aktif_versiyon = son_versiyon + 1 + 1;
                            // önceki versiyonu geçersiz yapiyoruz
                            otobus.sefer_versiyonunu_gecersiz_yap( aktif_tarih, son_versiyon + 1 );
                            Common.orer_degisiklik_log_kaydet(oto, aktif_tarih,"ORER DEGISMIS", aktif_versiyon, son_versiyon, rows.size() );
                            System.out.println( "["+Common.get_current_hmin() + "]  "+ aktif_tarih  + " " +  "ORER DOWNLOAD [ " + oto + " ] ORER FARKLILIĞI VAR");
                            // TODO alarm vericez burada ORER degismis
                            db_insert = true;
                        } else {
                            // gelen veri son versiyonla uyumlu, son versiyon geçerli
                            aktif_versiyon = sefer_versiyonlari.get(son_versiyon);
                        }
                    } else {
                        // son versiyonla gelen veri uyumsuz yeni versiyon olusturuyoruz
                        aktif_versiyon = son_versiyon + 1 + 1;
                        // önceki versiyonu geçersiz yapiyoruz
                        otobus.sefer_versiyonunu_gecersiz_yap( aktif_tarih, son_versiyon + 1 );
                        Common.orer_degisiklik_log_kaydet(oto, aktif_tarih,"ROWS SIZE FARKLI", aktif_versiyon, son_versiyon, rows.size() );
                        System.out.println( "["+Common.get_current_hmin() + "]  "+ aktif_tarih  + " " +  "ORER DOWNLOAD [ " + oto + " ] EKSİK VEYA FAZLA SEFER VAR");
                        // TODO alarm vericez burada fazla veya eksik sefer var
                        db_insert = true;
                    }
                //}
            }
            String durak_bilgisi = "YOK";
            for( int i = 1; i < rows.size(); i++ ){
                row = rows.get(i);
                cols = row.select("td");

                orer = Common.regex_trim(cols.get(7).getAllElements().get(2).text());

                Map<String, String> pdks_bilgileri = otobus.sefer_hmin_tara( aktif_tarih, orer );
                surucu_sicil_no = pdks_bilgileri.get("surucu");
                plaka = pdks_bilgileri.get("plaka");
                //System.out.println(oto +" --> " + orer + " --> " + surucu_sicil_no +" " + plaka);



                if( cols.get(12).text().replaceAll("\u00A0", "").equals("A") && cols.get(3).getAllElements().size() > 2 ){
                    durak_bilgisi = cols.get(3).getAllElements().get(2).attr("title").replaceAll("\u00A0", "");
                }
                otobus.aktif_durak_bilgisi_guncelle( durak_bilgisi );

                /*if( !surucu_data_alindi ){
                    surucu_data = otobus.pdks_kaydi_al( aktif_tarih );
                    surucu_data_alindi = true;
                }*/
                //System.out.println(surucu_data);
                /*if( surucu_data.size() == 1 ){
                    // eger kart basan surucu bir taneyse, karti bastigi saatten sonraki seferlerin surucusu o oluyor
                    if( Sefer_Sure.gecmis( orer, surucu_data.get(0).get_kart_okutma_saati() ) ){
                        surucu_sicil_no = surucu_data.get(0).get_surucu();
                    } else {
                        // karti basmadan yaptigi seferler belirsiz surucu
                        surucu_sicil_no = "-1";
                    }
                } else if( surucu_data.size() > 1 ){
                    // birden fazla surucu var
                    // kart basan tum suruculer icin; kart basma orer den once mi sonra mi kontrolu yapiyoruz.
                    //  Eger orer kart basmadan gerideyse bir onceki surucunun sicil_no gecerli oluyor.
                    //  Eger orer kart basmadan ilerideyse loop ta aktif olan surucunun sicil_nosu gecerli oluyor.
                    for( PDKS_Data pdks_data : surucu_data ) if( Sefer_Sure.gecmis( orer, pdks_data.get_kart_okutma_saati() ) ) surucu_sicil_no = pdks_data.get_surucu();
                } else {
                    // kart basan yok belirsiz sürücü
                    surucu_sicil_no = "-1";
                }*/

                if( !hat_alindi ){
                    hat = cols.get(1).text().trim();
                    if( cols.get(1).text().trim().contains("!")  ) hat = cols.get(1).text().trim().substring(1, cols.get(1).text().trim().length() - 1 );
                    if( cols.get(1).text().trim().contains("#") ) hat = cols.get(1).text().trim().substring(1, cols.get(1).text().trim().length() - 1 );
                    if( cols.get(1).text().trim().contains("*") ) hat = cols.get(1).text().trim().substring(1, cols.get(1).text().trim().length() - 1);
                    hat_alindi = true;
                }

                if( cols.get(12).text().replaceAll("\u00A0", "").equals("A") && cols.get(3).getAllElements().size() > 2 ){
                    aktif_sefer_verisi = Common.regex_trim(cols.get(3).getAllElements().get(2).attr("title"));
                }

                tek_sefer_data = new Sefer_Data(
                        Common.regex_trim(cols.get(0).text()),
                        hat,
                        Common.regex_trim(cols.get(2).text()),
                        Common.regex_trim(cols.get(3).getAllElements().get(1).text()),
                        Common.regex_trim(cols.get(4).getAllElements().get(2).text()),
                        "",
                        surucu_sicil_no,
                        "",
                        Common.regex_trim(cols.get(6).text()),
                        orer,
                        "",
                        Common.regex_trim(cols.get(8).text()),
                        Common.regex_trim(cols.get(9).text()),
                        Common.regex_trim(cols.get(10).text()),
                        Common.regex_trim(cols.get(11).text()),
                        Common.regex_trim(cols.get(12).text()),
                        cols.get(13).text().substring(5),
                        plaka,
                        1,
                        aktif_versiyon
                );
                seferler.add(tek_sefer_data.tojson());

                if( db_insert ){
                    otobus.sefer_verisi_ekle( aktif_tarih, tek_sefer_data );
                } else {
                    otobus.sefer_verisi_guncelle( aktif_tarih, tek_sefer_data );
                }
                cols.clear();
            }

            rows.clear();
        } catch( NullPointerException e ){
            e.printStackTrace();
            System.out.println( "["+Common.get_current_hmin() + "]  "+ aktif_tarih  + " " +  oto+ " ORER sefer veri ayıklama hatası. Tekrar deneniyor.");
            //yap();
        }
    }
    public ArrayList<JSONObject> get_seferler(){
        return seferler;
    }
    public String get_aktif_sefer_verisi(){
        return aktif_sefer_verisi;
    }
}

/**
 * Filodan otobüse gönderilen mesajlari aldigimiz class.
 */
class Mesaj_Task extends Filo_Task {
    public Mesaj_Task( String oto, String cookie, String aktif_tarih ){
        this.oto = oto;
        this.cookie = cookie;
        this.aktif_tarih = aktif_tarih;
        this.logprefix = "Mesaj";
    }
    public void yap(){
        if( aktif_tarih.equals("BEKLEMEDE") ) return;
        System.out.println( "["+Common.get_current_hmin() + "]  "+ aktif_tarih  + " " +  "[ "+ oto + " GELEN MESAJ DOWNLOAD ]");
        veri_ayikla( parse_html( istek_yap("http://filo5.iett.gov.tr/_FYS/000/sorgu.php?konum=ana&konu=mesaj&oto=") ), false );

        System.out.println( "["+Common.get_current_hmin() + "]  "+ aktif_tarih  + " " +  "[ "+ oto + " GİDEN MESAJ DOWNLOAD ]");
        veri_ayikla( parse_html( istek_yap("http://filo5.iett.gov.tr/_FYS/000/sorgu.php?konu=mesaj&mtip=gelen&oto=") ), true );
    }

    private void veri_ayikla( Document document, boolean giden ){
        try {

        } catch( NullPointerException e ){
            //yap();
            e.printStackTrace();
        }
        Element row = null;
        Elements cols = null;
        Elements table = null;
        Elements rows = null;
        try {
            table = document.select("table");
            rows = table.select("tr");
            Otobus otobus = new Otobus( oto );
            String mesaj_saat;
            Filo_Mesaj_Data mesaj;
            Map<String, String> mesaj_ekstra_data;
            for (int i = 2; i < rows.size(); i++) {
                row = rows.get(i);
                cols = row.select("td");
                mesaj_saat = Common.regex_trim( cols.get(3).getElementsByTag("small").text() );
                mesaj_ekstra_data = otobus.sefer_hmin_tara( aktif_tarih, mesaj_saat );

                mesaj = new Filo_Mesaj_Data( oto,
                                             mesaj_ekstra_data.get("plaka"),
                                             mesaj_ekstra_data.get("surucu"),
                                             Common.regex_trim(cols.get(2).getElementsByTag("a").text()),
                                             Common.regex_trim(cols.get(4).text()),
                                             mesaj_saat,
                                             aktif_tarih,
                                             0
                );

                if( giden ){
                    mesaj.giden_mesaj_ekle(rows.size()-i);
                } else {
                    mesaj.ekle(rows.size()-i);
                }
            }
        } catch( NullPointerException e ){
            e.printStackTrace();
            System.out.println( "["+Common.get_current_hmin() + "]  "+ aktif_tarih  + " " +  oto + " Mesaj veri ayıklama hatası. Tekrar deneniyor.");
            //yap();
        }
    }
}

class IYS_Task extends Filo_Task {
    private ArrayList<String> otobusler;
    private Map<String, String> cookies;
    public IYS_Task( ArrayList<String> otobusler, Map<String, String> cookies, String aktif_tarih ){
        this.otobusler = otobusler;
        this.logprefix = "IYS Download";
        this.cookies = cookies;
        this.aktif_tarih = aktif_tarih;
    }
    public void yap(){
        if( aktif_tarih.equals("BEKLEMEDE") ) return;
        // bolge bolge yap
        cookie = cookies.get("A");
        System.out.println("A Bölgesi IYS Tarama");
        org.jsoup.Connection.Response req = istek_yap("http://filo5.iett.gov.tr/SYS/_SYS.WS.php?sonuc=html&soru=opIYSAcikDosya&liste=A30,A31,A32,A35,A29");
        Document doc = parse_html( req );
        veri_ayikla( doc );

        cookie = cookies.get("B");
        System.out.println("B Bölgesi IYS Tarama");
        req = istek_yap("http://filo5.iett.gov.tr/SYS/_SYS.WS.php?sonuc=html&soru=opIYSAcikDosya&liste=B30,B31,B32,B35,B36,B42");
        doc = parse_html( req );
        veri_ayikla( doc );

        System.out.println("C Bölgesi IYS Tarama");
        cookie = cookies.get("C");
        req = istek_yap("http://filo5.iett.gov.tr/SYS/_SYS.WS.php?sonuc=html&soru=opIYSAcikDosya&liste=C30,C31,C32,C33,C34,C35,C36");
        doc = parse_html( req );
        veri_ayikla( doc );
    }
    private void veri_ayikla_json( Document document ){

        JSONArray iys_array = new JSONArray(document.body().text());

        try{
            PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter("C://test.json", false)));
            writer.print(document.body().text());
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        JSONObject item;
        for( int j = 0; j < iys_array.length(); j++ ){
            item = iys_array.getJSONObject(j);
            if( otobusler.contains(item.getString("OTO") )){
                System.out.println( item.getString("OTO"));
            }
        }

    }
    private void veri_ayikla( Document document ){
        try {
            if( document == null ){}
        } catch( NullPointerException e ){
            e.printStackTrace();
            yap();
        }
        Elements table = null;
        Elements rows = null;
        Element row = null;
        Elements cols = null;
        try {
            table = document.select("table");
            rows = table.select("tr");
        } catch( NullPointerException e ){
            e.printStackTrace();
            System.out.println( "["+Common.get_current_hmin() + "]  "+ aktif_tarih  +" " + oto + " " +  "IYS Table null exception");
            //yap();
            return;
        }


        String oto;
        ArrayList<String> alarm_verilecekler = new ArrayList<>();
        java.sql.Connection con = null;
        java.sql.Statement  st = null;
        java.sql.PreparedStatement pst = null;
        java.sql.ResultSet  res = null;
        try {
            con = DBC.getInstance().getConnection();
        } catch( SQLException e ){
            e.printStackTrace();
        }
        System.out.println( "IYS Row length: " + rows.size());
        for (int i = 1; i < rows.size(); i++) {
            row = rows.get(i);
            cols = row.select("td");
            try {
                oto = cols.get(1).text().replaceAll("\u00A0", "");
                //System.out.println("["+oto+"]");
                if (otobusler.contains(oto)) {
                    Otobus otobus = new Otobus(oto);
                    Map<String, String> mesaj_ekstra_data = otobus.sefer_hmin_tara(aktif_tarih, Common.get_current_hmin());
                    try {
                        pst = con.prepareStatement("SELECT * FROM " + GitasDBT.IYS_KAYIT + " WHERE oto = ? && kaynak = ? && tarih = ? && ozet = ?");
                        pst.setString(1, oto);
                        pst.setString(2, cols.get(4).text().replaceAll("\u00A0", ""));
                        pst.setString(3, Common.iys_to_date(cols.get(2).text().replaceAll("\u00A0", "")));
                        pst.setString(4, cols.get(5).text().replaceAll("\u00A0", ""));
                        res = pst.executeQuery();
                        if (Common.result_count(res) == 0) {
                            if (!alarm_verilecekler.contains(oto)) alarm_verilecekler.add(oto);
                            pst = con.prepareStatement("INSERT INTO " + GitasDBT.IYS_KAYIT + " ( oto, kaynak, tarih, ozet, tip, goruldu, surucu, plaka ) VALUES ( ?, ?, ?, ?, ?, ?, ?, ? )");
                            pst.setString(1, oto);
                            pst.setString(2, cols.get(4).text().replaceAll("\u00A0", ""));
                            pst.setString(3, Common.iys_to_date(cols.get(2).text().replaceAll("\u00A0", "")));
                            pst.setString(4, cols.get(5).text().replaceAll("\u00A0", ""));
                            pst.setString(5, cols.get(3).text());
                            pst.setInt(6, 0);
                            pst.setString(7, mesaj_ekstra_data.get("surucu"));
                            pst.setString(8, mesaj_ekstra_data.get("plaka"));
                            pst.executeUpdate();
                        }
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
            } catch( NullPointerException | IndexOutOfBoundsException e ){
                e.printStackTrace();
            }
        }

        try {
            if( res != null ) res.close();
            if( pst != null ) pst.close();
            if( con != null ) con.close();
        } catch( SQLException e ){
            e.printStackTrace();
        }


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