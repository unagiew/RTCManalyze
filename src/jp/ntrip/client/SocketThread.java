package jp.ntrip.client;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import javafx.application.Platform;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.scene.text.Text;

public class SocketThread extends Service<Boolean> {
    private FxField fxField;
    private HttpRequest request;

    private Socket soc = null;
    private DataInputStream inBinaryBuf = null;
    private PrintWriter outStringBuf = null;

    DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss.SSS");

    /**
     * コンストラクタ
     * @param fxField JAVAFXのフィールド名
     */
    public SocketThread(FxField fxField) {
        this.fxField = fxField;
    }

    @Override
    protected Task<Boolean> createTask() {
        return new Task<>() {
            @Override
            protected Boolean call() {
                connectSession();
                return null;
            }
        };
    }

    /**
     * HttpRequestセッター<br>
     * メイン画面で作成されたHTTP Requestをセットする
     */
    public void setRequest(HttpRequest request) {
        this.request = request;
    }

    private void connectSession() {
        try {
            soc = new Socket(request.getDestAddress(), request.getDestPort());
            outStringBuf = new PrintWriter(soc.getOutputStream(), true);
            inBinaryBuf = new DataInputStream(soc.getInputStream());

            sendRequest();
            receiveLoop();
        } catch (UnknownHostException e) {
            outputError(e.getMessage());
            e.printStackTrace();
        } catch (ConnectException e) {
            outputError(e.getMessage());
        } catch (SocketException e) {
            System.out.println("Socket: SocketException");
            outputError(e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            disconnect();
        }
    }

    /*
     *
     */
    private void sendRequest() {
        String sendRequest = request.getRequst();
        LocalDateTime localDateTime = LocalDateTime.now();

        outStringBuf.println(sendRequest);

        Text hexText = new Text(localDateTime.format(dtf) + "\r\n" +
                Utilities.hexPrint(sendRequest.getBytes(), sendRequest.getBytes().length));
        Text sendText = new Text(localDateTime.format(dtf) + "\r\n" +
                "dest: " + request.getDestAddress() + ":" + request.getDestPort() + "\r\n" +
                request.getRequst() + "\r\n");
        String sendString = request.getRequestLine().replaceAll("\r\n$", "");
        Platform.runLater(() -> {
            fxField.getHexText().getChildren().add(hexText);
            fxField.getAnalyzedText().getChildren().add(sendText);
            fxField.getInformationTable().getItems().add(new InfoTable("HTTP Request", sendString));
        });
    }

    /*
     *
     */
    private void receiveLoop() {
        int receivedLen;
        byte[] data= new byte[8192];
        LocalDateTime localDateTime = null;
        HttpResponse response = new HttpResponse();
        DecodeRTCM3 rtcm = new DecodeRTCM3(fxField);
        boolean httpEndFlag = false;

        try {
            while((receivedLen = inBinaryBuf.read(data)) != -1) {
                localDateTime = LocalDateTime.now();
                outputRawData(localDateTime, data, receivedLen);

                if(httpEndFlag) {
                    // RTCM処理
                    rtcm.addData(localDateTime.format(dtf), data, receivedLen);
                } else {
                    // HTTP処理
                    response.addData(data, receivedLen);
                    if(httpEndFlag = response.checkHttpEnd()) {
                        outputHTTPResponseOnTable(localDateTime, response);
                        if(response.hasBinaryData()) {
                            // RTCM処理
                            System.out.println("BINARY");
                            rtcm.addData(localDateTime.format(dtf), response.getBinaryData(), response.getBinaryLength());
                        }
                        response = null;
                    }
                }
            }

            // TCP切断時にレスポンスデータが残っていれば出力処理
            // SOURCETABLEや通常HTTP Response受信時の処理
            if(response != null) {
                assert localDateTime != null;
                outputHTTPResponseOnTable(localDateTime, response);
            }
        } catch (SocketException e) {
            System.out.println("Read: SocketException");
            outputError(e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            Platform.runLater(() -> {
                fxField.getHexText().getChildren().add(new Text("Connection Closed\r\n"));
                fxField.getAnalyzedText().getChildren().add(new Text("Connection Closed\r\n"));
            });
        }
    }

    private void outputRawData(LocalDateTime ldt, byte[] data, int length) {
        Text text = new Text(ldt.format(dtf) + "  (" + length + "bytes)" + "\r\n" + Utilities.hexPrint(data, length));

        // textflow出力。メモリ使用量抑制のため100行超えると50行削除
        Platform.runLater(() -> {
            if(fxField.getHexText().getChildren().size() > 100) {
                fxField.getHexText().getChildren().remove(0, 50);
            }
            if(fxField.getAnalyzedText().getChildren().size() > 100) {
                fxField.getAnalyzedText().getChildren().remove(0, 50);
            }
            fxField.getHexText().getChildren().add(text);
        });
    }

    private void outputHTTPResponseOnTable(LocalDateTime ldt, HttpResponse response) {
        Text text = new Text(ldt.format(dtf) + "\r\n" + response.getHttpData());
        Platform.runLater(() -> {
            fxField.getAnalyzedText().getChildren().add(text);
            fxField.getInformationTable().getItems().add(new InfoTable("HTTP Response",response.getStatusLine().replaceAll("\r\n$", "")));
        });
    }

    private void outputError(String message) {
        Platform.runLater(() -> fxField.getErrorLabel().setText(message));
    }

    /**
     * socketおよびfileをクローズする
     */
    public void disconnect() {
        System.out.println("disconnect");
        Platform.runLater(() -> {
            fxField.getStartButton().setText("Start");
            fxField.getStartButton().setStyle(null);
            fxField.getStartButton().setDisable(false);
        });

        try {
            if(outStringBuf != null) {
                outStringBuf.close();
            }
            if(inBinaryBuf != null) {
                inBinaryBuf.close();
            }
            if(soc != null) {
                soc.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
