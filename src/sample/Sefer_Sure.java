package sample;

/**
 * Created by Obarey on 01.02.2017.
 */
public class Sefer_Sure {

    public static int hesapla( String from, String to ){
        if( from.equals("") || to.equals("") ) return 0;
        String[] from_exp = from.split(":");
        String[] to_exp = to.split(":");
        int from_saat = Integer.valueOf(from_exp[0]);
        int to_saat = Integer.valueOf( to_exp[0] );
        int from_dk = Integer.valueOf( from_exp[1] );
        int to_dk = Integer.valueOf( to_exp[1] );
        int dakika = 0;
        int saat = 0;
        boolean eksi = false;

        if( to_exp.length < 2 || from_exp.length < 2 ) return 0;


        Integer[] saat_liste = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23 };
        if( from_saat == to_saat ) return to_dk - from_dk;
        // sadece dakikalari degistiriyoruz
        if( to_saat < from_saat ){
            int to_dk_temp = to_dk;
            to_dk = from_dk;
            from_dk = to_dk_temp;
            //eksi = true;
        }
        int ileri = 0, geri = 0;
        for( int x = from_saat; ; x++ ){
            if( x == saat_liste.length ) x = 0;
            if( saat_liste[x] == to_saat ) break;
            ileri++;
        }
        for( int x = from_saat; ; x-- ){
            if( x == -1 ) x = 23;
            if( saat_liste[x] == to_saat ) break;
            geri++;
        }
        int varis;
        if( geri < ileri ){
            varis = geri;
            eksi = true;
        } else if( geri > ileri ){
            varis = ileri;
        } else {
            varis = geri;
        }
        saat = 0;
        dakika += to_dk;
        dakika += 60 - from_dk;
        saat++;
        while( saat != varis ){
            dakika += 60;
            saat++;
        }
        if( eksi ) return dakika*-1;
        return dakika;
    }

    // from to yu gecmis mi kontrol
    public static boolean gecmis( String from, String to ){
        if( from.equals("") || to.equals("") ) return false;
        String[] from_exp = from.split(":");
        String[] to_exp = to.split(":");
        int from_saat = Integer.valueOf(from_exp[0]);
        int to_saat = Integer.valueOf( to_exp[0] );
        int from_dk = Integer.valueOf( from_exp[1] );
        int to_dk = Integer.valueOf( to_exp[1] );
        int dakika = 0;
        int saat = 0;
        boolean eksi = false;

        if( to_exp.length < 2 || from_exp.length < 2 ) return false;

        // todo DAKİKALARIDA DAHİL EDİCEZ AYNI SAAT İÇİN

        Integer[] saat_liste = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 0 };
        if( from_saat == to_saat ){
            if( to_dk > from_dk ) return false;
            if( to_dk == from_dk || to_dk < from_dk  ) return true;
        }
        // sadece dakikalari degistiriyoruz
        if( to_saat < from_saat ){
            int to_dk_temp = to_dk;
            to_dk = from_dk;
            from_dk = to_dk_temp;
            //eksi = true;
        }
       boolean ileri = false;
       int  from_index = 0;
       for( int j = 0; j < saat_liste.length; j++ ){
           if( saat_liste[j] == from_saat ){
               from_index = j;
               break;
           }
       }

        for( int x = from_index; x < 24 ; x++ ){
            if( saat_liste[x] == to_saat ){
                ileri = true;
                break;
            }
        }
        return !ileri;
    }

    public static int hesapla_uzun( String from, String to ){
        if( from.equals("") || to.equals("") ) return 0;
        String[] from_exp = from.split(":");
        String[] to_exp = to.split(":");
        int from_saat = Integer.valueOf(from_exp[0]);
        int to_saat = Integer.valueOf( to_exp[0] );
        int from_dk = Integer.valueOf( from_exp[1] );
        int to_dk = Integer.valueOf( to_exp[1] );
        int dakika = 0;
        int saat = 0;
        boolean eksi = false;
        if( to_exp.length < 2 || from_exp.length < 2 ) return 0;
        Integer[] saat_liste = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23 };
        if( from_saat == to_saat ) return to_dk - from_dk;
        int ileri = 0;
        for( int x = from_saat; ; x++ ){
            if( x == saat_liste.length ) x = 0;
            if( saat_liste[x] == to_saat ) break;
            ileri++;
        }
        int varis = ileri;
        saat = 0;
        dakika += to_dk;
        dakika += 60 - from_dk;
        saat++;
        while( saat != varis ){
            dakika += 60;
            saat++;
        }
        return dakika;
    }

}
