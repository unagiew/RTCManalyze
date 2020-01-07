package jp.ntrip.client;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Side;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.XYChart.Series;

public class RtcmMsm extends RtcmMessage {
    private static final String[] GPSSIGNALMAP = {"","","1C","1P","1W","","","","2C","2P","2W","","","","","2S","2L","2X","","","","","5I","5Q","5X","","","","","","1S","1L","1X"};
    private static final String[] GLONASSSIGNALMAP = {"","","1C","1P","","","","","2C","2P"};
    private static final String[] GALILEOSIGNALMAP = {"","","1C","1A","1B","1X","1Z","","6C","6A","6B","6X","6Z","","7I","7Q","7X","","8I","8Q","8X","","5I","5Q","5X"};
    private static final String[] SBASSINGALMAP = {"","","1C","","","","","","","","","","","","","","","","","","","","5I","5Q","5X"};
    private static final String[] QZSSIGNALMAP = {"","","1C","","","","","","","6S","6L","6X","","","","2S","2L","2X","","","","","5I","5Q","5X","","","","","","1S","1L","1X"};
    private static final String[] BEIDOUSIGNALMAP = {"","","2I","2Q","2X","","","","6I","6Q","6X","","","","7I","7Q","7X"};
    private static final String[][] SIGNALMAP = {GPSSIGNALMAP,GLONASSSIGNALMAP,GALILEOSIGNALMAP,SBASSINGALMAP,QZSSIGNALMAP,BEIDOUSIGNALMAP};

    private int msmType;
    private int gnssType;

    private List<Integer> satelliteList;	// satellite IDのリスト
    private List<Integer> signalList;		// signal IDのリスト
    private long cellMask;					// cellの2Dテーブル
    private int epochTime;
    private List<Integer> ltiList;
    private List<Integer> cnrList;

    public RtcmMsm(byte[] data, int index, int length) {
        super(data, index, length);
    }

    @Override
    RtcmMsm decode() {
        int bitPoint = decodeHeader();
        ltiList = new ArrayList<>();
        cnrList = new ArrayList<>();

        /*
         * satellite Data
         */
        int satelliteNumber = satelliteList.size();
        if(msmType == 4 || msmType == 5 || msmType == 6 || msmType == 7) {
            // DF397	8bits*Nsat
            bitPoint += 8 * satelliteNumber;
        }

        if(msmType == 5 || msmType == 7) {
            // Extended Satellite Information	4bits*Nsat
            bitPoint += 4 * satelliteNumber;
        }

        // DF398	10bits*Nsat
        bitPoint += 10 * satelliteNumber;


        if(msmType == 5 || msmType == 7) {
            // DF399	14bits*Nsat
            bitPoint += 14 * satelliteNumber;
        }

        /*
         * signal Data
         */
        int cellNumber = Long.bitCount(cellMask);
        if(msmType == 1 || msmType == 3 || msmType == 4 || msmType == 5) {
            // DF400 signal fine pseudorange
            bitPoint += 15 * cellNumber;
        }

        if(msmType == 2 || msmType == 3 || msmType == 4 || msmType == 5) {
            // DF401 signal fine phaserange data
            bitPoint += 22 * cellNumber;

            // DF402 phaserange Lock Time Indicator
            for(int i = 0; i < cellNumber; i++) {
                ltiList.add(Utilities.bitToInt(data, bitPoint, 4));
                bitPoint += 4;
            }

            // DF420 half-cycle ambiguity indicator
            bitPoint += cellNumber;
        }

        if(msmType == 4 || msmType == 5) {
            // DF403 signal CNRs
            for(int i = 0; i < cellNumber; i++) {
                cnrList.add(Utilities.bitToInt(data, bitPoint, 6));
                bitPoint += 6;
            }
        }

        if(msmType == 5) {
            // DF404 signal fine phaserangerate
            bitPoint += 15 * cellNumber;
        }

        if(msmType == 6 || msmType == 7) {
            // DF405 signal fine pseudoranges
            bitPoint += 20 * cellNumber;

            // DF406 signal fine Phaserange
            bitPoint += 24 * cellNumber;

            // DF407 Phaserange Lock Time Indicator
            for(int i = 0; i < cellNumber; i++) {
                ltiList.add(Utilities.bitToInt(data, bitPoint, 10));
                bitPoint += 10;
            }

            // DF420 Half-cycle ambiguity indicator
            bitPoint += cellNumber;

            // DF408 signal CNR	  *doubleだがintにキャストしている。cellMask変換を統一するため
            for(int i = 0; i < cellNumber; i++) {
                cnrList.add((int)(Utilities.bitToInt(data, bitPoint, 10) * Math.pow(2, -4)));
                bitPoint += 10;
            }
        }

 //       if(msmType == 5 || msmType == 7) {
 //           // DF404 signal fine PhaseRangeRates
 //           bitPoint += 15 * cellNumber;
 //       }

        return this;
    }

