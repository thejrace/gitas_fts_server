package sample;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Obarey on 18.03.2017.
 */
public class Otobus {

    private String kapi_kodu;
    private boolean ok = true;

    private String ruhsat_plaka, aktif_plaka;

    public Otobus( String kapi_kodu ){
        this.kapi_kodu = kapi_kodu;

        try {
            Connection con = DBC.getInstance().getConnection();
            PreparedStatement st = con.prepareStatement("SELECT * FROM " + GitasDBT.OTOBUSLER + " WHERE kod = ?");
            st.setString(1, kapi_kodu );
            ResultSet res = st.executeQuery();
            if( res.next() ){
                ruhsat_plaka = res.getString("ruhsat_plaka");
                aktif_plaka = res.getString("aktif_plaka");
            } else {
                ok = false;
            }
            res.close();
            st.close();
            con.close();
        } catch( SQLException e ){
            e.printStackTrace();
        }
    }

    public void pdks_kaydi_ekle( String tarih, int no, String surucu, String kart_okutma_saati, int tip ){
        // kayit eklenirken aktif plaka degil degisimlerden plaka bilgisini aliyoruz
        Map<String, String> sefer_hmin_tara = sefer_hmin_tara( tarih, kart_okutma_saati );
        int index;
        try {
            Connection con = DBC.getInstance().getConnection();
            PreparedStatement pst;
            pst = con.prepareStatement("SELECT * FROM " + GitasDBT.PDKS_KAYIT + " WHERE oto = ? && tarih = ? && no = ? ");
            pst.setString(1, kapi_kodu );
            pst.setString(2, tarih );
            pst.setInt( 3, no );
            ResultSet res = pst.executeQuery();
            if( !res.next() ){
                pst = con.prepareStatement("INSERT INTO " + GitasDBT.PDKS_KAYIT + " ( oto, surucu, plaka, tarih, kart_okutma_saati, tip, no ) VALUES ( ?, ?, ?, ?, ?, ?, ? )");
                pst.setString(1, kapi_kodu );
                pst.setString(2, surucu );
                pst.setString(3, sefer_hmin_tara.get("plaka") );
                pst.setString(4, tarih );
                pst.setString(5, kart_okutma_saati );
                pst.setInt(6, tip );
                pst.setInt(7, no );
                pst.executeUpdate();
            }
            res.close();
            pst.close();
            con.close();
        } catch(SQLException e ){
            e.printStackTrace();
        }

    }

