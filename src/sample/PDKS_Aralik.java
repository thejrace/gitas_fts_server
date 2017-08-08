package sample;

/**
 * Created by Jeppe on 01.05.2017.
 */
public class PDKS_Aralik {

    private String surucu, binis = "YOK", inis = "YOK";
    public PDKS_Aralik( String surucu ){
        this.surucu = surucu;
    }
    public void set_binis( String binis ){
        this.binis = binis;
    }
    public void set_inis( String inis ){
        this.inis = inis;
    }
    public boolean kontrol( String hmin ){
        if( Sefer_Sure.gecmis( hmin, binis ) && inis.equals("AKTİF") ) return true;
        return Sefer_Sure.gecmis( hmin, binis ) && Sefer_Sure.gecmis( inis, hmin );
    }
    public String get_binis(){
        return binis;
    }
    public String get_inis(){
        return inis;
    }
    public String get_surucu(){
        return surucu;
    }



}
