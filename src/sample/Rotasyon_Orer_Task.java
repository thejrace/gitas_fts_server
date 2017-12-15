package sample;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;

public class Rotasyon_Orer_Task extends Filo_Task {
    private String hat, tarih;

    private Map<DayOfWeek, ArrayList<ArrayList<String>>> orer_data = new HashMap<>();
    public Rotasyon_Orer_Task( String _hat, String _tarih ){
        hat = _hat;
        tarih = _tarih;
    }

    public Map<DayOfWeek, ArrayList<ArrayList<String>>> get_data(){
        return orer_data;
    }
    public void yap(){
        oto = ""; // Filo_Task url in sonuna ekliyor otoyu o yuzden bosluk yap
        // her kod icin yapicaz
        System.out.println( "["+Common.get_current_hmin() + "] [  ] Rotasyon download");
        //org.jsoup.Connection.Response req = istek_yap("http://gitasgaz.com/fts_admin/test.php"); // TEST
        org.jsoup.Connection.Response req = istek_yap("http://filo5.iett.gov.tr/_FYS/Kaynak/orer2.php?hat="+hat+"&tarih="+tarih); // TEST
        Document doc = parse_html( req );
        rotasyon_ayikla( doc );
    }
    private void rotasyon_ayikla( Document document ){
        try {
            Elements tables = document.select("table");

            /*
            * tables[1] -> haftaici A noktasi
            * tables[2] -> haftaici B
            * tables[3] -> cumartesi A
            * tables[4] -> cumartesi B
            * tables[5] -> pazar A
            * tables[6] -> pazar B
            * */
            ArrayList<Map<Integer, ArrayList<String>>> data = new ArrayList<>();
            ArrayList<String> index_list_temp;
            Element table;
            Elements elements_temp;
            String row_item;
            String orer_temp;
            int oas_tabela_counter;
            for( int k = 1; k < tables.size(); k++ ){
                oas_tabela_counter = 1;
                // her bir tablo için tarama başlat
                table = tables.get(k);
                // ilk row [ OAS, OHO ] falan yazan
                // buradan OAS lar bizi ilgilendiriyor
                elements_temp = table.select("tr").get(1).select("td");
                // orerleri atacagimiz list
                data.add( new HashMap<Integer, ArrayList<String>>() );
                for( int j = 0; j < elements_temp.size(); j++ ){
                    row_item = elements_temp.get(j).text();
                    if( row_item.indexOf("OAŞ") > 0 ) {
                       Elements orer_row = table.select("tr");
                       index_list_temp = new ArrayList<>();
                       for( int l = 2; l < orer_row.size(); l++ ){
                           orer_temp = orer_row.get(l).select("td").get(j).text();
                           if( !orer_temp.equals("") ){
                               try {
                                   index_list_temp.add(orer_temp.substring(0,5));
                               } catch( IndexOutOfBoundsException e){}
                           }
                       }
                       data.get(k-1).put( oas_tabela_counter, index_list_temp );
                       oas_tabela_counter++;
                    }
                }
            }
            ArrayList<ArrayList<String>> hafta_ici = new ArrayList<>();
            ArrayList<ArrayList<String>> cumartesi = new ArrayList<>();
            ArrayList<ArrayList<String>> pazar = new ArrayList<>();
            ArrayList<String> temp_data = new ArrayList<>();
            for( int k = 1; k < data.get(0).size() + 1; k++ ){
                temp_data.addAll(data.get(0).get(k));
                temp_data.addAll(data.get(1).get(k));
                Collections.sort( temp_data, new Orer_Sira_Duzenle());
                hafta_ici.add( temp_data );
                temp_data = new ArrayList<String>();
            }
            for( int k = 1; k < data.get(2).size() + 1; k++ ){
                temp_data.addAll(data.get(2).get(k));
                temp_data.addAll(data.get(3).get(k));
                Collections.sort( temp_data, new Orer_Sira_Duzenle());
                cumartesi.add( temp_data );
                temp_data = new ArrayList<String>();
            }
            for( int k = 1; k < data.get(4).size() + 1; k++ ){
                temp_data.addAll(data.get(4).get(k));
                temp_data.addAll(data.get(5).get(k));
                Collections.sort( temp_data, new Orer_Sira_Duzenle());
                pazar.add( temp_data );
                temp_data = new ArrayList<String>();
            }

            orer_data.put( DayOfWeek.FRIDAY, hafta_ici );
            orer_data.put( DayOfWeek.SATURDAY, cumartesi );
            orer_data.put( DayOfWeek.SUNDAY, pazar );

        } catch( NullPointerException e ){
            System.out.println( "["+Common.get_current_hmin() + "]  "+ aktif_tarih  + " Rotasyon null exception");
            e.printStackTrace();
            yap();
        }
    }


}

class Orer_Sira_Duzenle implements Comparator<String> {
    // Used for sorting in ascending order of
    // roll name
    public int compare(String a, String b) {
        if( Sefer_Sure.gecmis(a, b) ){
            return 1;
        } else {
            return -1;
        }
    }
}