    public void pdks_kaydi_ekle_eski( String tarih, String surucu, String kart_okutma_saati, int tip ){
        // kayit eklenirken aktif plaka degil degisimlerden plaka bilgisini aliyoruz
        Map<String, String> sefer_hmin_tara = sefer_hmin_tara( tarih, kart_okutma_saati );
        try {
            Connection con = DBC.getInstance().getConnection();
            PreparedStatement pst = null;
            // ayni veriyi tekrar kaydetmemek için kontrol ediyoruz
            pst = con.prepareStatement("SELECT * FROM " + GitasDBT.PDKS_KAYIT + " WHERE oto = ? && surucu = ? && plaka = ? && tarih = ? && kart_okutma_saati = ? && tip = ?");
            pst.setString(1, kapi_kodu );
            pst.setString(2, surucu );
            pst.setString( 3, sefer_hmin_tara.get("plaka") );
            pst.setString( 4, tarih );
            pst.setString(5, kart_okutma_saati );
            pst.setInt(6, tip );
            ResultSet res = pst.executeQuery();
            if( !res.next() ){
                pst = con.prepareStatement("INSERT INTO " + GitasDBT.PDKS_KAYIT + " ( oto, surucu, plaka, tarih, kart_okutma_saati, tip ) VALUES ( ?, ?, ?, ?, ?, ? )");
                pst.setString(1, kapi_kodu );
                pst.setString(2, surucu );
                pst.setString(3, sefer_hmin_tara.get("plaka") );
                pst.setString(4, tarih );
                pst.setString(5, kart_okutma_saati );
                pst.setInt(6, tip );
                pst.executeUpdate();
            }
            res.close();
            pst.close();
            con.close();
        } catch( SQLException e ){
            e.printStackTrace();
        }


    }
    @Deprecated
    public void pdks_kaydi_ekle_eski( String tarih, String surucu, String kart_okutma_saati ){
        boolean insert_bayrak = false;
        ArrayList<PDKS_Data> pdks_kayitlar = pdks_kaydi_al( tarih );
        try {
            Connection con =  DBC.getInstance().getConnection();
            PreparedStatement pst = null;
            if( pdks_kayitlar.size() == 0 ){
                insert_bayrak = true;
            } else {
                for( PDKS_Data pdks_data : pdks_kayitlar ){
                    if( !pdks_data.get_plaka().equals( aktif_plaka ) ){
                        // onceki pdks kaydindaki plaka ile otobusun aktif plakasi ayni degil, araç değişmiş
                        insert_bayrak = true;
                    } else {
                        // ayni veriyi tekrar kaydetmemek için kontrol ediyoruz
                        pst = con.prepareStatement("SELECT * FROM " + GitasDBT.PDKS_KAYIT + " WHERE oto = ? && surucu = ? && plaka = ? && tarih = ? && kart_okutma_saati = ?");
                        pst.setString(1, kapi_kodu );
                        pst.setString(2, surucu );
                        pst.setString( 3, aktif_plaka );
                        pst.setString( 4, tarih );
                        pst.setString(5, kart_okutma_saati );
                        ResultSet res = pst.executeQuery();
                        insert_bayrak = !res.next();
                        res.close();
                    }
                }
            }
            if( insert_bayrak ){
                pst = con.prepareStatement("INSERT INTO " + GitasDBT.PDKS_KAYIT + " ( oto, surucu, plaka, tarih, kart_okutma_saati ) VALUES ( ?, ?, ?, ?, ? )");
                pst.setString(1, kapi_kodu );
                pst.setString(2, surucu );
                pst.setString(3, aktif_plaka );
                pst.setString(4, tarih );
                pst.setString(5, kart_okutma_saati );
                pst.executeUpdate();
            }
            if( pst != null ) pst.close();
            con.close();
        } catch( SQLException e ){
            e.printStackTrace();
        }
    }

    public ArrayList<PDKS_Data> pdks_kaydi_al( String tarih ){
        ArrayList<PDKS_Data> output = new ArrayList<>();
        try {
            Connection con = DBC.getInstance().getConnection();
            // burada kayitlari DESC cagir
            PreparedStatement pst = con.prepareStatement("SELECT * FROM " + GitasDBT.PDKS_KAYIT + " WHERE oto = ? && tarih = ? ORDER BY no");
            pst.setString(1, kapi_kodu );
            pst.setString(2, tarih );
            ResultSet res = pst.executeQuery();
            while( res.next() ){
                output.add( new PDKS_Data( res.getString("oto"), res.getString("tarih"), res.getString("surucu"), res.getString("plaka"), res.getString("kart_okutma_saati"), res.getInt("tip") ));
            }
            res.close();
            pst.close();
            con.close();
        } catch( SQLException e ){
            e.printStackTrace();
        }
        return output;
    }

    public void sefer_versiyonunu_gecersiz_yap( String tarih, int versiyon ){
        try {
            Connection con = DBC.getInstance().getConnection();
            PreparedStatement pst = con.prepareStatement("UPDATE " + GitasDBT.ORER_KAYIT + " SET gecerli = ? WHERE ( oto = ? && tarih = ? && versiyon = ? ) ");
            pst.setInt(1, 0);
            pst.setString(2, kapi_kodu );
            pst.setString(3, tarih );
            pst.setInt(4, versiyon);
            pst.executeUpdate();
            pst.close();
            con.close();
        } catch( SQLException e ){
            e.printStackTrace();
        }
    }

