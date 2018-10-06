package sample;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.sql.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Otobüsün orer verisini aldigimiz class
 * Günlük seferlerini içeren liste dönüyoruz.
 * Eğer kaydet bayrağı varsa local db ye kayit yapiyoruz veya guncelliyoruz eski veriyi
 * */
class Orer_Task extends Filo_Task {
    private String aktif_sefer_verisi;
    private ArrayList<JSONObject> seferler;
    private boolean kaydet = false;
    private boolean mobil_flag = false;
    private Map<String, Integer> sefer_ozet = new HashMap<>();
    private String ui_main_notf, ui_notf, ui_led, hat;
    public Orer_Task( String oto, String cookie, String aktif_tarih ){
        this.oto = oto;
        this.cookie = cookie;
        this.aktif_tarih = aktif_tarih;
        this.logprefix = "ORER";
    }

    public void set_mobil_flag(){
        mobil_flag = true;
    }


    public void set_kaydet( boolean bayrak ){
        this.kaydet = bayrak;
    }
    public void yap(){
        // veri yokken nullpointer yemeyek diye resetliyoruz başta
        if( aktif_tarih.equals("BEKLEMEDE") ) return;
        seferler = new ArrayList<>();
        aktif_sefer_verisi = "";
        //System.out.println("ORER download [ "+ aktif_tarih +" ] [ " + oto + " ]");
        org.jsoup.Connection.Response sefer_verileri_req = istek_yap("https://filotakip.iett.gov.tr/_FYS/000/sorgu.php?konum=ana&konu=sefer&otobus=");
        //org.jsoup.Connection.Response sefer_verileri_req = istek_yap("http://filo5.iett.gov.tr/_FYS/000/sorgu.php?konum=ana&konu=sefer&otobus=");
        Document sefer_doc = parse_html( sefer_verileri_req );
        if( mobil_flag ){
            sefer_veri_ayikla_mobil_v1(sefer_doc);
        } else {
            sefer_veri_ayikla_v1( sefer_doc );
        }


    }


