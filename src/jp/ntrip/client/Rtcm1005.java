package jp.ntrip.client;

import java.util.Arrays;

public class Rtcm1005 extends RtcmMessage {
    private double[] ecef = new double[3];
    private double[] lla;

    public Rtcm1005(byte[] data, int index, int length) {
        super(data, index, length);
    }

    @Override
    Rtcm1005 decode() {
        int bitPoint = HEADER_BYTE_LENGTH * 8;

        // DF002 Message Number
        messageType = Utilities.bitToInt(data, bitPoint, 12);
        bitPoint += 12;

        // DF003 Reference Station ID
        @SuppressWarnings("unused")
        int stationID = Utilities.bitToInt(data, bitPoint, 12);
        bitPoint += 12;

        // DF021 ITRF (not used)
        bitPoint += 6;

        // DF022/023/024/141
        bitPoint += 4;

        // DF025 Antenna Reference Point ECEF-X
        ecef[0] = Utilities.bitToLongMSB(data, bitPoint, 38) * 0.0001;
        bitPoint += 38;

        // DF142/001
        bitPoint += 2;

        // DF026 Antenna Reference Point ECEF-Y
        ecef[1] = Utilities.bitToLongMSB(data, bitPoint, 38) * 0.0001;
        bitPoint += 38;

        // DF364
        bitPoint += 2;

        // DF027 Antenna Reference Point ECEF-Y
        ecef[2] = Utilities.bitToLongMSB(data, bitPoint, 38) * 0.0001;
//        bitPoint += 38;

        lla = GeoCode.ecef2lla(ecef);

        return this;
    }

    @Override
    public String getSuccinctInfo() {
        return "ECEF\t" + Arrays.toString(ecef) + "\r\nLLA\t" + Arrays.toString(lla);
    }

    @Override
    public String getAnalyzedString() {
        return "Stationary RTK Reference Station ARP\r\n" +
                "ECEF(X) " + ecef[0] + ", ECEF(Y) " + ecef[1] + ", ECEF(Z) " + ecef[2] + "\r\n";
    }
}
