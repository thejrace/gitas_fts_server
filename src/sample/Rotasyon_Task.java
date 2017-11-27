package sample;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

class Rotasyon_Task extends Filo_Task{

    private String bolge;
    private ArrayList<String> otobusler;
    private String[] liste = { "A30","A31","A32","A35","A29","B30","B31","B32","B35","B36","B42","C30","C31","C32","C33","C34","C35","C36" };
    private Map<String, Otobus_Rotasyon> rotasyon_liste = new HashMap<>();
    public Rotasyon_Task( ArrayList<String> otobusler ){
        this.otobusler = otobusler;

    }
    public void yap(){
        oto = ""; // Filo_Task url in sonuna ekliyor otoyu o yuzden bosluk yap
        // her kod icin yapicaz
        for( int k = 0; k < liste.length; k++ ){
            System.out.println( "["+Common.get_current_hmin() + "] [ "+liste[k]+" ] Rotasyon download");
            //org.jsoup.Connection.Response req = istek_yap("http://gitasgaz.com/fts_admin/test.php"); // TEST
            org.jsoup.Connection.Response req = istek_yap("http://filo5.iett.gov.tr/_FYS/Kaynak/orer3.php?hat="+liste[k]+"&tarih="+Common.month_int()+"."+Common.year_int());
            Document doc = parse_html( req );
            rotasyon_ayikla( doc );
        }
    }
    private void rotasyon_ayikla( Document document ){
        try {
            Elements table = document.select("table");
            Elements rows = table.select("tr");
            Element row;
            Elements cols;
            String oto, otob;
            // row indexler icin temp array
            Map<String, Integer> temp_index_list = new HashMap<>();
            int rows_size = rows.size();
            // bizim otoları ayıklıyoruz
            // ilk col' a bakarak, basladiklari row un indexini kaydediyoruz
            for (int i = 1; i < rows_size; i++) {
                row = rows.get(i);
                cols = row.select("td");
                oto = cols.get(2).text();
                try {
                    // bolge kodundan sonra 3 hane varsa yakala
                    otob = oto.substring(0,1) + "-" + oto.substring(1, 5);
                    if( otobusler.contains(otob) ){
                        rotasyon_liste.put(otob, new Otobus_Rotasyon());
                        temp_index_list.put(otob, i );
                    }
                } catch( IndexOutOfBoundsException e ){

                }
            }
            int cc,
                month_day_count = Common.month_day_count( Common.month_int(), Common.year_int() ),
                rotasyon_liste_size;
            // otoların indexlerinden rotasyon verilerini alıyoruz
            for (Map.Entry<String, Integer> entry : temp_index_list.entrySet()) {
                cc = 1;
                for( int j = entry.getValue(); j < rows_size; j++ ){
                    row = rows.get(j);
                    cols = row.select("td");
                    rotasyon_liste_size = rotasyon_liste.get(entry.getKey()).get_aylik_plan().size();
                    if( month_day_count == rotasyon_liste_size ){
                        // ay için kontrol bitmiş
                        break;
                    } else {
                        // rotasyon tarama bitmemiş
                        if( j == rows_size - 1 ){
                            // eger tablonun sonuna geldiysek yukarı kırılma olabilir
                            // kontrol edilen aydaki gün sayısı ile kontrol ediyoruz tüm rotasyonu alıp almadığımızı
                            if( month_day_count > rotasyon_liste_size ){
                                // yukarı dogru kırılma yap
                                j = 0; // ilk row a git
                                // j'yi 1 degilde 0 yaptik cunku su an kırılmadan bir önceki loop cycle dayız
                                // yukarıda for loop ile j++ çalışacak ve j=1 olacak
                            }
                        }
                    }
                    System.out.println("[ OTO: " + entry.getKey() + "] [ HAT: " + cols.get(0).text() + " ] [ TABELA: " + cols.get(1).text() + " ] [ GÜN: " + (cc) + " ]");
                    rotasyon_liste.get(entry.getKey()).plan_ekle( new Hat_Tabela_Data( cc, cols.get(0).text(), cols.get(1).text()));
                    // sutun hic degismiyecek hep sağa kayacak
                    cc++;
                }
            }
        } catch( NullPointerException e ){
            System.out.println( "["+Common.get_current_hmin() + "]  "+ aktif_tarih  + " Rotasyon null exception");
            e.printStackTrace();
            yap();
        }
    }
}