    private void sefer_veri_ayikla_mobil_v1( Document document ){
        double start = Common.get_unix();
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

        DateFormat dateFormat = new SimpleDateFormat("HH:mm");
        Date date = new Date();
        String SIMDI = dateFormat.format(date);

        try {
            table = document.select("table");
            rows = table.select("tr");
            int rows_size = rows.size();
            if( rows_size == 0 ){
                Common.orer_degisiklik_log_kaydet(oto, aktif_tarih,"HİÇ VERİ YOK", 0, 0, rows.size() );
                return;
            }
            if( rows_size == 1  ){
                System.out.println(oto + " ORER Filo Veri Yok");
                return;
            }

            sefer_ozet.put(Sefer_Data.DTAMAM, 0);
            sefer_ozet.put(Sefer_Data.DAKTIF, 0);
            sefer_ozet.put(Sefer_Data.DBEKLEYEN, 0);
            sefer_ozet.put(Sefer_Data.DIPTAL, 0);
            sefer_ozet.put(Sefer_Data.DYARIM, 0);

            // durum degiskenleri
            boolean tum_seferler_tamam = false,
                    tum_seferler_bekleyen = false;

            JSONArray sefer_data = new JSONArray();
            JSONObject sefer, sonraki_sefer = null;
            boolean hat_alindi = false;
            for( int i = 1; i < rows_size; i++ ) {
                row = rows.get(i);
                cols = row.select("td");
                try {
                    sefer = new JSONObject();
                    sefer.put("no", Common.regex_trim(cols.get(0).text()) );
                    sefer.put("orer", Common.regex_trim(cols.get(9).getAllElements().get(2).text()));
                    sefer.put("amir", Common.regex_trim(cols.get(10).text()) );
                    sefer.put("tahmin", Common.regex_trim(cols.get(12).text()));

                    String d_dk = "";
                    String durum = "";
                    String dk = "";
                    try {
                        d_dk = cols.get(14).text();
                        durum = d_dk.substring(0,1);
                        dk = d_dk.substring(2, d_dk.length());

                    } catch (StringIndexOutOfBoundsException e ){
                        //e.printStackTrace();
                    }
                    sefer.put("durum", durum);
                    sefer.put("durum_kodu", dk);
                    if( durum.equals("A") ){
                        sefer.put("durak", cols.get(15).text());
                    } else {
                        sefer.put("durak", "YOK");
                    }
                    if( !hat_alindi ){
                        hat = cols.get(2).text().trim();
                        if( cols.get(2).text().trim().contains("!")  ){
                            hat = cols.get(2).text().trim().substring(3, cols.get(2).text().trim().length() - 1 );
                        } else if( cols.get(2).text().trim().contains("#") ) {
                            hat = cols.get(2).text().trim().substring(3, cols.get(2).text().trim().length() - 1 );
                        } else  if( cols.get(2).text().trim().contains("*") ){
                            hat = cols.get(2).text().trim().substring(3, cols.get(2).text().trim().length() - 1);
                        } else {
                            hat = hat.substring(2, hat.length());
                        }
                        hat_alindi = true;
                    }
                    sefer.put("hat", hat);

                    sefer_data.put( sefer );
                    if (sefer_ozet.containsKey(durum)) {
                        sefer_ozet.replace(durum, sefer_ozet.get(durum), sefer_ozet.get(durum) + 1);
                    } else {
                        sefer_ozet.put(durum, 1);
                    }
                } catch (JSONException | NullPointerException | IndexOutOfBoundsException e ){
                    e.printStackTrace();
                }
            }

            if (sefer_ozet.get(Sefer_Data.DTAMAM) == sefer_data.length()) tum_seferler_tamam = true;
            if (sefer_ozet.get(Sefer_Data.DBEKLEYEN) == sefer_data.length()) tum_seferler_bekleyen = true;

            if (tum_seferler_tamam) {
                ui_led = Sefer_Data.DTAMAM;
                ui_main_notf = "Tüm Seferler Tamam";
                ui_notf = "Sefer yüzdesi: %100";
                durum_guncelle();
                return;
            } else if (tum_seferler_bekleyen) {
                ui_led = Sefer_Data.DBEKLEYEN;
                ui_main_notf = "Seferini bekliyor.";
                ui_notf = "Bir sonraki sefer: " + sefer_data.getJSONObject(0).getString("orer");
                durum_guncelle();
                return;
            }
            String sefer_no,
                    sefer_orer,
                    sefer_tahmin,
                    sefer_durum,
                    sefer_amir,
                    sefer_durum_kodu;
            for( int j = 0; j < sefer_data.length(); j++ ){
                sefer = sefer_data.getJSONObject(j);
                sefer_no = sefer.getString("no");
                sefer_orer = sefer.getString("orer");
                sefer_tahmin = sefer.getString("tahmin");
                sefer_durum = sefer.getString("durum");
                sefer_durum_kodu = sefer.getString("durum_kodu");
                sefer_amir = sefer.getString("amir");
                sonraki_sefer = null;
                // bir sonraki sefer varsa aliyoruz verisini
                if (!sefer_data.isNull(j + 1)) sonraki_sefer = sefer_data.getJSONObject(j + 1);
                // yarim sefer
                if (sefer_durum.equals(Sefer_Data.DYARIM)) {
                    if (sonraki_sefer != null) {
                        int k = j + 1;
                        int duzeltilmis_sefer_index = 0;
                        boolean sonraki_seferler_duzeltilmis = false;
                        while (!sefer_data.isNull(k)) {
                            JSONObject yarim_sonrasi_sefer = sefer_data.getJSONObject(k);
                            // yarim kalan seferden sonra bekleyen veya tamamlanan sefer yoksa yarim kaldi durumuna getiriyoruz
                            // varsa dokunmuyoruz zaten yukarıda sonraki seferleri geçerken bekleyen veya tamamlandi olarak degisecek durum
                            if (yarim_sonrasi_sefer.getString("durum").equals("B") || yarim_sonrasi_sefer.getString("durum").equals("T")) {
                                duzeltilmis_sefer_index = k;
                                sonraki_seferler_duzeltilmis = true;
                                Alarm_Data.db_insert(new Alarm_Data(Alarm_Data.SEFERLER_DUZELTILDI, Alarm_Data.YESIL, oto, Alarm_Data.MESAJ_SEFERLER_DUZELTILDI, "1"), oto, aktif_tarih);
                                break;
                            }
                            k++;
                        }
                        // seferler duzeltilmemişse yarim kaldi diyoruz
                        // bu muhtemelen son sefer yarim kaldiginda olur cunku genelde yarim kalan sefer sonrasi iptal oluyor sonrakiler
                        if (!sonraki_seferler_duzeltilmis) {
                            Alarm_Data.db_insert(new Alarm_Data(Alarm_Data.SEFER_YARIM, Alarm_Data.KIRMIZI, oto, Alarm_Data.MESAJ_YARIM, sefer_no), oto, aktif_tarih);
                            ui_led = Sefer_Data.DYARIM;
                            ui_main_notf = "Sefer Yarım Kaldı";
                            ui_notf = "Durum Kodu: " + sefer_durum_kodu;
                        } else {
                            // bekleyen sefer ( durum led icin )
                            JSONObject iptal_sonrasi_sefer = sefer_data.getJSONObject(duzeltilmis_sefer_index);
                            if (iptal_sonrasi_sefer.getString("durum").equals(Sefer_Data.DBEKLEYEN)) {
                                ui_led = Sefer_Data.DBEKLEYEN;
                                ui_main_notf = "Seferini Bekliyor";
                                if (!iptal_sonrasi_sefer.getString("amir").equals("") && !iptal_sonrasi_sefer.getString("amir").equals("[ 10 5 2 ]") &&  !iptal_sonrasi_sefer.getString("amir").equals("[10 5 2]")) {
                                    ui_notf = "Bir sonraki sefer " + iptal_sonrasi_sefer.getString("amir") + " ( Amir )";
                                } else {
                                    ui_notf = "Bir sonraki sefer " + iptal_sonrasi_sefer.getString("orer");
                                }
                            }
                        }
                    } else {
                        // son sefer yarım kalmis
                        Alarm_Data.db_insert(new Alarm_Data(Alarm_Data.SEFER_YARIM, Alarm_Data.KIRMIZI, oto, Alarm_Data.MESAJ_YARIM, sefer_no), oto, aktif_tarih);
                        ui_led = Sefer_Data.DYARIM;
                        ui_main_notf = "Sefer Yarım Kaldı";
                        ui_notf = "Durum Kodu: " + sefer_durum_kodu;
                    }
                }
                // sefer iptalse
                if (sefer_durum.equals(Sefer_Data.DIPTAL)) {
                    if (sonraki_sefer != null) {
                        int k = j + 1;
                        boolean sonraki_seferler_duzeltilmis = false;
                        int duzeltilmis_sefer_index = 0;
                        while (!sefer_data.isNull(k)) {
                            JSONObject yarim_sonrasi_sefer = sefer_data.getJSONObject(k);
                            // iptal seferden sonra bekleyen veya tamamlanan sefer yoksa yarim kaldi durumuna getiriyoruz
                            // varsa dokunmuyoruz zaten yukarıda sonraki seferleri geçerken bekleyen veya tamamlandi olarak degisecek durum
                            if (yarim_sonrasi_sefer.getString("durum").equals("B") || yarim_sonrasi_sefer.getString("durum").equals("T")) {
                                duzeltilmis_sefer_index = k;
                                sonraki_seferler_duzeltilmis = true;
                                break;
                            }
                            k++;
                        }
                        // seferler duzeltilmemişse durum iptal diyoruz
                        if (!sonraki_seferler_duzeltilmis) {

                            Alarm_Data.db_insert(new Alarm_Data(Alarm_Data.SEFER_IPTAL, Alarm_Data.KIRMIZI, oto, Alarm_Data.MESAJ_IPTAL, "1"), oto, aktif_tarih);
                            ui_led = Sefer_Data.DIPTAL;
                            ui_main_notf = "Sefer İptal";
                            ui_notf = "Durum Kodu: " + sefer_durum_kodu;
                        } else {
                            // bekleyen sefer ( durum led icin )
                            Alarm_Data.db_insert(new Alarm_Data(Alarm_Data.SEFER_IPTAL, Alarm_Data.KIRMIZI, oto, Alarm_Data.MESAJ_IPTAL, "1"), oto, aktif_tarih);
                            Alarm_Data.db_insert(new Alarm_Data(Alarm_Data.SEFERLER_DUZELTILDI, Alarm_Data.YESIL, oto, Alarm_Data.MESAJ_SEFERLER_DUZELTILDI, "1"), oto, aktif_tarih);
                            JSONObject iptal_sonrasi_sefer = sefer_data.getJSONObject(duzeltilmis_sefer_index);
                            if (iptal_sonrasi_sefer.getString("durum").equals(Sefer_Data.DBEKLEYEN)) {
                                ui_led = Sefer_Data.DBEKLEYEN;
                                ui_main_notf = "Seferini Bekliyor";
                                if (!iptal_sonrasi_sefer.getString("amir").equals("") && !iptal_sonrasi_sefer.getString("amir").equals("[ 10 5 2 ]") && !iptal_sonrasi_sefer.getString("amir").equals("[10 5 2]")) {
                                    ui_notf = "Bir sonraki sefer " + iptal_sonrasi_sefer.getString("amir") + " ( Amir )";
                                } else {
                                    ui_notf = "Bir sonraki sefer " + iptal_sonrasi_sefer.getString("orer");
                                }
                            }

                        }
                    } else {
                        // son sefer iptal
                        ui_led = Sefer_Data.DIPTAL;
                        Alarm_Data.db_insert(new Alarm_Data(Alarm_Data.SEFER_IPTAL, Alarm_Data.KIRMIZI, oto, Alarm_Data.MESAJ_IPTAL, "1"), oto, aktif_tarih);
                        ui_main_notf = "Sefer İptal";
                        ui_notf = "Durum Kodu: " + sefer_durum_kodu;
                    }
                }
                // aktif sefer
                if (sefer_durum.equals(Sefer_Data.DAKTIF)) {
                    // ui durumlari
                    ui_led = Sefer_Data.DAKTIF;
                    if (sefer_tahmin.equals("")) {
                        ui_main_notf = "Aktif Sefer " + sefer_orer;
                    } else {
                        ui_main_notf = "Aktif Sefer " + sefer_orer + " (T " + sefer_tahmin + ")";
                    }
                    if (!sefer.getString("durak").equals("YOK")) {
                        try {
                            ui_notf = sefer.getString("durak").substring(0, sefer.getString("durak").indexOf(" ("));
                        } catch( StringIndexOutOfBoundsException e ){
                            ui_notf = "Durak bilgisi yok.";
                        }
                    } else {
                        ui_notf = "Durak bilgisi yok.";
                    }
                    if (sonraki_sefer != null && !sefer_tahmin.equals("")) {
                        // gec kalip kalmama kontrolu
                        if (!sonraki_sefer.getString("amir").equals("") && !sonraki_sefer.getString("amir").equals("[ 10 5 2 ]") && !sonraki_sefer.getString("amir").equals("[10 5 2]")) {
                            // bir sonraki sefere amir saat atamis
                            if (Sefer_Sure.hesapla(sefer_tahmin, sonraki_sefer.getString("amir")) < 0) {
                                // amir saat atamis ama gene de geç kalacak
                                Alarm_Data.db_insert(new Alarm_Data(Alarm_Data.GEC_KALMA, Alarm_Data.TURUNCU, oto, Alarm_Data.MESAJ_GEC_KALMA, sefer_no), oto, aktif_tarih);
                            }
                        } else {
                            // amir saat atamamis
                            if (Sefer_Sure.hesapla(sefer_tahmin, sonraki_sefer.getString("orer")) < 0) {
                                // amir saat atamis ama gene de geç kalacak
                                Alarm_Data.db_insert(new Alarm_Data(Alarm_Data.GEC_KALMA, Alarm_Data.TURUNCU, oto, Alarm_Data.MESAJ_GEC_KALMA, sefer_no), oto, aktif_tarih);
                            }
                        }
                    }
                }
                // tamamlanmis sefer
                if (sefer_durum.equals(Sefer_Data.DTAMAM)) {
                    if (sonraki_sefer != null) {
                        if (sonraki_sefer.getString("durum").equals(Sefer_Data.DBEKLEYEN)) {
                            // sonraki seferi var ve durumu bekleyense, bekleyen seferin saatini aliyoruz
                            ui_led = Sefer_Data.DBEKLEYEN;
                            ui_main_notf = "Seferini Bekliyor";
                            if (!sonraki_sefer.getString("amir").equals("") && !sonraki_sefer.getString("amir").equals("[ 10 5 2 ]") && !sonraki_sefer.getString("amir").equals("[10 5 2]")) {
                                // eger amir saat atamişsa
                                ui_notf = "Bir sonraki sefer: " + sonraki_sefer.getString("amir") + " ( Amir )";
                                Alarm_Data.db_insert(new Alarm_Data(Alarm_Data.AMIR_SAAT_ATADI, Alarm_Data.MAVI, oto, Alarm_Data.MESAJ_AMIR_SAAT_ATADI, sefer_no), oto, aktif_tarih);
                            } else {
                                // amirsiz bir sonraki sefer
                                ui_notf = "Bir sonraki sefer " + sonraki_sefer.getString("orer");
                            }
                        }
                    } else {
                        // bir sonraki seferi yoksa tamamlamis demektir
                        // bunun 'tum_seferler_tamam' dan farki yukaridaki tamamladi kontrolu
                        // tum seferlerin T olmasina bakiyor. burada ise yapacak seferi kalmamis anlamina geliyor.
                        // yani arada iptal, yarim sefer olabilir
                        ui_main_notf = "Günü Tamamladı";
                        ui_notf = "Sefer yüzdesi: %" + (sefer_ozet.get("T") * 100) / sefer_data.length();
                        ui_led = Sefer_Data.DTAMAM;
                    }
                }
                if (sefer_durum.equals(Sefer_Data.DBEKLEYEN)) {
                    if (!sefer_amir.equals("") && !sefer_amir.equals("[ 10 5 2 ]") && !sefer_amir.equals("[10 5 2]") ) {
                        if (Sefer_Sure.gecmis(SIMDI, sefer_amir)) {
                            Alarm_Data.db_insert(new Alarm_Data(Alarm_Data.SEFER_BASLAMADI, Alarm_Data.TURUNCU, oto, Alarm_Data.MESAJ_SEFER_BASLAMADI, sefer_no), oto, aktif_tarih );
                        }
                    } else {
                        if (Sefer_Sure.gecmis(SIMDI, sefer_orer)) {
                            Alarm_Data.db_insert(new Alarm_Data(Alarm_Data.SEFER_BASLAMADI, Alarm_Data.TURUNCU, oto, Alarm_Data.MESAJ_SEFER_BASLAMADI, sefer_no), oto, aktif_tarih);
                        }
                    }
                }
            }
            durum_guncelle();
            System.out.println("OADD Aksiyon [ "+ aktif_tarih +" ] [ " + oto + " ] [ OK ( "+( Common.get_unix()-start)+" sn)  ]");
            rows.clear();
        } catch( NullPointerException e ){
            e.printStackTrace();
            System.out.println( "["+Common.get_current_hmin() + "]  "+ aktif_tarih  + " " +  oto+ " ORER MOBİL sefer veri ayıklama hatası. Tekrar deneniyor.");
            //yap();
        }


    }

