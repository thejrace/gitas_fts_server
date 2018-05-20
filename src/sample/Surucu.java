package sample;

import java.util.ArrayList;

public class Surucu {

    private String sicil_no, isim, telefon, orer, bitis;
    private ArrayList<String> gidisler = new ArrayList<>();
    private ArrayList<String> bitisler = new ArrayList<>();
    public Surucu( String _sicil_no, String _isim, String _telefon ){
        sicil_no = _sicil_no;
        isim = _isim;
        telefon = _telefon;
    }

    public Surucu( String _isim, String _orer, String _bitis, String _durum ){
        isim = _isim;
        orer = _orer;
        bitis = _bitis;
    }

    public void add_bitis( String _data ){
        bitisler.add(_data);
    }
    public void add_gidis( String _data ){
        gidisler.add(_data);
    }
    public ArrayList<String> get_gidisler(){
        return gidisler;
    }
    public ArrayList<String> get_bitisler(){
        return bitisler;
    }

    public String get_orer(){
        return orer;
    }
    public String get_bitis(){
        return bitis;
    }
    public void set_orer( String _orer ){
        orer = _orer;
    }
    public void set_bitis( String _bitis ){
        bitis = _bitis;
    }

    public String get_sicil_no(){
        return sicil_no;
    }
    public String get_isim(){
        return isim;
    }
    public String get_telefon(){
        return telefon;
    }
}