    private int decodeHeader() {
        int bitPoint = HEADER_BYTE_LENGTH * 8;

        // DF002 Message Number
        messageType = Utilities.bitToInt(data, bitPoint, 12);
        bitPoint += 12;

        classifyMSM();

        // DF003 Reference Station ID
        @SuppressWarnings("unused")
        int stationID = Utilities.bitToInt(data, bitPoint, 12);
        bitPoint += 12;

        // GNSS Epoch Time
        if(gnssType == GeoCode.GNSSGLONASS) {
            @SuppressWarnings("unused")
            int dow = Utilities.bitToInt(data, bitPoint, 3);
            epochTime = Utilities.bitToInt(data, bitPoint + 3, 27);
        } else {
            epochTime = Utilities.bitToInt(data, bitPoint, 30);
        }
        bitPoint += 30;

        // MMB:1, IODS:3, Rsv:7, CSI:2, ECI:2, GDSI:1, GSI:3
        bitPoint += 19;

        // DF394  satellite Mask
        satelliteList = getBitMask(data, bitPoint, 64);
        bitPoint += 64;

        // DF395  signal Mask
        signalList = getBitMask(data, bitPoint, 32);
        bitPoint += 32;

        // DF396  cell Mask
        cellMask = Utilities.bitToLong(data, bitPoint, satelliteList.size() * signalList.size());
        bitPoint += satelliteList.size() * signalList.size();

        return bitPoint;
    }

    // cellマスクとListを組み合わせて2Dテーブルのリストを作る
    // ビットが立っていない場合は0を挿入
    private List<Integer> getCombinedCellList(List<Integer> target) {
        List<Integer> list = new ArrayList<>();
        int cellLength = satelliteList.size() * signalList.size();
        for(int i = 0, counter = 0; i < satelliteList.size() * signalList.size(); i++) {
            if(((cellMask >> --cellLength) & 0x01) == 1) {
                list.add(target.get(counter++));
            } else {
                list.add(0);
            }
        }
        return list;
    }

    private void classifyMSM() {
        if(messageType >= 1071 && messageType <= 1077) {
            msmType = messageType - 1070; gnssType = GeoCode.GNSSGPS;
        } else if(messageType >= 1081 && messageType <= 1087) {
            msmType = messageType - 1080; gnssType = GeoCode.GNSSGLONASS;
        } else if(messageType >= 1091 && messageType <= 1097) {
            msmType = messageType - 1090; gnssType = GeoCode.GNSSGALILEO;
        } else if(messageType >= 1101 && messageType <= 1107) {
            msmType = messageType - 1100; gnssType = GeoCode.GNSSSBAS;
        } else if(messageType >= 1111 && messageType <= 1117) {
            msmType = messageType - 1110; gnssType = GeoCode.GNSSQZSS;
        } else if(messageType >= 1121 && messageType <= 1127) {
            msmType = messageType - 1120; gnssType = GeoCode.GNSSBEIDOU;
        }
    }

    // satellite IDをPRNに変換する
    private List<String> getPRNList() {
        int weight;

        switch(gnssType) {
            case GeoCode.GNSSSBAS:
                weight = 119;
                break;
            case GeoCode.GNSSQZSS:
                weight = 192;
                break;
            default:
                weight = 0;
        }

        return satelliteList.stream().map(Object -> Object + weight).map(Object::toString).collect(Collectors.toList());
    }

    // bitがたっている位置をlistに格納する
    // 上位ビットを1から開始する
    private List<Integer> getBitMask(byte[] src, int bitPosition, int bitLength) {
        List<Integer> list = new ArrayList<>();
        int j = 0;
        for(int i = bitPosition; i < bitPosition + bitLength; i++) {
            ++j;
            if(((src[i / 8] >> (7 - i % 8)) & 0x01) == 1) {
                list.add(j);
            }
        }

        return list;
    }

