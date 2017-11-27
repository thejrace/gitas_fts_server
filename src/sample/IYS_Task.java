package sample;


import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Map;

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