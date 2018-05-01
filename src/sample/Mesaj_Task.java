package sample;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.Map;

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
        System.out.println( "["+Common.get_current_hmin() + "]  "+ aktif_tarih  + " " +  "[ "+ oto + " MESAJ DOWNLOAD ]");
        veri_ayikla( parse_html( istek_yap("http://filo5.iett.gov.tr/_FYS/000/sorgu.php?konum=ana&konu=mesaj&oto=") ), false );

        //System.out.println( "["+Common.get_current_hmin() + "]  "+ aktif_tarih  + " " +  "[ "+ oto + " GİDEN MESAJ DOWNLOAD ]");
        veri_ayikla( parse_html( istek_yap("http://filo5.iett.gov.tr/_FYS/000/sorgu.php?konu=mesaj&mtip=gelen&oto=") ), true );
    }

    private void veri_ayikla(Document document, boolean giden ){
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