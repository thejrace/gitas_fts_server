package sample;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Created by Obarey on 10.02.2017.
 * PHP deki class in java versiyonu
 */
public class Filo_Senkronizasyon {

    private static String aktif_gun;

    public static void aktif_gun_hesapla( /*String son_orer*/ ){
        Connection con = null;
        Statement st = null;
        ResultSet res = null;

        try{
            con = DBC.getInstance().getConnection();

            st = con.createStatement();
            res = st.executeQuery("SELECT * FROM " + GitasDBT.ORER_LOG + " WHERE durum = '1' ");
            if( res.next() ){
                // eski kayit var
                if(  Double.valueOf(res.getString("gecerlilik")) < Common.get_unix() ){
                    // artik gecerli degil bu tarih

                    if( Double.valueOf(res.getString("sonraki_gun_hesaplama")) < Common.get_unix() ){
                        // bir sonraki gunun hesaplama saati gelmiş||geçmiş

                        // onceki gunun durumunu 0 yapiyoruz once
                        st = con.createStatement();
                        st.executeUpdate("UPDATE " + GitasDBT.ORER_LOG + " SET durum = '0' WHERE id = '"+ res.getString("id")+"' ");

                        // ( suan dan son orer e kadar olan saniye ) + ( SIMDI ) --> gecerlilik unix
                        long gecerlilik_dk_uzun = Sefer_Sure.hesapla_uzun( Common.get_current_hmin(), "02:00" );
                        long gecerlilik_kisa = Sefer_Sure.hesapla( Common.get_current_hmin(), "02:00" );
                        if( gecerlilik_dk_uzun < 0 ) gecerlilik_dk_uzun *= -1;
                        if( gecerlilik_kisa < 0 ) gecerlilik_kisa *= -1;
                        long gecerlilik;
                        long sonraki_gun_hesaplama;
                        System.out.println(gecerlilik_kisa);
                        System.out.println(gecerlilik_dk_uzun);
                        if( gecerlilik_kisa <= 90 ){
                            //saat 00 dan sonra giris yapilmis
                            gecerlilik = ( gecerlilik_kisa * 60 ) + Common.get_unix();
                            //sonraki_gun_hesaplama = ( Sefer_Sure.hesapla( Common.get_current_hmin(), "05:45" ) * 60  ) + ( Common.get_unix() );
                            sonraki_gun_hesaplama = gecerlilik + 13500;
                            aktif_gun = Common.get_yesterday_date();
                        } else {
                            gecerlilik = ( gecerlilik_dk_uzun * 60 ) + Common.get_unix();
                            //sonraki_gun_hesaplama = ( Sefer_Sure.hesapla_uzun( Common.get_current_hmin(), "05:45" ) * 60  ) + ( Common.get_unix() );
                            sonraki_gun_hesaplama = gecerlilik + 13500;
                            aktif_gun = Common.get_current_date();
                        }

                        // yeni kaydi ekle
                        ResultSet kontrol = null;
                        st = con.createStatement();
                        kontrol = st.executeQuery("SELECT * FROM " + GitasDBT.ORER_LOG + " WHERE tarih = '"+aktif_gun+"' ");
                        if( !kontrol.next() ){
                            st = con.createStatement();
                            st.executeUpdate( "INSERT INTO " + GitasDBT.ORER_LOG + "  ( tarih, son_orer, gecerlilik, sonraki_gun_hesaplama, durum ) VALUES ( '"+aktif_gun+"', '03', '"+gecerlilik+"', '"+sonraki_gun_hesaplama+"', '1')  ");
                        }
                        kontrol.close();
                    } else {
                        // yeni gunun hesaplama saati gelmediyse beklemeye aliyoruz durumu
                        aktif_gun = "BEKLEMEDE";
                    }
                } else {
                    // halen gecerli aktif tarih
                    aktif_gun = res.getString("tarih");
                }
            } else {
                // ilk kayit

                // ( suan dan son orer e kadar olan saniye ) + ( SIMDI ) --> gecerlilik unix
                long gecerlilik_dk_uzun = Sefer_Sure.hesapla_uzun( Common.get_current_hmin(), "02:00" );
                long gecerlilik_kisa = Sefer_Sure.hesapla( Common.get_current_hmin(), "02:00" );
                if( gecerlilik_dk_uzun < 0 ) gecerlilik_dk_uzun *= -1;
                if( gecerlilik_kisa < 0 ) gecerlilik_kisa *= -1;
                long gecerlilik;
                long sonraki_gun_hesaplama;
                System.out.println(gecerlilik_kisa);
                System.out.println(gecerlilik_dk_uzun);
                if( gecerlilik_kisa <= 90 ){
                    //saat 00 dan sonra giris yapilmis
                    gecerlilik = ( gecerlilik_kisa * 60 ) + Common.get_unix();
                    //sonraki_gun_hesaplama = ( Sefer_Sure.hesapla( Common.get_current_hmin(), "05:45" ) * 60  ) + ( Common.get_unix() );
                    sonraki_gun_hesaplama = gecerlilik + 13500;
                    aktif_gun = Common.get_yesterday_date();
                } else {
                    gecerlilik = ( gecerlilik_dk_uzun * 60 ) + Common.get_unix();
                    //sonraki_gun_hesaplama = ( Sefer_Sure.hesapla_uzun( Common.get_current_hmin(), "05:45" ) * 60  ) + ( Common.get_unix() );
                    sonraki_gun_hesaplama = gecerlilik + 13500;
                    aktif_gun = Common.get_current_date();
                }

                // yeni kaydi ekle
                st = con.createStatement();
                st.executeUpdate( "INSERT INTO " + GitasDBT.ORER_LOG + "  ( tarih, son_orer, gecerlilik, sonraki_gun_hesaplama, durum ) VALUES ( '"+aktif_gun+"', '03', '"+gecerlilik+"', '"+sonraki_gun_hesaplama+"', '1')  ");


            }
        } catch( SQLException e ){
            try {
                FileWriter f2 = new FileWriter(new File("C:\\temp\\log.txt"), true);
                f2.write( Common.get_current_datetime() + " --> " + e.toString()+ "\r\n");
                f2.close();
            } catch( IOException ex ){
                ex.printStackTrace();
            }
            e.printStackTrace();
        }
        try {
            if( res != null ) res.close();
            if( st != null ) st.close();
            if( con != null ) con.close();
        } catch( SQLException e ){
            e.printStackTrace();
        }

    }

    public static String get_aktif_gun(){
        return aktif_gun;
    }

}
