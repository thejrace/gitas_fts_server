package sample;


import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Created by Obarey on 13.02.2017.
 */
public class Filo_Mesaj_Data {

    private String mesaj, kaynak, tarih, oto, plaka, surucu, saat;
    private int hareket_dokumu;

    public Filo_Mesaj_Data( String kaynak, String tarih, String mesaj  ){
        this.mesaj = mesaj;
        this.kaynak = kaynak;
        this.tarih = tarih;
    }

    public Filo_Mesaj_Data( String oto, String plaka, String surucu, String kaynak, String mesaj, String saat, String tarih, int hareket_dokumu ){
        this.oto = oto;
        this.plaka = plaka;
        this.mesaj = mesaj;
        this.kaynak = kaynak;
        this.tarih = tarih;
        this.hareket_dokumu = hareket_dokumu;
        this.surucu = surucu;
        this.saat = saat;
    }

    public void ekle( int no ){
        try {
            Connection con = DBC.getInstance().getConnection();
            PreparedStatement pst = con.prepareStatement("SELECT * FROM " + GitasDBT.FILO_MESAJLAR + " WHERE tarih = ? && oto = ? && no = ?");
            pst.setString(1, tarih );
            pst.setString(2, oto );
            pst.setInt(3, no );
            ResultSet res = pst.executeQuery();
            if( !res.next() ){
                //System.out.println(oto + " MESAJ İLK EKLEME");
                pst = con.prepareStatement("INSERT INTO " + GitasDBT.FILO_MESAJLAR + " ( oto, plaka, surucu, kaynak, mesaj, tarih, saat, hareket_dokumu, no ) VALUES ( ?, ?, ?, ?, ?, ?, ?, ?, ? ) ");
                pst.setString(1, oto );
                pst.setString(2, plaka );
                pst.setString(3, surucu );
                pst.setString(4, kaynak );
                pst.setString(5, mesaj );
                pst.setString(6, tarih );
                pst.setString(7, saat );
                pst.setInt(8, hareket_dokumu );
                pst.setInt(9, no );
                pst.executeUpdate();
            } else {
                //System.out.println(oto + " MESAJ GÜNCELLEME");
                // surucu veya plaka değişmişse kaydi guncelle
                // dusuk ihtimal ama pdks veya plaka degismis ama mesaj ondan once kaydedilmis
                /*System.out.println("MSJ DOWNLOAD: KAPI KODU: " + oto );
                System.out.println("MSJ DOWNLOAD: SÜRÜCÜ: "  + surucu  );
                System.out.println("MSJ DOWNLOAD: "+  res.getString("surucu") );*/
                if( !res.getString("surucu").equals(surucu) || !res.getString("plaka").equals("plaka") ){
                    pst = con.prepareStatement("UPDATE " + GitasDBT.FILO_MESAJLAR + " SET plaka = ?, surucu = ? WHERE id = ?");
                    pst.setString(1, plaka );
                    pst.setString(2, surucu );
                    pst.setInt(3, res.getInt("id"));
                    pst.executeUpdate();
                }
            }
            res.close();
            pst.close();
            con.close();
        } catch( SQLException e ){
            e.printStackTrace();
        }
    }

    public void giden_mesaj_ekle( int no ){
        try {
            Connection con = DBC.getInstance().getConnection();
            PreparedStatement pst = con.prepareStatement("SELECT * FROM " + GitasDBT.FILO_MESAJLAR_GIDEN + " WHERE tarih = ? && oto = ? && no = ?");
            pst.setString(1, tarih );
            pst.setString(2, oto );
            pst.setInt(3, no );
            ResultSet res = pst.executeQuery();
            if( !res.next() ){
                //System.out.println(oto + " GİDEN MESAJ İLK EKLEME");
                pst = con.prepareStatement("INSERT INTO " + GitasDBT.FILO_MESAJLAR_GIDEN + " ( oto, plaka, surucu, kaynak, mesaj, tarih, saat, no ) VALUES ( ?, ?, ?, ?, ?, ?, ?, ? ) ");
                pst.setString(1, oto );
                pst.setString(2, plaka );
                pst.setString(3, surucu );
                pst.setString(4, kaynak );
                pst.setString(5, mesaj );
                pst.setString(6, tarih );
                pst.setString(7, saat );
                pst.setInt(8, no );
                pst.executeUpdate();
            } else {
                //System.out.println(oto + " GİDEN MESAJ GÜNCELLEME");
                // surucu veya plaka değişmişse kaydi guncelle
                // dusuk ihtimal ama pdks veya plaka degismis ama mesaj ondan once kaydedilmis
                /*System.out.println("MSJ DOWNLOAD: KAPI KODU: " + oto );
                System.out.println("MSJ DOWNLOAD: SÜRÜCÜ: "  + surucu  );
                System.out.println("MSJ DOWNLOAD: "+  res.getString("surucu") );*/
                if( !res.getString("surucu").equals(surucu) || !res.getString("plaka").equals("plaka") ){
                    pst = con.prepareStatement("UPDATE " + GitasDBT.FILO_MESAJLAR_GIDEN + " SET plaka = ?, surucu = ? WHERE id = ?");
                    pst.setString(1, plaka );
                    pst.setString(2, surucu );
                    pst.setInt(3, res.getInt("id"));
                    pst.executeUpdate();
                }
            }
            res.close();
            pst.close();
            con.close();
        } catch( SQLException e ){
            e.printStackTrace();
        }
    }

    public String get_kaynak(){
        return kaynak;
    }
    public String get_mesaj(){
        return mesaj;
    }
    public String get_tarih(){
        return tarih;
    }
    public String get_surucu(){ return surucu; }
    public String get_oto(){ return oto; }
    public String get_plaka(){ return plaka; }
    public int get_hareket_dokumu(){ return hareket_dokumu; }

}