    private String printTable(List<Integer> data) {
        return GeoCode.print2DimensionTable(getPRNList(), getSignalStringList(), data);
    }

    @Override
    String getSuccinctInfo() {
        return GeoCode.getGnssName(gnssType) + " MSM" + msmType +"\r\n" +
                "Satellite: " + getPRNList().size() +
                ",  Signal: " + getSignalStringList().size();
    }

    @Override
    String getAnalyzedString() {
        StringBuilder sb = new StringBuilder();

        sb.append(GeoCode.getGnssName(gnssType)).append(" MSM").append(msmType).append("\r\n");
        sb.append("epochtime: ").append(epochTime).append("\r\n");
        sb.append("satellite: ").append(getPRNList()).append("\r\n");
        sb.append("signal: ").append(getSignalStringList()).append("\r\n");
        if(ltiList.size() > 0) {
            sb.append("Lock Time Indicator\r\n").append(printTable(getCombinedCellList(ltiList))).append("\r\n");
        }
        if(cnrList.size() > 0) {
            sb.append("CNR\r\n").append(printTable(getCombinedCellList(cnrList))).append("\r\n");
        }
        return sb.toString();
    }

    @SuppressWarnings("unused")
    private String getConvertedEpocTime() {

        return null;
    }

    private List<String> getSignalStringList() {
        List<String> list = new ArrayList<>();
        for (Integer integer : signalList) {
            list.add(SIGNALMAP[gnssType][integer]);
        }

        return list;
    }

    /**
     * CNR用JAVAFXのバーチャート（Y軸主体）を作成する
     * X軸：dB
     * Y軸：サテライト名
     * データは別途追加すること
     * @return JAVAFX BarChartの枠
     */
    BarChart<Number, String> makeBarChart() {
        NumberAxis na = new NumberAxis(0, 64, 4);
        na.setLabel("dB-Hz");
        CategoryAxis ca = new CategoryAxis();
        ca.setLabel("Satellite");

        BarChart<Number, String> tmpChart = new BarChart<>(na, ca);
        tmpChart.setTitle(GeoCode.getGnssName(gnssType));
        tmpChart.setLegendSide(Side.TOP);
        tmpChart.setTitleSide(Side.TOP);
        tmpChart.setAnimated(false);
        tmpChart.setPrefWidth(400);

        // バー幅アルゴリズム再検討 & makeSeriesへ移動
        tmpChart.setPrefHeight(200 + satelliteList.size() * 15 +  (satelliteList.size() * (signalList.size() - 1)) * 5);
        tmpChart.setCategoryGap((float)(50 / satelliteList.size()));

        return tmpChart;
    }

    /**
     * CNRデータからJAVAFX Seriesを作成する<br>
     * カテゴリ：サテライト名<br>
     * サブカテゴリ：シグナル名
     * @return CNRデータのObservableList
     */
    ObservableList<XYChart.Series<Number,String>> makeCnrSeries() {
        return makeSeries(getPRNList(), getSignalStringList(), getCombinedCellList(cnrList));
    }

    private ObservableList<XYChart.Series<Number,String>> makeSeries(List<String> category, List<String> subCategory, List<Integer> data) {
        ObservableList<XYChart.Series<Number,String>> list = FXCollections.observableArrayList();

        for(int i = 0; i < subCategory.size(); i++) {
            XYChart.Series<Number,String> series = new Series<>();
            series.setName(subCategory.get(i));
            for(int j = i, cat = 0; j < data.size(); j += subCategory.size(), cat++) {
                series.getData().add(new XYChart.Data<>(data.get(j), category.get(cat)));
            }
            list.add(series);
        }
        return list;
    }

    // Getter
    List<Integer> getSatelliteList() {
        return satelliteList;
    }
    List<Integer> getSignalList() {
        return signalList;
    }
    int getGnssType() {
        return gnssType;
    }
    int getMsmType() {
        return msmType;
    }
    int getEpochTime() {
        return epochTime;
    }
    List<Integer> getLtiList() {
        return ltiList;
    }
    List<Integer> getCnrList() {
        return cnrList;
    }
}