    public ArrayList<Sefer_Data> seferleri_al( String tarih, int versiyon ){
        ArrayList<Sefer_Data> output = new ArrayList<>();
        try {
            Connection con = DBC.getInstance().getConnection();
            PreparedStatement pst = con.prepareStatement("SELECT * FROM " + GitasDBT.ORER_KAYIT + " WHERE oto = ? && tarih = ? && versiyon = ?");
            pst.setString(1, kapi_kodu);
            pst.setString(2, tarih);
            pst.setInt(3, versiyon);
            ResultSet res = pst.executeQuery();
            Surucu_Data surucu;
            while(res.next()){
                surucu = new Surucu_Data( res.getString("surucu" ));
                output.add( new Sefer_Data(
                        res.getString("no"),
                        res.getString("hat"),
                        res.getString("servis"),
                        res.getString("guzergah"),
                        kapi_kodu,
                        surucu.get_isim(),
                        res.getString("surucu"),
                        surucu.get_telefon(),
                        res.getString("gelis"),
                        res.getString("orer"),
                        "",
                        res.getString("amir"),
                        res.getString("gidis"),
                        res.getString("tahmin"),
                        res.getString("bitis"),
                        res.getString("durum"),
                        res.getString("durum_kodu"),
                        res.getString("plaka"),
                        res.getInt("gecerli"),
                        res.getInt("versiyon")
                ));
            }
            res.close();
            pst.close();
            con.close();
        } catch( SQLException e ){
            e.printStackTrace();
        }
        return output;
    }

    public ArrayList<Integer> sefer_versiyonlari_al( String tarih ){
        ArrayList<Integer> output = new ArrayList<>();
        try {
            Connection con = DBC.getInstance().getConnection();
            PreparedStatement pst = con.prepareStatement("SELECT * FROM " + GitasDBT.ORER_KAYIT + " WHERE oto = ? && tarih = ?");
            pst.setString(1, kapi_kodu);
            pst.setString(2, tarih);
            ResultSet res = pst.executeQuery();
            while(res.next()) if(!output.contains(res.getInt("versiyon")) ) output.add( res.getInt("versiyon"));
            res.close();
            pst.close();
            con.close();
        } catch( SQLException e ){
            e.printStackTrace();
        }
        return output;
    }

    // aracin son uc plaka degisimini aliyoruz
    public ArrayList<Map<String, String>> plaka_degisimlerini_al(){
        ArrayList<Map<String, String>> output = new ArrayList<>();
        try {
            Map<String, String> item_data;
            Connection con = DBC.getInstance().getConnection();
            PreparedStatement pst = con.prepareStatement("SELECT * FROM " + GitasDBT.PLAKA_DEGISIMLERI + " WHERE oto = ? ORDER BY id DESC");
            pst.setString(1, kapi_kodu );
            ResultSet res = pst.executeQuery();
            int c = 0;
            while( res.next() ){
                item_data = new HashMap<>();
                item_data.put("plaka", res.getString("aktif_plaka"));
                item_data.put("tarih", res.getString("tarih"));
                output.add(item_data);
                c++;
                if( c == 10 ) break;
            }
            //String degisim_tarih = res.getString("tarih").substring(11, 16);
            res.close();
            pst.close();
            con.close();
        } catch(SQLException e ){
            e.printStackTrace();
        }
        return output;
    }

