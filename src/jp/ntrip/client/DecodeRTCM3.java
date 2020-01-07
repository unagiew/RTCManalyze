package jp.ntrip.client;

import java.util.HashMap;
import java.util.Map;
import javafx.application.Platform;
import javafx.scene.chart.BarChart;
import javafx.scene.text.Text;

public class DecodeRTCM3 {
    private static final int BUFFERLENGTH = 4096;

    private static final byte PREAMBLE = (byte)0xd3;		// 8bits
    private static final byte RESERVED = (byte)0x00;		// upper6bits
    private static final int CRCBYTELENGTH = 3;			    // 24bits CRC length
    private static final int HEADERBYTELENGTH = 3;		    // 24bits
    private static final int CRC24_POLY  = 0x1864CFB;

    private byte[] storeData = new byte[BUFFERLENGTH];
    private int storeLength;
    private String time;

    private Map<Integer, BarChart<Number, String>> barChartMap = new HashMap<>();
    private Map<Integer, InfoTable> infoTableMap = new HashMap<>();
    private StringBuilder analyzedOutput = new StringBuilder();
    private FxField fxField;

    public DecodeRTCM3(FxField fxField) {
        this.fxField = fxField;
    }

    public void addData(String time, final byte[] receiveData, int length) {
        this.time = time;
        System.arraycopy(receiveData, 0, storeData, storeLength, length);
        storeLength += length;
        execute();
    }

    private void execute() {
        int preambleIndex = -1;		// preamble位置
        int length = 0;				// RTCMメッセージ長

        while(storeLength > HEADERBYTELENGTH) {
            for(int i = 0; i < storeLength - 1; i++) {
                if(storeData[i] == PREAMBLE && ((storeData[i + 1] & 0xfc) >> 2) == RESERVED) {
                    preambleIndex = i;

                    if(preambleIndex + HEADERBYTELENGTH > storeLength) {
                        return;
                    }

                    length = Utilities.bitToInt(storeData, 14, 10) + HEADERBYTELENGTH + CRCBYTELENGTH;
                    if(preambleIndex + length > storeLength)  {
                        return;
                    }
                    break;
                }
            }

            if(checkLenghCrc(preambleIndex, length)) {
                checkMessageNumber(preambleIndex, length);	// RTCMメッセージデコード処理
            }

            // storeDataの残りを0位置に移動
            storeLength -= length;
            System.arraycopy(storeData, length, storeData, 0, storeLength);
        }
    }

    private boolean checkLenghCrc(int index, int length) {
        // length check
        if(index + length < storeLength && storeData[index + length] != PREAMBLE) {
            System.out.println("Length Error");
            outputInfoTable("RTCM Length", "Length Error");
            return false;
        }

        // CRC check
        int crc = 0;
        int targetCRC = Utilities.bitToInt(storeData, (index + length - CRCBYTELENGTH) * 8, CRCBYTELENGTH * 8);
        for(int i = 0; i < length - CRCBYTELENGTH; i++) {
            crc ^= storeData[index + i] << 16;
            for(int j= 0; j < 8; j++) {
                crc <<= 1;
                if((crc & 0x1000000) != 0) {
                    crc ^= CRC24_POLY;
                }
            }
        }
        if(targetCRC != crc) {
            System.out.println("CRC Error");
            outputInfoTable("RTCM CRC", "CRC Error");
            return false;
        }
        return true;
    }

    private void checkMessageNumber(int index, int length) {
        int type = Utilities.bitToInt(storeData, (index + HEADERBYTELENGTH) * 8, 12);
        analyzedOutput.append(time).append("  (").append(length).append("bytes)\r\ntype:").append(type).append(" ");
        RtcmMessage message = null;

        if(type == 1005 || type == 1006) {
            message = new Rtcm1005(storeData, index, length).decode();
        } else if(type == 1033) {
            message = new Rtcm1033(storeData, index, length).decode();
//        } else if(type == 1019) {
//            message = new Rtcm1019(storeData, index, length).decode();
        } else if(type >= 1071 && type <= 1127) {
            message = new RtcmMsm(storeData, index, length).decode();
            addBarChart((RtcmMsm) message);
        } else {
            analyzedOutput.append("Unknown\r\n");
        }

        if(message != null) {
            outputInfoTable(message.getMessageType(), message.getSuccinctInfo());
            analyzedOutput.append(message.getAnalyzedString());
        }
        Text text = new Text(analyzedOutput.toString() + "\r\n");
        Platform.runLater(() -> fxField.getAnalyzedText().getChildren().add(text));
        analyzedOutput.setLength(0);
    }

    private void addBarChart(RtcmMsm message) {
        int gnssType = message.getGnssType();
        BarChart<Number, String> barChart;
        if((barChart = barChartMap.get(gnssType)) == null) {
            BarChart<Number, String> tmpChart = message.makeBarChart();
            barChartMap.put(gnssType, tmpChart);
            Platform.runLater(() -> {
                tmpChart.getData().addAll(message.makeCnrSeries());
                fxField.getPaneForChart().getChildren().add(tmpChart);
            });
        } else {
            Platform.runLater(() -> {
                barChart.getData().clear();
                barChart.getData().addAll(message.makeCnrSeries());
            });
        }
    }

    private void outputInfoTable(String item, String value) {
        InfoTable table;
        if((table = infoTableMap.get(Integer.valueOf(item))) == null) {
            InfoTable tmp = new InfoTable(item, value);
            infoTableMap.put(Integer.valueOf(item), tmp);
            Platform.runLater(() -> fxField.getInformationTable().getItems().add(tmp));
        } else {
            table.setValue(value);
            table.setTimes(table.getTimes() + 1);
        }
    }
}
