package sample;

/**
 * Created by Jeppe on 30.04.2017.
 */
public class PDKS_Data  {

    public static int   INDI = 1,
                        BINDI = 2;

    private String oto, tarih, surucu, plaka, kart_okutma_saati;
    private int tip;

    public PDKS_Data( String oto, String tarih, String surucu, String plaka, String kart_okutma_saati, int tip ){
        this.oto = oto;
        this.tarih = tarih;
        this.surucu = surucu;
        this.plaka = plaka;
        this.kart_okutma_saati = kart_okutma_saati;
        this.tip = tip;
    }

    public String get_oto(){ return oto; }
    public String get_tarih(){ return tarih; }
    public String get_surucu(){ return surucu; }
    public String get_plaka(){ return plaka; }
    public String get_kart_okutma_saati(){ return kart_okutma_saati; }
    public int get_tip(){ return tip; }
    public static String tip_to_str( int tip ){
        if( tip == INDI ){
            return "İNDİ";
        } else {
            return "BİNDİ";
        }
    }

}