    public Map<String, String> sefer_hmin_tara( String aktif_tarih, String hmin ){
        Map<String, String> output = new HashMap<>();
        ArrayList<PDKS_Data> pdks_kayitlar = pdks_kaydi_al( aktif_tarih );
        output.put("surucu", "-1");
        output.put("plaka", ruhsat_plaka );

        PDKS_Data pdks_data;
        int son_tip = 0;
        String son_surucu = "";
        ArrayList<Integer> binen_tekrarlar = new ArrayList<>();
        ArrayList<Integer> inen_tekrarlar = new ArrayList<>();
        //System.out.println(kapi_kodu + " PDKS KAYIT SAYISI: " + pdks_kayitlar.size() );
        for( int x = 0; x < pdks_kayitlar.size(); x++ ){
            pdks_data = pdks_kayitlar.get(x);
            if(  son_surucu.equals( pdks_data.get_surucu() ) ){
                // suruculer ayniysa tip kontrolu yapiyoruz
                if( ( son_tip == 0 || son_tip != pdks_data.get_tip() ) ){

                } else {
                    if( pdks_data.get_tip() == PDKS_Data.BINDI ){
                        // bindi tekrari ise bir onceki veri gecerli
                        binen_tekrarlar.add( x );
                    } else {
                        // inislerde en son yaptigi gecerli
                        inen_tekrarlar.add( x - 1 );
                    }
                }
            }
            son_tip = pdks_data.get_tip();
            son_surucu = pdks_data.get_surucu();
        }
        //System.out.println("İNEN TEKRARLAR: " + inen_tekrarlar );
        //System.out.println("BİNEN TEKRARLAR: " + binen_tekrarlar );
        ArrayList<PDKS_Aralik> araliklar = new ArrayList<>();
        PDKS_Aralik aralik = null;
        int c = 0;
        for( PDKS_Data pdks_kayit : pdks_kayitlar ){
            if( binen_tekrarlar.contains(c) || inen_tekrarlar.contains(c) ) continue;
            if( pdks_kayit.get_tip() == PDKS_Data.BINDI ){
                aralik = new PDKS_Aralik( pdks_kayit.get_surucu() );
                aralik.set_binis( pdks_kayit.get_kart_okutma_saati() );
            } else {
                aralik = new PDKS_Aralik( pdks_kayit.get_surucu() );
                aralik.set_inis( pdks_kayit.get_kart_okutma_saati() );
            }
            araliklar.add( aralik );
            c++;
            //System.out.println( kapi_kodu + " ARALIK1 [ " + aralik.get_surucu() + " ] [ " + aralik.get_binis() + " ] [ " + aralik.get_inis() + " ]" );
        }

        ArrayList<PDKS_Aralik> araliklar_final = new ArrayList<>();
        for( int x = 0; x < araliklar.size(); x++ ){
            aralik = araliklar.get(x);
            if( aralik.get_inis().equals("YOK") ){
                try {
                    PDKS_Aralik sonraki_kayit = araliklar.get(x+1);
                    if( sonraki_kayit.get_surucu().equals(aralik.get_surucu()) ){
                        // suruculer ayniysa
                        if( !sonraki_kayit.get_inis().equals("YOK") && sonraki_kayit.get_binis().equals("YOK") ){
                            // aktif kaydin biniş saati var iniş saati yok [ BİNİŞ, ]
                            // bir sonraki kaydin binişi boş, inişi dolu iste iki kaydi birlestiriyoruz [ , İNİŞ ]
                            aralik.set_inis( sonraki_kayit.get_inis() );
                            araliklar_final.add( aralik );
                        } else {
                            // ayni surucu tekrar tekrar biniş basmis
                            aralik.set_inis( sonraki_kayit.get_binis() );
                            araliklar_final.add( aralik );
                        }
                    } else {
                        // suruculer farkli
                        if( aralik.get_binis().equals("YOK") && !sonraki_kayit.get_binis().equals("YOK") ){
                            // kayit1 [ , INIS1 ] -- kayit2 [ BINIS2, ] ---> bir şey yapmiyoruz aralik sayilmiyor burasi
                        } else if( !aralik.get_binis().equals("YOK") && !sonraki_kayit.get_binis().equals("YOK") ){
                            // kayit1 [ BINIS1, ] -- kayit2 [ BINIS2, ] ---> [ BINIS1, BINIS2 ]
                            // ikisini birleştir [ BINIS1, BINIS2 ]
                            aralik.set_inis( sonraki_kayit.get_binis() );
                            araliklar_final.add( aralik );
                        }
                    }
                } catch( IndexOutOfBoundsException e ){
                    // sonraki kayit yok
                    aralik.set_inis("AKTİF");
                    araliklar_final.add( aralik );
                    //e.printStackTrace();
                }
            }
        }
        for( PDKS_Aralik son_aralik : araliklar_final ){
            //System.out.println( kapi_kodu + " ARALIKFINAL [ " + son_aralik.get_surucu() + " ] [ " + son_aralik.get_binis() + " ] [ " + son_aralik.get_inis() + " ]" );
            if( son_aralik.kontrol( hmin ) ){
                output.put("surucu", son_aralik.get_surucu() );
                break;
            }
        }
        // aracin hmin deki plakasini aktif plaka kaydindan degil plaka degisimlerinden aliyorum
        // ornegin; programi aksam acti sabah herhangi bir saatteki hmin ile tarama yaptiginda
        // otobusun aktif plakasini alirsak; oglen aracin plakasi degismisse, degisimden onceki plakanin
        // verileri de aktif plakaya yazilir ve sıçar
        ArrayList<Map<String, String>> plaka_degisimleri = plaka_degisimlerini_al();
        if( plaka_degisimleri.size() == 0 ){
            // eger hiç plaka degisimi yapilmamişsa aktif plakayi aliyoruz
            output.put("plaka", aktif_plaka );
        } else {
            //System.out.println(kapi_kodu + "  PDEGS" +  plaka_degisimleri.toString());
            for( Map<String, String> degisim : plaka_degisimleri ){
                if( Common.dt_gecmis( aktif_tarih + " " + hmin + ":00", degisim.get("tarih") ) ){
                    // degisimler gene DESC geliyor, degisim hminden onceyse onu kabul ediyoruz
                    output.put("plaka", degisim.get("plaka"));
                    break;
                }
            }
        }
        return output;
    }

