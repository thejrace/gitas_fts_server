package sample;

import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import org.jsoup.Jsoup;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;


public class Filo_Captcha_Controller implements Initializable {

    private WebView wv_1;
    @FXML
    private HBox wv_container;
    private Refresh_Listener listener;
    private URL url;
    private boolean wv_inited = false;
    @Override
    public void initialize(URL fxmlFileLocation, ResourceBundle resources) {
        random_sessid_al();
    }

    public void add_finish_listener( Refresh_Listener _listener ){
        listener = _listener;
    }

    private void web_view_init( final String login, final String password ){
        try {
            url = new URL("http://filo5.iett.gov.tr/login.php?sayfa=");
        } catch( MalformedURLException e ){
            e.printStackTrace();
        }
        wv_1 = new WebView();
        wv_1.setPrefWidth(300);
        URI uri = null;
        try {
            uri = new URI(url.getProtocol(), url.getHost(), url.getPath(), url.getQuery(), null);
        } catch( URISyntaxException | NullPointerException e ){
            e.printStackTrace();
        }
        Map<String, List<String>> headers = new LinkedHashMap<>();
        headers.put("Set-Cookie", Arrays.asList("PHPSESSID="+Filo_Download.filo5_cookie));
        try {
            java.net.CookieHandler.getDefault().put(uri, headers);
        } catch( IOException e ){
            e.printStackTrace();
        }

        WebEngine we = wv_1.getEngine();
        try {
            URL url = new URL("http://filo5.iett.gov.tr/login.php?sayfa=");
            we.setJavaScriptEnabled(true);
            we.getLoadWorker().stateProperty().addListener(
                    (ObservableValue<? extends Worker.State> observable,
                     Worker.State oldValue,
                     Worker.State newValue) -> {
                        if (newValue != Worker.State.SUCCEEDED) {
                            return;
                        }
                            we.executeScript(" " +
                                    " function hide( elem ){ elem.style.display = \"none\"; } "+
                                    " document.body.style.backgroundColor = \"#302e2e\"; document.body.style.overflowY = \"hidden\";" +
                                    " document.body.style.color = \"#272727\"; document.body.style.fontSize = \"0px\";" +
                                    " var link = document.getElementsByTagName(\"a\"); link[0].style.color= \"#d1d1d1\"; link[0].style.marginLeft = \"-50px\"; link[0].style.fontFamily = \"Tahoma\"; link[0].innerHTML = \"Kodu Değiştir\";" +
                                    " link[0].style.textDecoration = \"none\"; link[0].style.fontSize = \"11\";  link[0].style.fontWeight = \"bold\";  "+
                                    " var trs = document.getElementsByTagName(\"tr\"); hide(trs[0]); hide(trs[2]); hide(trs[3]); " +
                                    " var cin = document.querySelectorAll('[name=\"captcha\"]'); cin[0].style.width = \"45px\"; cin[0].style.position = \"relative\"; cin[0].style.top = \"-68px\"; cin[0].style.left = \"220px\";"+
                                    " var form = document.getElementById(\"aday\"); form.style.marginTop = \"-30px\"; form.style.marginLeft = \"-50px\"; " +
                                    " var submitbtn = document.querySelectorAll('[value=\"Giriş\"]');  submitbtn[0].style.marginTop = \"-68px\"; submitbtn[0].style.marginLeft = \"220px\";" +
                                    " submitbtn[0].style.backgroundColor = \"#7b3275\"; submitbtn[0].style.color = \"#d1d1d1\"; submitbtn[0].style.fontWeight = \"bold\";  submitbtn[0].style.border = \"none\"; " +
                                    " submitbtn[0].style.padding = \"6px 10px 6px 10px\"; submitbtn[0].style.borderRadius = \"3px\"; submitbtn[0].style.fontSize = \"11px\"; submitbtn[0].style.cursor = \"pointer\"; "+
                                    " var form_login = document.querySelectorAll('[name=\"login\"]');" +
                                    " var form_pass = document.querySelectorAll('[name=\"password\"]');" +
                                    " if( form_login[0] != undefined ) hide(form_login[0]); form_login[0].value=\""+login+"\"; if( form_pass[0] != undefined ) hide(form_pass[0]); form_pass[0].value=\""+password+"\";  " +
                                    " var divo = document.createElement(\"div\"); divo.id = \"hederoy\"; document.body.appendChild(divo); document.getElementById(\"hederoy\").innerHTML = document.cookie;" +
                                    " var cimg = document.getElementsByTagName(\"img\");  " );
                            if( wv_inited ){
                                try {
                                    // eger <font>Yanlış kod girildi</font> yoksa bilerek exception attiriyoruz, takip scene e geciyoruz
                                    we.executeScript( " var fs = document.getElementsByTagName(\"font\");" +
                                            " fs[0].style.opacity = 0.3; ");
                                    Alert alert = new Alert(Alert.AlertType.ERROR);
                                    alert.setTitle("Gitaş Filo Takip Sistemi");
                                    alert.setHeaderText("Hata!");
                                    alert.setContentText("Güvenlik kodu yanlış. Tekrar deneyin.");
                                    alert.showAndWait();
                                } catch( netscape.javascript.JSException e ) {
                                    listener.on_refresh();
                                }
                            }
                            if( !wv_inited ){
                                Platform.runLater(new Runnable() {
                                    @Override
                                    public void run() {
                                        wv_container.getChildren().add(0, wv_1);
                                        wv_inited = true;
                                    }
                                });
                            }
                    });
            we.load(url.toString());
        } catch( MalformedURLException e ){
            e.printStackTrace();
        }
    }

    private void random_sessid_al(){
        login_thread( "", "", "");
    }

    private void login_thread( final String login, final String password, final String captcha ){
        Thread th = new Thread(new Runnable() {
            @Override
            public void run() {
                org.jsoup.Connection.Response res;
                try{
                    System.out.println( "Filo phpsessid alınıyor..");
                    // random phpssid
                    res = Jsoup.connect("http://filo5.iett.gov.tr/login.php?sayfa=/_FYS.php&aday=x")
                            .method(org.jsoup.Connection.Method.POST)
                            .timeout(0)
                            .execute();

                    Filo_Download.filo5_cookie = res.cookies().get("PHPSESSID");
                    System.out.println( " phpsessid alındı. -c["+res.cookies().get("PHPSESSID")+"]");
                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            web_view_init( "dk_oasfilo", "oas.123");
                        }
                    });
                } catch( IOException e ){
                    System.out.println( " HATA");
                }
            }
        });
        th.setDaemon(true);
        th.start();

    }

}
