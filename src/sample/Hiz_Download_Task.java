package sample;

import org.jsoup.nodes.Document;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class Hiz_Download_Task extends Filo_Task {
    private int limit;
    public Hiz_Download_Task( String _oto, int _limit ){
        oto = _oto;
        limit = _limit;
    }
    public void yap(){
        // veri yokken nullpointer yemeyek diye resetliyoruz başta
        System.out.println("Hız download [ " + oto + " ]");
        //org.jsoup.Connection.Response sefer_verileri_req = istek_yap("http://filo5.iett.gov.tr/_FYS/000/harita.php?konu=oto&oto=");
        org.jsoup.Connection.Response sefer_verileri_req = istek_yap("https://filotakip.iett.gov.tr/_FYS/000/harita.php?konu=oto&oto=");
        Document sefer_doc = parse_html( sefer_verileri_req );
        sefer_veri_ayikla( sefer_doc );
    }
    public void sefer_veri_ayikla( Document document ){
        try {
            String sayfa = document.toString();
            String data_string = sayfa.substring( sayfa.indexOf("veri_ilklendir")+14, sayfa.indexOf("veri_hatcizgi") );
            String[] exploded = data_string.split("\\|");
            Otobus otobus = new Otobus( oto );
            if( !otobus.is_ok() ) return;
            try {
                int hiz = Integer.valueOf(exploded[4]);
                if( hiz < limit ) return;
                Connection con = DBC.getInstance().getConnection();
                PreparedStatement pst = con.prepareStatement("INSERT INTO " + GitasDBT.FILO_HIZ_KAYITLARI + " ( oto, ruhsat_plaka, aktif_plaka, hiz, lat, lang, tarih ) VALUES (?,?,?,?,?,?,?)");
                pst.setString(1, oto);
                pst.setString(2, otobus.get_ruhsat_plaka());
                pst.setString(3, otobus.get_aktif_plaka());
                pst.setInt(4, hiz );
                pst.setString(5, exploded[2]);
                pst.setString(6, exploded[3]);
                pst.setString(7, Common.get_current_date() + " " + exploded[1] + ":00");
                pst.executeUpdate();
                pst.close();
                con.close();
            } catch( SQLException | ArrayIndexOutOfBoundsException e ){
                e.printStackTrace();
            }
        } catch( Exception e ){
            e.printStackTrace();
            Common.exception_db_kayit("Hiz_Download_task.java", e.getMessage());
            System.out.println( "["+Common.get_current_hmin() + "]  "+  oto+ " Hız sefer veri ayıklama hatası. Tekrar deneniyor.");
        }
    }
}