    @Deprecated
    public Map<String, String> sefer_hmin_tara_v2( String aktif_tarih, String hmin ){
        Map<String, String> output = new HashMap<>();
        ArrayList<PDKS_Data> pdks_kayitlar = pdks_kaydi_al( aktif_tarih );
        output.put("surucu", "-1");
        for( PDKS_Data pdks_kayit : pdks_kayitlar ){
            //if( pdks_kayit.get_tip() != PDKS_Data.BINDI ) continue;
            // kayitlar DESC olarak geliyor
            if(  Sefer_Sure.gecmis( hmin, pdks_kayit.get_kart_okutma_saati() ) ){
                // kart okutma saati hmin den önceyse ve bi tane pdks kaydi varsa o geçerli oluyor
                output.put("surucu", pdks_kayit.get_surucu());
                break;
            }
        }
        // aracin hmin deki plakasini aktif plaka kaydindan degil plaka degisimlerinden aliyorum
        // ornegin; programi aksam acti sabah herhangi bir saatteki hmin ile tarama yaptiginda
        // otobusun aktif plakasini alirsak; oglen aracin plakasi degismisse, degisimden onceki plakanin
        // verileri de aktif plakaya yazilir ve sıçar
        ArrayList<Map<String, String>> plaka_degisimleri = plaka_degisimlerini_al();
        if( plaka_degisimleri.size() == 0 ){
            // eger hiç plaka degisimi yapilmamişsa aktif plakayi aliyoruz
            output.put("plaka", aktif_plaka );
        } else {
            for( Map<String, String> degisim : plaka_degisimleri ){
                if( Common.dt_gecmis( aktif_tarih + " " + hmin + ":00", degisim.get("tarih") ) ){
                    // degisimler gene DESC geliyor, degisim hminden onceyse onu kabul ediyoruz
                    output.put("plaka", degisim.get("plaka"));
                    break;
                }
            }
        }

        return output;
    }

    @Deprecated
    // filo mesajlarini surucu - plaka ile eşleştirme
    public Map<String, String> sefer_hmin_tara_v1(String aktif_tarih, String hmin ){
        Map<String, String> output = new HashMap<>();
        ArrayList<Integer> sefer_versiyonlari = sefer_versiyonlari_al( aktif_tarih );
        for( Sefer_Data sefer :  seferleri_al( aktif_tarih, sefer_versiyonlari.get(sefer_versiyonlari.size()-1) ) ){
            if( sefer.get_durum().equals(Sefer_Data.DTAMAM ) ){
                // eger sefer tamamsa VE  mesaj tarihi orerden önceyse VE mesaj tarihi seferin bitisinden önceyse
                if( Sefer_Sure.gecmis( hmin, sefer.get_orer() ) && Sefer_Sure.gecmis( sefer.get_bitis(), hmin ) ){
                    output.put("surucu", sefer.get_surucu());
                    output.put("plaka", sefer.get_plaka());
                    break;
                } else {
                    // Mesaj tarihi: 11:50 olsun, yukaridaki bloktaki sefer ORER: 10:00 - BİTİŞ: 11:37 olursa mesaj tarihi kosulu saglamiyor
                    // seferin durumu da tamam oldugu icin bulamiyoruz. O yuzden tamam seferler için arada kalan saatlerde surucu kontrolu yapmamiz gerekiyor.
                    // İkinci seferi baska surucu  TODO
                    if( Sefer_Sure.gecmis( sefer.get_orer(), hmin ) ){
                        output.put("surucu", sefer.get_surucu());
                        output.put("plaka", sefer.get_plaka());
                        break;
                    }
                }
            } else {
                // sefer tamam haric diger durumlarda hminden önceki ilk seferin detaylarini al
                if( Sefer_Sure.gecmis(hmin, sefer.get_orer() ) ){
                    output.put("surucu", sefer.get_surucu());
                    output.put("plaka", sefer.get_plaka());
                    break;
                }
            }
        }
        return output;
    }