    private void sefer_veri_ayikla_mobil( Document document ){
        double start = Common.get_unix();
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

        DateFormat dateFormat = new SimpleDateFormat("HH:mm");
        Date date = new Date();
        String SIMDI = dateFormat.format(date);

        try {
            table = document.select("table");
            rows = table.select("tr");
            int rows_size = rows.size();
            if( rows_size == 0 ){
                Common.orer_degisiklik_log_kaydet(oto, aktif_tarih,"HİÇ VERİ YOK", 0, 0, rows.size() );
                return;
            }
            if( rows_size == 1  ){
                System.out.println(oto + " ORER Filo Veri Yok");
                return;
            }

            sefer_ozet.put(Sefer_Data.DTAMAM, 0);
            sefer_ozet.put(Sefer_Data.DAKTIF, 0);
            sefer_ozet.put(Sefer_Data.DBEKLEYEN, 0);
            sefer_ozet.put(Sefer_Data.DIPTAL, 0);
            sefer_ozet.put(Sefer_Data.DYARIM, 0);

            // durum degiskenleri
            boolean tum_seferler_tamam = false,
                    tum_seferler_bekleyen = false;

            JSONArray sefer_data = new JSONArray();
            JSONObject sefer, sonraki_sefer = null;
            boolean hat_alindi = false;
            for( int i = 1; i < rows_size; i++ ) {
                row = rows.get(i);
                cols = row.select("td");
                try {
                    sefer = new JSONObject();
                    sefer.put("no", Common.regex_trim(cols.get(0).text()));
                    sefer.put("orer", Common.regex_trim(cols.get(9).getAllElements().get(2).text()));
                    sefer.put("amir", Common.regex_trim(cols.get(10).text()));
                    sefer.put("tahmin", Common.regex_trim(cols.get(12).text()));

                    String d_dk;
                    String durum = "";
                    String dk = "";
                    try {
                        d_dk = cols.get(14).text();
                        durum = d_dk.substring(0,1);
                        dk = d_dk.substring(2, d_dk.length());

                    } catch (StringIndexOutOfBoundsException e ){
                        //e.printStackTrace();
                    }
                    sefer.put("durum", durum);
                    sefer.put("durum_kodu", dk);

                    if( durum.equals("A") ){
                        sefer.put("durak",  cols.get(15).text().substring(0, cols.get(15).text().indexOf("(")));
                    } else {
                        sefer.put("durak", "YOK");
                    }

                    if( !hat_alindi ){
                        hat = cols.get(2).text().trim();
                        if( cols.get(2).text().trim().contains("!")  ){
                            hat = cols.get(2).text().trim().substring(3, cols.get(2).text().trim().length() - 1 );
                        } else if( cols.get(2).text().trim().contains("#") ) {
                            hat = cols.get(2).text().trim().substring(3, cols.get(2).text().trim().length() - 1 );
                        } else  if( cols.get(2).text().trim().contains("*") ){
                            hat = cols.get(2).text().trim().substring(3, cols.get(2).text().trim().length() - 1);
                        } else {
                            hat = hat.substring(2, hat.length());
                        }
                        hat_alindi = true;
                    }
                    sefer.put("hat", hat);

                    sefer_data.put( sefer );
                    if (sefer_ozet.containsKey(durum)) {
                        sefer_ozet.replace(durum, sefer_ozet.get(durum), sefer_ozet.get(durum) + 1);
                    } else {
                        sefer_ozet.put(durum, 1);
                    }
                } catch (JSONException | NullPointerException | IndexOutOfBoundsException e ){
                    e.printStackTrace();
                }
            }

            if (sefer_ozet.get(Sefer_Data.DTAMAM) == sefer_data.length()) tum_seferler_tamam = true;
            if (sefer_ozet.get(Sefer_Data.DBEKLEYEN) == sefer_data.length()) tum_seferler_bekleyen = true;

            if (tum_seferler_tamam) {
                ui_led = Sefer_Data.DTAMAM;
                ui_main_notf = "Tüm Seferler Tamam";
                ui_notf = "Sefer yüzdesi: %100";
                durum_guncelle();
                return;
            } else if (tum_seferler_bekleyen) {
                ui_led = Sefer_Data.DBEKLEYEN;
                ui_main_notf = "Seferini bekliyor.";
                ui_notf = "Bir sonraki sefer: " + sefer_data.getJSONObject(0).getString("orer");
                durum_guncelle();
                return;
            }
            String sefer_no,
                    sefer_orer,
                    sefer_tahmin,
                    sefer_durum,
                    sefer_amir,
                    sefer_durum_kodu;
            for( int j = 0; j < sefer_data.length(); j++ ){
                sefer = sefer_data.getJSONObject(j);
                sefer_no = sefer.getString("no");
                sefer_orer = sefer.getString("orer");
                sefer_tahmin = sefer.getString("tahmin");
                sefer_durum = sefer.getString("durum");
                sefer_durum_kodu = sefer.getString("durum_kodu");
                sefer_amir = sefer.getString("amir");
                sonraki_sefer = null;
                // bir sonraki sefer varsa aliyoruz verisini
                if (!sefer_data.isNull(j + 1)) sonraki_sefer = sefer_data.getJSONObject(j + 1);
                // yarim sefer
                if (sefer_durum.equals(Sefer_Data.DYARIM)) {
                    if (sonraki_sefer != null) {
                        int k = j + 1;
                        int duzeltilmis_sefer_index = 0;
                        boolean sonraki_seferler_duzeltilmis = false;
                        while (!sefer_data.isNull(k)) {
                            JSONObject yarim_sonrasi_sefer = sefer_data.getJSONObject(k);
                            // yarim kalan seferden sonra bekleyen veya tamamlanan sefer yoksa yarim kaldi durumuna getiriyoruz
                            // varsa dokunmuyoruz zaten yukarıda sonraki seferleri geçerken bekleyen veya tamamlandi olarak degisecek durum
                            if (yarim_sonrasi_sefer.getString("durum").equals("B") || yarim_sonrasi_sefer.getString("durum").equals("T")) {
                                duzeltilmis_sefer_index = k;
                                sonraki_seferler_duzeltilmis = true;
                                Alarm_Data.db_insert(new Alarm_Data(Alarm_Data.SEFERLER_DUZELTILDI, Alarm_Data.YESIL, oto, Alarm_Data.MESAJ_SEFERLER_DUZELTILDI, "1"), oto, aktif_tarih);
                                break;
                            }
                            k++;
                        }
                        // seferler duzeltilmemişse yarim kaldi diyoruz
                        // bu muhtemelen son sefer yarim kaldiginda olur cunku genelde yarim kalan sefer sonrasi iptal oluyor sonrakiler
                        if (!sonraki_seferler_duzeltilmis) {
                            Alarm_Data.db_insert(new Alarm_Data(Alarm_Data.SEFER_YARIM, Alarm_Data.KIRMIZI, oto, Alarm_Data.MESAJ_YARIM, sefer_no), oto, aktif_tarih);
                            ui_led = Sefer_Data.DYARIM;
                            ui_main_notf = "Sefer Yarım Kaldı";
                            ui_notf = "Durum Kodu: " + sefer_durum_kodu;
                        } else {
                            // bekleyen sefer ( durum led icin )
                            JSONObject iptal_sonrasi_sefer = sefer_data.getJSONObject(duzeltilmis_sefer_index);
                            if (iptal_sonrasi_sefer.getString("durum").equals(Sefer_Data.DBEKLEYEN)) {
                                ui_led = Sefer_Data.DBEKLEYEN;
                                ui_main_notf = "Seferini Bekliyor";
                                if (!iptal_sonrasi_sefer.getString("amir").equals("") && !iptal_sonrasi_sefer.getString("amir").equals("[ 10 5 2 ]")) {
                                    ui_notf = "Bir sonraki sefer " + iptal_sonrasi_sefer.getString("amir") + " ( Amir )";
                                } else {
                                    ui_notf = "Bir sonraki sefer " + iptal_sonrasi_sefer.getString("orer");
                                }
                            }
                        }
                    } else {
                        // son sefer yarım kalmis
                        Alarm_Data.db_insert(new Alarm_Data(Alarm_Data.SEFER_YARIM, Alarm_Data.KIRMIZI, oto, Alarm_Data.MESAJ_YARIM, sefer_no), oto, aktif_tarih);
                        ui_led = Sefer_Data.DYARIM;
                        ui_main_notf = "Sefer Yarım Kaldı";
                        ui_notf = "Durum Kodu: " + sefer_durum_kodu;
                    }
                }
                // sefer iptalse
                if (sefer_durum.equals(Sefer_Data.DIPTAL)) {
                    if (sonraki_sefer != null) {
                        int k = j + 1;
                        boolean sonraki_seferler_duzeltilmis = false;
                        int duzeltilmis_sefer_index = 0;
                        while (!sefer_data.isNull(k)) {
                            JSONObject yarim_sonrasi_sefer = sefer_data.getJSONObject(k);
                            // iptal seferden sonra bekleyen veya tamamlanan sefer yoksa yarim kaldi durumuna getiriyoruz
                            // varsa dokunmuyoruz zaten yukarıda sonraki seferleri geçerken bekleyen veya tamamlandi olarak degisecek durum
                            if (yarim_sonrasi_sefer.getString("durum").equals("B") || yarim_sonrasi_sefer.getString("durum").equals("T")) {
                                duzeltilmis_sefer_index = k;
                                sonraki_seferler_duzeltilmis = true;
                                break;
                            }
                            k++;
                        }
                        // seferler duzeltilmemişse durum iptal diyoruz
                        if (!sonraki_seferler_duzeltilmis) {

                            Alarm_Data.db_insert(new Alarm_Data(Alarm_Data.SEFER_IPTAL, Alarm_Data.KIRMIZI, oto, Alarm_Data.MESAJ_IPTAL, "1"), oto, aktif_tarih);
                            ui_led = Sefer_Data.DIPTAL;
                            ui_main_notf = "Sefer İptal";
                            ui_notf = "Durum Kodu: " + sefer_durum_kodu;
                        } else {
                            // bekleyen sefer ( durum led icin )
                            Alarm_Data.db_insert(new Alarm_Data(Alarm_Data.SEFER_IPTAL, Alarm_Data.KIRMIZI, oto, Alarm_Data.MESAJ_IPTAL, "1"), oto, aktif_tarih);
                            Alarm_Data.db_insert(new Alarm_Data(Alarm_Data.SEFERLER_DUZELTILDI, Alarm_Data.YESIL, oto, Alarm_Data.MESAJ_SEFERLER_DUZELTILDI, "1"), oto, aktif_tarih);
                            JSONObject iptal_sonrasi_sefer = sefer_data.getJSONObject(duzeltilmis_sefer_index);
                            if (iptal_sonrasi_sefer.getString("durum").equals(Sefer_Data.DBEKLEYEN)) {
                                ui_led = Sefer_Data.DBEKLEYEN;
                                ui_main_notf = "Seferini Bekliyor";
                                if (!iptal_sonrasi_sefer.getString("amir").equals("") && !iptal_sonrasi_sefer.getString("amir").equals("[ 10 5 2 ]")) {
                                    ui_notf = "Bir sonraki sefer " + iptal_sonrasi_sefer.getString("amir") + " ( Amir )";
                                } else {
                                    ui_notf = "Bir sonraki sefer " + iptal_sonrasi_sefer.getString("orer");
                                }
                            }

                        }
                    } else {
                        // son sefer iptal
                        ui_led = Sefer_Data.DIPTAL;
                        Alarm_Data.db_insert(new Alarm_Data(Alarm_Data.SEFER_IPTAL, Alarm_Data.KIRMIZI, oto, Alarm_Data.MESAJ_IPTAL, "1"), oto, aktif_tarih);
                        ui_main_notf = "Sefer İptal";
                        ui_notf = "Durum Kodu: " + sefer_durum_kodu;
                    }
                }
                // aktif sefer
                if (sefer_durum.equals(Sefer_Data.DAKTIF)) {
                    // ui durumlari
                    ui_led = Sefer_Data.DAKTIF;
                    if (sefer_tahmin.equals("")) {
                        ui_main_notf = "Aktif Sefer " + sefer_orer;
                    } else {
                        ui_main_notf = "Aktif Sefer " + sefer_orer + " (T " + sefer_tahmin + ")";
                    }
                    if (!sefer.getString("durak").equals("YOK")) {
                        ui_notf = sefer.getString("durak").substring(16, sefer.getString("durak").indexOf(" ("));
                    } else {
                        ui_notf = "Durak bilgisi yok.";
                    }
                    if (sonraki_sefer != null && !sefer_tahmin.equals("")) {
                        // gec kalip kalmama kontrolu
                        if (!sonraki_sefer.getString("amir").equals("") && !sonraki_sefer.getString("amir").equals("[ 10 5 2 ]")) {
                            // bir sonraki sefere amir saat atamis
                            if (Sefer_Sure.hesapla(sefer_tahmin, sonraki_sefer.getString("amir")) < 0) {
                                // amir saat atamis ama gene de geç kalacak
                                Alarm_Data.db_insert(new Alarm_Data(Alarm_Data.GEC_KALMA, Alarm_Data.TURUNCU, oto, Alarm_Data.MESAJ_GEC_KALMA, sefer_no), oto, aktif_tarih);
                            }
                        } else {
                            // amir saat atamamis
                            if (Sefer_Sure.hesapla(sefer_tahmin, sonraki_sefer.getString("orer")) < 0) {
                                // amir saat atamis ama gene de geç kalacak
                                Alarm_Data.db_insert(new Alarm_Data(Alarm_Data.GEC_KALMA, Alarm_Data.TURUNCU, oto, Alarm_Data.MESAJ_GEC_KALMA, sefer_no), oto, aktif_tarih);
                            }
                        }
                    }
                }
                // tamamlanmis sefer
                if (sefer_durum.equals(Sefer_Data.DTAMAM)) {
                    if (sonraki_sefer != null) {
                        if (sonraki_sefer.getString("durum").equals(Sefer_Data.DBEKLEYEN)) {
                            // sonraki seferi var ve durumu bekleyense, bekleyen seferin saatini aliyoruz
                            ui_led = Sefer_Data.DBEKLEYEN;
                            ui_main_notf = "Seferini Bekliyor";
                            if (!sonraki_sefer.getString("amir").equals("") && !sonraki_sefer.getString("amir").equals("[ 10 5 2 ]")) {
                                // eger amir saat atamişsa
                                ui_notf = "Bir sonraki sefer: " + sonraki_sefer.getString("amir") + " ( Amir )";
                                Alarm_Data.db_insert(new Alarm_Data(Alarm_Data.AMIR_SAAT_ATADI, Alarm_Data.MAVI, oto, Alarm_Data.MESAJ_AMIR_SAAT_ATADI, sefer_no), oto, aktif_tarih);
                            } else {
                                // amirsiz bir sonraki sefer
                                ui_notf = "Bir sonraki sefer " + sonraki_sefer.getString("orer");
                            }
                        }
                    } else {
                        // bir sonraki seferi yoksa tamamlamis demektir
                        // bunun 'tum_seferler_tamam' dan farki yukaridaki tamamladi kontrolu
                        // tum seferlerin T olmasina bakiyor. burada ise yapacak seferi kalmamis anlamina geliyor.
                        // yani arada iptal, yarim sefer olabilir
                        ui_main_notf = "Günü Tamamladı";
                        ui_notf = "Sefer yüzdesi: %" + (sefer_ozet.get("T") * 100) / sefer_data.length();
                        ui_led = Sefer_Data.DTAMAM;
                    }
                }
                if (sefer_durum.equals(Sefer_Data.DBEKLEYEN)) {
                    if (!sefer_amir.equals("") && !sefer_amir.equals("[ 10 5 2 ]")) {
                        if (Sefer_Sure.gecmis(SIMDI, sefer_amir)) {
                            Alarm_Data.db_insert(new Alarm_Data(Alarm_Data.SEFER_BASLAMADI, Alarm_Data.TURUNCU, oto, Alarm_Data.MESAJ_SEFER_BASLAMADI, sefer_no), oto, aktif_tarih );
                        }
                    } else {
                        if (Sefer_Sure.gecmis(SIMDI, sefer_orer)) {
                            Alarm_Data.db_insert(new Alarm_Data(Alarm_Data.SEFER_BASLAMADI, Alarm_Data.TURUNCU, oto, Alarm_Data.MESAJ_SEFER_BASLAMADI, sefer_no), oto, aktif_tarih);
                        }
                    }
                }
            }
            durum_guncelle();
            System.out.println("OADD Aksiyon [ "+ aktif_tarih +" ] [ " + oto + " ] [ OK ( "+( Common.get_unix()-start)+" sn)  ]");
            rows.clear();
        } catch( NullPointerException e ){
            e.printStackTrace();
            System.out.println( "["+Common.get_current_hmin() + "]  "+ aktif_tarih  + " " +  oto+ " ORER MOBİL sefer veri ayıklama hatası. Tekrar deneniyor.");
            //yap();
        }


    }

