package jp.ntrip.client;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import javafx.application.Platform;


public class HttpRequest {
    private FxField fxField;

    private String destAddress;
    private int destPort;
    private String originalUrl;
    private String finalUrl;
    private String method;
    private Map<String, String> headers = new LinkedHashMap<>();

    public HttpRequest(FxField fxField) {
        this.fxField = fxField;
    }

    /**
     * 送信先アドレスおよびディレクトリを設定する
     * @param host 送信先アドレス(URL)
     * @param path ディレクトリ
     * @return URLが適正なフォーマットであればtrue
     */
    public boolean setHostAndPath(String host, String path) {
        if(host == null || host.equals("")) {
            outputError("Hostが入力されていません");
            return false;
        }

        host = host.replaceAll("^http://|/+$", "");
        if(path == null || path.equals("")) {
            originalUrl = "http://" + host + "/";
        } else {
            originalUrl = "http://" + host + "/" + path.replaceAll("^/+", "");
        }

        URL url;
        try {
            url = new URL(originalUrl);
        } catch (MalformedURLException e) {
            outputError("Hostが不正です(" + e.getMessage() + ")");
            return false;
        }

        destAddress = url.getHost();
        if((destPort = url.getPort()) == -1) {
            destPort = 80;
        }
        finalUrl = url.getPath();

        return true;
    }

    /**
     * プロキシアドレスを設定する
     * @param proxyAddress プロキシアドレス
     * @param proxyPort プロキシポート
     * @return プロキシポートが数値であればtrue
     */
    public boolean setProxy(String proxyAddress, String proxyPort) {
        destAddress = proxyAddress;
        try {
            destPort = Integer.parseInt(proxyPort);
        } catch(NumberFormatException e) {
            outputError("Proxy Portが数値ではありません");
            return false;
        }
        finalUrl = originalUrl;
        return true;
    }

    /**
     * ベーシック認証を設定する
     * @param user ユーザ名
     * @param pass パスワード
     */
    public void setBasicAuth(String user, String pass) {
        String src = user + ":" + pass;
        String encoded = Base64.getEncoder().encodeToString(src.getBytes());
        addHeader("Authorization", "Basic " + encoded);
    }


    /**
     * NMEAを設定する。<br>
     * 緯度、経度以外は固定値。
     * @param latitude 緯度
     * @param Longitude 経度
     */
    public void setNMEA(String latitude, String Longitude) {
        // NMEAのボディ処理を追加
    }

    /**
     * HTTPメソッドを設定する<br>
     * 固定ヘッダも設定する(User-Agent / Accept / Connection)
     * @param method メソッド
     */
    public void setMethod(String method) {
        this.method = method;

        addHeader("User-Agent", "NTRIP Client0.1");
        addHeader("Accept", "*/*");
        addHeader("Connection", "close");
    }


    /**
     * HTTPヘッダを追加する
     * @param name ヘッダ名
     * @param value 値
     */
    public void addHeader(String name, String value) {
        headers.put(name, value);
    }

    /**
     * HTTPリクエストを生成する
     * @return HTTPリクエスト全文
     */
    public String getRequst() {
        StringBuilder sb = new StringBuilder();

        sb.append(getRequestLine());
        for(Map.Entry<String, String> entry : headers.entrySet()) {
            sb.append(entry.getKey()).append(": ").append(entry.getValue()).append("\r\n");
        }
        sb.append("\r\n");

        return sb.toString();
    }

    /**
     * リクエストラインを生成する
     * @return リクエストライン(HTTPの1行目)
     */
    public String getRequestLine() {
        return method + " " + finalUrl + " HTTP/1.1\r\n";
    }

    /**
     * 送信先アドレスを返す
     * @return 送信先アドレス
     */
    public String getDestAddress() {
        return destAddress;
    }

    /**
     * 送信先ポートを返す
     * @return 送信先ポート
     */
    public int getDestPort() {
        return destPort;
    }

    private void outputError(String message) {
        Platform.runLater(() -> fxField.getErrorLabel().setText(message));
    }
}