    public void sefer_verisi_ekle( String tarih, Sefer_Data sefer_data ){
        try {
            Connection con = DBC.getInstance().getConnection();
            PreparedStatement pst = con.prepareStatement("INSERT INTO " + GitasDBT.ORER_KAYIT +
                    " ( no, hat, servis, guzergah, oto, surucu, gelis, orer, amir, gidis, tahmin, bitis, durum, durum_kodu, plaka, tarih, gecerli, versiyon ) VALUES ( ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " );
            pst.setString(1, sefer_data.get_no() );
            pst.setString(2, sefer_data.get_hat() );
            pst.setString(3, sefer_data.get_servis() );
            pst.setString(4, sefer_data.get_guzergah() );
            pst.setString(5, kapi_kodu );
            pst.setString(6, sefer_data.get_surucu_sicil_no() );
            pst.setString(7, sefer_data.get_gelis() );
            pst.setString(8, sefer_data.get_orer() );
            pst.setString(9, sefer_data.get_amir() );
            pst.setString(10, sefer_data.get_gidis() );
            pst.setString(11, sefer_data.get_tahmin() );
            pst.setString(12, sefer_data.get_bitis() );
            pst.setString(13, sefer_data.get_durum() );
            pst.setString(14, sefer_data.get_durum_kodu() );
            pst.setString(15, sefer_data.get_plaka() );
            pst.setString(16, tarih );
            pst.setInt(17, 1 );
            pst.setInt(18, sefer_data.get_versiyon() );
            pst.executeUpdate();
            pst.close();
            con.close();
        } catch( SQLException e ){
            e.printStackTrace();
        }

    }

    public void sefer_verisi_guncelle( String tarih, Sefer_Data sefer_data ){
        try {
            Connection con = DBC.getInstance().getConnection();
            PreparedStatement pst = con.prepareStatement("UPDATE " + GitasDBT.ORER_KAYIT +
                    " SET guzergah = ?, surucu = ?, gelis = ?, orer = ?, amir = ?, gidis = ?, tahmin = ?, durum = ?, durum_kodu = ?, bitis = ? WHERE oto = ? && tarih = ? && no = ? && versiyon = ? && gecerli = ? ");
            pst.setString(1, sefer_data.get_guzergah());
            pst.setString(2, sefer_data.get_surucu_sicil_no());
            pst.setString(3, sefer_data.get_gelis());
            pst.setString(4, sefer_data.get_orer());
            pst.setString(5, sefer_data.get_amir());
            pst.setString(6, sefer_data.get_gidis());
            pst.setString(7, sefer_data.get_tahmin());
            pst.setString(8, sefer_data.get_durum());
            pst.setString(9, sefer_data.get_durum_kodu());
            pst.setString(10, sefer_data.get_bitis());
            pst.setString(11, kapi_kodu );
            pst.setString(12, tarih );
            pst.setString(13, sefer_data.get_no() );
            pst.setInt(14, sefer_data.get_versiyon() );
            pst.setInt(15, 1 );

            pst.executeUpdate();
            pst.close();
            con.close();
        } catch( SQLException e ){
            e.printStackTrace();
        }
    }