    public void sefer_veri_ayikla_v2( Document document ){
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
            int rows_size = rows.size();

            if( rows_size == 0 ){
                Common.orer_degisiklik_log_kaydet(oto, aktif_tarih,"HİÇ VERİ YOK", 0, 0, rows.size() );
                return;
            }

            if( rows_size == 1  ){
                System.out.println(oto + " ORER Filo Veri Yok");
                return;
            }

            double start = Common.get_unix();

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
                if( son_versiyon_seferler.size() == rows_size - 1 ){
                    // eger veri sayilari tutuyorsa orer saatlerinin uyumuna bakiyoruz
                    for( int i = 1; i < rows_size; i++ ){
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
                        Common.orer_degisiklik_log_kaydet(oto, aktif_tarih,"ORER DEGISMIS", aktif_versiyon, son_versiyon, rows_size );
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
                    Common.orer_degisiklik_log_kaydet(oto, aktif_tarih,"ROWS SIZE FARKLI", aktif_versiyon, son_versiyon, rows_size );
                    System.out.println( "["+Common.get_current_hmin() + "]  "+ aktif_tarih  + " " +  "ORER DOWNLOAD [ " + oto + " ] EKSİK VEYA FAZLA SEFER VAR");
                    // TODO alarm vericez burada fazla veya eksik sefer var
                    db_insert = true;
                }
                //}
            }
            String durak_bilgisi = "YOK";
            for( int i = 1; i < rows_size; i++ ){
                row = rows.get(i);
                cols = row.select("td");

                orer = Common.regex_trim(cols.get(9).getAllElements().get(2).text());

                Map<String, String> pdks_bilgileri = otobus.sefer_hmin_tara( aktif_tarih, orer );
                surucu_sicil_no = pdks_bilgileri.get("surucu");
                plaka = pdks_bilgileri.get("plaka");
                //System.out.println(oto +" --> " + orer + " --> " + surucu_sicil_no +" " + plaka);


                String d_dk = "";
                String durum = "";
                String dk = "";
                try {
                    d_dk = cols.get(14).text();
                    durum = d_dk.substring(0,1);
                    dk = d_dk.substring(2, d_dk.length());

                } catch (StringIndexOutOfBoundsException e ){
                    //e.printStackTrace();
                }

                if( durum.equals("A") ){
                    durak_bilgisi = cols.get(15).text();
                    otobus.aktif_durak_bilgisi_guncelle( durak_bilgisi );
                }


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
                    hat = cols.get(2).text().trim();
                    if( cols.get(2).text().trim().contains("!")  ){
                        hat = cols.get(2).text().trim().substring(3, cols.get(2).text().trim().length() - 1 );
                    } else if( cols.get(2).text().trim().contains("#") ) {
                        hat = cols.get(2).text().trim().substring(3, cols.get(2).text().trim().length() - 1 );
                    } else  if( cols.get(2).text().trim().contains("*") ){
                        hat = cols.get(2).text().trim().substring(3, cols.get(2).text().trim().length() - 1);
                    } else {
                        hat = hat.substring(2, hat.length());
                    }
                    hat_alindi = true;
                }

               /* tek_sefer_data = new Sefer_Data(
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
                );*/

                tek_sefer_data = new Sefer_Data(
                        Common.regex_trim(cols.get(0).text()), // no
                        hat, // hat
                        Common.regex_trim(cols.get(3).text()), // servis
                        Common.regex_trim(cols.get(4).getAllElements().get(1).text()), // guzergah
                        Common.regex_trim(cols.get(5).text()), // oto
                        "",
                        surucu_sicil_no,
                        "",
                        Common.regex_trim(cols.get(8).text()), // gelis
                        orer, // orer
                        "",
                        Common.regex_trim(cols.get(10).text()), // amir
                        Common.regex_trim(cols.get(11).text()), // gidis
                        Common.regex_trim(cols.get(12).text()), // tahmin
                        Common.regex_trim(cols.get(13).text()), // bitis
                        durum,
                        dk,
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
            System.out.println("ORER download [ "+ aktif_tarih +" ] [ " + oto + " ] [ OK ( "+( Common.get_unix()-start)+" sn)  ]");
            rows.clear();
        } catch( NullPointerException e ){
            e.printStackTrace();
            System.out.println( "["+Common.get_current_hmin() + "]  "+ aktif_tarih  + " " +  oto+ " ORER sefer veri ayıklama hatası. Tekrar deneniyor.");
            //yap();
        }
    }

