package jp.ntrip.client;

public class Rtcm1033 extends RtcmMessage {
    String dataString;

    public Rtcm1033(byte[] data, int index, int length) {
        super(data, index, length);
    }

    @Override
    Rtcm1033 decode() {
        int bitPoint = HEADER_BYTE_LENGTH * 8;

        // DF002 Message Number
        messageType = Utilities.bitToInt(data, bitPoint, 12);
        bitPoint += 12;

        // DF003 Reference Station ID
        @SuppressWarnings("unused")
        int stationID = Utilities.bitToInt(data, bitPoint, 12);
        bitPoint += 12;

        // DF029 Antenna Descriptor Counter
        int ad = Utilities.bitToInt(data, bitPoint, 8);
        bitPoint += 8;
        // DF030 Antenna Descriptor
        String antDes = new String(data, bitPoint / 8, ad);
        bitPoint += 8 * ad;

        // DF031 Antenna Setup ID
        int antSetup = Utilities.bitToInt(data, bitPoint, 8);
        bitPoint += 8;

        // DF032 Antenna Serial Number Counter
        int asn = Utilities.bitToInt(data, bitPoint, 8);
        bitPoint += 8;
        // DF033 Antenna Serial Number
        String antNum = new String(data, bitPoint / 8, asn);
        bitPoint += 8 * asn;

        // DF227 Receiver Type Descriptor Counter
        int rtd = Utilities.bitToInt(data, bitPoint, 8);
        bitPoint += 8;
        // DF228 Receiver Type Descriptor
        String recDes = new String(data, bitPoint / 8, rtd);
        bitPoint += 8 * rtd;

        // DF229 Receiver Firmware Version Counter
        int rfv = Utilities.bitToInt(data, bitPoint, 8);
        bitPoint += 8;
        // DF230 Receiver Firmware Version
        String recVer = new String(data, bitPoint / 8, rfv);
        bitPoint += 8 * rfv;

        // DF231 Receiver Serial Number Counter
        int rsn = Utilities.bitToInt(data, bitPoint, 8);
        bitPoint += 8;
        // DF232 Receiver Serial Number
        String recNum = new String(data, bitPoint / 8, rsn);

        dataString = "Receiver and Antenna Descriptors\r\n" +
                "Antenna Descriptor: " + antDes +
                "\r\nAntenna Setup ID: " + antSetup +
                "\r\nAntenna Serial Number: " + antNum +
                "\r\nReceiver Type Descriptor: " + recDes +
                "\r\nReceiver Firmware Version: " + recVer +
                "\r\nReceiver Serial Number: " + recNum;

        return this;
    }

    @Override
    String getSuccinctInfo() {
        return dataString;
    }

    @Override
    String getAnalyzedString() {
        return dataString + "\r\n";
    }
}