    public void aktif_durak_bilgisi_guncelle( String durak ){
        Connection con;
        PreparedStatement pst;
        ResultSet res;
        try {
            con = DBC.getInstance().getConnection();
            pst = con.prepareStatement("SELECT * FROM " + GitasDBT.OTOBUS_DURAK_TAKIP + " WHERE oto = ?");
            pst.setString(1, kapi_kodu );
            res = pst.executeQuery();
            if( res.next() ){
                pst = con.prepareStatement("UPDATE " + GitasDBT.OTOBUS_DURAK_TAKIP + " SET durak = ? WHERE oto = ?");
                pst.setString(1, durak );
                pst.setString(2, kapi_kodu );
                pst.executeUpdate();
            } else {
                pst = con.prepareStatement("INSERT INTO " + GitasDBT.OTOBUS_DURAK_TAKIP + " ( oto, durak ) VALUES ( ?, ? )");
                pst.setString(1, kapi_kodu );
                pst.setString( 2, durak );
                pst.executeUpdate();
            }
            res.close();
            pst.close();
            con.close();
        } catch( SQLException e ){
            e.printStackTrace();
        }



    }

    public boolean not_kontrol(){
        boolean output = false;
        try {
            Connection con = DBC.getInstance().getConnection();
            PreparedStatement st = con.prepareStatement("SELECT * FROM " + GitasDBT.NOTLAR + " WHERE kapi_kodu = ?");
            st.setString(1, kapi_kodu );
            ResultSet res = st.executeQuery();
            output = res.next();
            res.close();
            st.close();
            con.close();
        } catch( SQLException e ){
            e.printStackTrace();
        }
        return output;

    }

    public boolean plaka_guncelle( String _aktif_plaka, String _ruhsat_plaka, String tarih ){
        try {
            Connection con = DBC.getInstance().getConnection();
            PreparedStatement st = con.prepareStatement("UPDATE " + GitasDBT.OTOBUSLER + " SET aktif_plaka = ?, ruhsat_plaka = ? WHERE kod = ?");
            st.setString(1, _aktif_plaka );
            st.setString(2, _ruhsat_plaka );
            st.setString(3, kapi_kodu );
            st.executeUpdate();

            st = con.prepareStatement("INSERT INTO " + GitasDBT.PLAKA_DEGISIMLERI + " ( aktif_plaka, oto, tarih ) VALUES (?, ?, ?) ");
            st.setString(1, _aktif_plaka );
            st.setString(2, kapi_kodu );
            st.setString(3, tarih );
            st.executeUpdate();

            st.close();
            con.close();
            return true;
        } catch( SQLException e ){
            e.printStackTrace();
        }
        return false;
    }

    public boolean iys_kontrolu_yap(){
        boolean output = false;
        try {
            Connection con = DBC.getInstance().getConnection();
            PreparedStatement st = con.prepareStatement("SELECT * FROM " + GitasDBT.IYS_KAYIT + " WHERE goruldu = ? && oto = ?");
            st.setInt(1,0);
            st.setString(2, kapi_kodu);
            ResultSet res = st.executeQuery();
            output = res.next();
            res.close();
            st.close();
            con.close();
        } catch( SQLException e ){
            e.printStackTrace();
        }
        return output;
    }

    public boolean zayi_sefer_kontrolu(){
        boolean output = false;
        try {
            Filo_Senkronizasyon.aktif_gun_hesapla();
            Connection con = DBC.getInstance().getConnection();
            PreparedStatement pst = con.prepareStatement("SELECT * FROM " + GitasDBT.ORER_KAYIT + " WHERE oto = ? && ( durum = ? || durum = ? ) && tarih = ?");
            pst.setString(1, kapi_kodu );
            pst.setString(2, Sefer_Data.DIPTAL );
            pst.setString(3, Sefer_Data.DYARIM );
            pst.setString(4, Filo_Senkronizasyon.get_aktif_gun() );
            ResultSet res = pst.executeQuery();
            output = res.next();
            res.close();
            pst.close();
            con.close();
        } catch( SQLException e ){
            e.printStackTrace();
        }
        return output;
    }

    public boolean is_ok(){
        return this.ok;
    }

    public String get_kapi_kodu(){
        return this.kapi_kodu;
    }
    public String get_ruhsat_plaka(){
        return this.ruhsat_plaka;
    }
    public String get_aktif_plaka(){
        return this.aktif_plaka;
    }

    public boolean plaka_degismis(){
        return !aktif_plaka.equals(ruhsat_plaka);
    }

}