    public void sefer_veri_ayikla_v1( Document document ){
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
            int rows_size = rows.size();

            if( rows_size == 0 ){
                Common.orer_degisiklik_log_kaydet(oto, aktif_tarih,"HİÇ VERİ YOK", 0, 0, rows.size() );
                return;
            }

            if( rows_size == 1  ){
                System.out.println(oto + " ORER Filo Veri Yok");
                return;
            }

            double start = Common.get_unix();

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
                if( son_versiyon_seferler.size() == rows_size - 1 ){
                    // eger veri sayilari tutuyorsa orer saatlerinin uyumuna bakiyoruz
                    for( int i = 1; i < rows_size; i++ ){
                        row = rows.get(i);
                        orer = Common.regex_trim(row.select("td").get(9).getAllElements().get(2).text());
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
                        Common.orer_degisiklik_log_kaydet(oto, aktif_tarih,"ORER DEGISMIS", aktif_versiyon, son_versiyon, rows_size );
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
                    Common.orer_degisiklik_log_kaydet(oto, aktif_tarih,"ROWS SIZE FARKLI", aktif_versiyon, son_versiyon, rows_size );
                    System.out.println( "["+Common.get_current_hmin() + "]  "+ aktif_tarih  + " " +  "ORER DOWNLOAD [ " + oto + " ] EKSİK VEYA FAZLA SEFER VAR");
                    // TODO alarm vericez burada fazla veya eksik sefer var
                    db_insert = true;
                }
                //}
            }
            String durak_bilgisi = "YOK";
            for( int i = 1; i < rows_size; i++ ){
                row = rows.get(i);
                cols = row.select("td");

                orer = Common.regex_trim(cols.get(9).getAllElements().get(2).text());

                Map<String, String> pdks_bilgileri = otobus.sefer_hmin_tara( aktif_tarih, orer );
                surucu_sicil_no = pdks_bilgileri.get("surucu");
                plaka = pdks_bilgileri.get("plaka");
                //System.out.println(oto +" --> " + orer + " --> " + surucu_sicil_no +" " + plaka);

                /*if( cols.get(12).text().replaceAll("\u00A0", "").equals("A") && cols.get(3).getAllElements().size() > 2 ) {
                    durak_bilgisi = cols.get(3).getAllElements().get(2).attr("title").replaceAll("\u00A0", "");
                    otobus.aktif_durak_bilgisi_guncelle( durak_bilgisi );
                }*/



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
                    hat = cols.get(2).text().trim();
                    if( cols.get(2).text().trim().contains("!")  ){
                        hat = cols.get(2).text().trim().substring(3, cols.get(2).text().trim().length() - 1 );
                    } else if( cols.get(2).text().trim().contains("#") ) {
                        hat = cols.get(2).text().trim().substring(3, cols.get(2).text().trim().length() - 1 );
                    } else  if( cols.get(2).text().trim().contains("*") ){
                        hat = cols.get(2).text().trim().substring(3, cols.get(2).text().trim().length() - 1);
                    } else {
                        hat = hat.substring(2, hat.length());
                    }
                    hat_alindi = true;
                }

                /*tek_sefer_data = new Sefer_Data(
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
                );*/

                String d_dk = "";
                String durum = "";
                String dk = "";
                try {
                    d_dk = cols.get(14).text();
                    durum = d_dk.substring(0,1);
                    dk = d_dk.substring(2, d_dk.length());

                } catch (StringIndexOutOfBoundsException e ){
                    //e.printStackTrace();
                }


                tek_sefer_data = new Sefer_Data(
                        Common.regex_trim(cols.get(0).text()), // no
                        hat, // hat
                        Common.regex_trim(cols.get(3).text()), // servis
                        Common.regex_trim(cols.get(4).getAllElements().get(1).text()), // guzergah
                        Common.regex_trim(cols.get(5).text()), // oto
                        "",
                        surucu_sicil_no,
                        "",
                        Common.regex_trim(cols.get(8).text()), // gelis
                        orer, // orer
                        "",
                        Common.regex_trim(cols.get(10).text()), // amir
                        Common.regex_trim(cols.get(11).text()), // gidis
                        Common.regex_trim(cols.get(12).text()), // tahmin
                        Common.regex_trim(cols.get(13).text()), // bitis
                        durum,
                        dk,
                        plaka,
                        1,
                        aktif_versiyon
                );

                /*tek_sefer_data = new Sefer_Data(
                        Common.regex_trim(cols.get(0).text()), // no
                        hat, // hat
                        Common.regex_trim(cols.get(3).text()), // servis
                        Common.regex_trim(cols.get(4).getAllElements().get(1).text()), // guzergah
                        Common.regex_trim(cols.get(5).text()), // oto
                        "",
                        surucu_sicil_no,
                        "",
                        Common.regex_trim(cols.get(8).text()), // gelis
                        orer, // orer
                        "",
                        Common.regex_trim(cols.get(10).text()), // amir
                        Common.regex_trim(cols.get(11).text()), // gidis
                        Common.regex_trim(cols.get(12).text()), // tahmin
                        Common.regex_trim(cols.get(13).text()), // bitis
                        durum,
                        dk,
                        plaka,
                        1,
                        aktif_versiyon
                );*/

                seferler.add(tek_sefer_data.tojson());

                if( db_insert ){
                    otobus.sefer_verisi_ekle( aktif_tarih, tek_sefer_data );
                } else {
                    otobus.sefer_verisi_guncelle( aktif_tarih, tek_sefer_data );
                }
                cols.clear();
            }
            System.out.println("ORER download [ "+ aktif_tarih +" ] [ " + oto + " ] [ OK ( "+( Common.get_unix()-start)+" sn)  ]");
            rows.clear();
        } catch( NullPointerException e ){
            e.printStackTrace();
            System.out.println( "["+Common.get_current_hmin() + "]  "+ aktif_tarih  + " " +  oto+ " ORER sefer veri ayıklama hatası. Tekrar deneniyor.");
            //yap();
        }
    }


    private void durum_guncelle(){
        try {
            Connection con = DBC.getInstance().getConnection();
            PreparedStatement pst = con.prepareStatement("SELECT * FROM " + GitasDBT.OTOBUS_AKTIF_DURUM + " WHERE oto = ?");
            pst.setString(1, oto );
            ResultSet res = pst.executeQuery();
            PreparedStatement pst_insert;
            if( res.next() ){
                // varolan kaydı guncelle
                pst_insert = con.prepareStatement("UPDATE " + GitasDBT.OTOBUS_AKTIF_DURUM + " SET durum = ?, main_notf = ?, notf = ?, tarih = ?, hat = ?, sefer_ozet = ? WHERE oto = ? ");
                pst_insert.setString(1, ui_led);
                pst_insert.setString(2, ui_main_notf);
                pst_insert.setString(3, ui_notf);
                pst_insert.setString(4, Common.get_current_datetime_db());
                pst_insert.setString(5, hat );
                pst_insert.setString(6, String.valueOf(sefer_ozet.get("A"))+"|"+String.valueOf(sefer_ozet.get("T"))+"|"+String.valueOf(sefer_ozet.get("B"))+"|"+String.valueOf(sefer_ozet.get("Y"))+"|"+String.valueOf(sefer_ozet.get("I")));
                pst_insert.setString(7, oto);
                pst_insert.executeUpdate();
            } else {
                // ilk defa kayit yapilacak, ekle
                pst_insert = con.prepareStatement("INSERT INTO " + GitasDBT.OTOBUS_AKTIF_DURUM + "( oto, durum, main_notf, notf, tarih, hat, sefer_ozet ) VALUES ( ?, ?, ?, ?, ?, ?, ?)" );
                pst_insert.setString(1, oto);
                pst_insert.setString(2, ui_led);
                pst_insert.setString(3, ui_main_notf);
                pst_insert.setString(4, ui_notf);
                pst_insert.setString(5, Common.get_current_datetime_db());
                pst_insert.setString(6, hat);
                pst_insert.setString(7, String.valueOf(sefer_ozet.get("A"))+"|"+String.valueOf(sefer_ozet.get("T"))+"|"+String.valueOf(sefer_ozet.get("B"))+"|"+String.valueOf(sefer_ozet.get("Y"))+"|"+String.valueOf(sefer_ozet.get("I")));
                pst_insert.executeUpdate();
            }
            pst_insert.close();
            res.close();
            pst.close();
            con.close();
        } catch( SQLException e ){
            e.printStackTrace();
        }
    }

    public ArrayList<JSONObject> get_seferler(){
        return seferler;
    }
    public String get_aktif_sefer_verisi(){
        return aktif_sefer_verisi;
    }
}