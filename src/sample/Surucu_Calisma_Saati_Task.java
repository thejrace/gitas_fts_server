package sample;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Surucu_Calisma_Saati_Task {

    private String oto, aktif_tarih;
    public Surucu_Calisma_Saati_Task( String _oto, String _aktif_tarih ){
        oto = _oto;
        aktif_tarih = _aktif_tarih;
    }

    /* sürücü calisma saatlerini web_servis api sinden kontrol ediyoruz
     * db query leri icin kodu php de yazdım zaten, java da tekrar yazana kadar localhosta istek yapıp hazir veriyi kullaniyorum */
    public void yap(){
        Map<String, Surucu> suruculer = new HashMap<>();
        Web_Request request = new Web_Request(Web_Request.SERVIS_URL, "&req=pdks_detay&oto="+oto );
        request.kullanici_pc_parametreleri_ekle();
        request.action();
        JSONArray data = new JSONObject(request.get_value()).getJSONObject("data").getJSONArray("pdks_data");
        int size = data.length();
        if( size > 0 ){
            suruculer = new HashMap<>();
            JSONObject surucu_item;
            for( int j = 0; j < size; j++ ){
                surucu_item = data.getJSONObject(j);
                suruculer.put(surucu_item.getString("isim"), new Surucu( surucu_item.getString("sicil_no"), surucu_item.getString("isim"), surucu_item.getString("telefon") ) );
            }
        }

        request = new Web_Request(Web_Request.SERVIS_URL, "&req=orer_surucu_data&oto="+oto+"&baslangic=&bitis=" );
        request.kullanici_pc_parametreleri_ekle();
        request.action();
        JSONArray sdata = new JSONObject(request.get_value()).getJSONObject("data").getJSONArray("orer_data");

        JSONObject item;
        ArrayList<String> kontrol = new ArrayList<>();
        for( int j = 0; j < sdata.length(); j++ ){
            item = sdata.getJSONObject(j);
            if( !item.getString("durum").equals("T") ) continue;
            // try catch BELIRSIZ SURUCU icin
            try {
                suruculer.get(item.getString("surucu")).add_gidis(item.getString("gidis"));
                suruculer.get(item.getString("surucu")).add_bitis(item.getString("bitis"));
            } catch( NullPointerException e ){}
            if( !kontrol.contains( item.getString("surucu"))){
                try {
                    suruculer.get(item.getString("surucu")).set_orer( item.getString("orer") );
                    kontrol.add(item.getString("surucu"));
                } catch( NullPointerException e ){}
            } else {
                if( !item.getString("bitis").equals("") ) suruculer.get(item.getString("surucu")).set_bitis( item.getString("bitis") );
            }
        }
        for (Map.Entry<String, Surucu> entry : suruculer.entrySet()) {
            try {
                if( Sefer_Sure.hesapla_uzun( entry.getValue().get_orer(), entry.getValue().get_bitis() ) / 60 >= 8 ){
                    Alarm_Data.db_insert( new Alarm_Data(Alarm_Data.SURUCU_COK_CALISTI, Alarm_Data.SURUCU_FLIP_FLOP, oto, Alarm_Data.MESAJ_SURUCU_COK_CALISTI.replace("%%ISIM%%", entry.getKey()).replace("%%SAAT%%", String.valueOf(8)), "-1"), oto, aktif_tarih );
                }
            } catch ( NullPointerException e ){ }
        }
    }
}
