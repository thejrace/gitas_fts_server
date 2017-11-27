package sample;

import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
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