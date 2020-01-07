package jp.ntrip.client;

public class HttpResponse {
    private static final int BUFFERLENGTH = 4096;
    private byte[] byteData = new byte[BUFFERLENGTH];
    private int dataLength;
    private byte[] binaryData;

    public HttpResponse() {
        super();
    }

    /**
     * ソケットで受信したデータを追加する
     * @param receiveData 受信データ
     * @param length 受信データ長
     */
    public void addData(byte[] receiveData, int length) {
        // 受信データがバッファより大きくなった場合に拡張する
        if((dataLength + length) > byteData.length) {
            int tmpLength = byteData.length;
            while(tmpLength < dataLength + length) {
                tmpLength += BUFFERLENGTH;
            }
            byte[] tmp = new byte[tmpLength];
            System.arraycopy(byteData, 0, tmp, 0, dataLength);
            byteData = tmp;
        }
        System.arraycopy(receiveData, 0, byteData, dataLength, length);
        dataLength += length;
    }

    /**
     * HTTPの終端を確認する
     * @return HTTPが終端した場合true
     */
    public boolean checkHttpEnd() {
        int index;

        byte[] target = "ICY 200 OK\r\n".getBytes();
        if((index = Utilities.indexOf(byteData, dataLength, target, 0)) != -1) {
            index += target.length;
            if(index != dataLength) {
                binaryData = new byte[dataLength - index];
                System.arraycopy(byteData, index, binaryData, 0, dataLength - index);
                dataLength = index;
            }
            return true;
        }

        return false;
    }

    public boolean hasBinaryData() {
        return binaryData != null;
    }

    public byte[] getBinaryData() {
        return binaryData;
    }

    public int getBinaryLength() {
        return binaryData.length;
    }

    public String getHttpData() {
        return new String(byteData, 0, dataLength) + "\r\n";
    }

    public String getStatusLine() {
        int statusLineEnd = Utilities.indexOf(byteData, dataLength, "\r\n".getBytes(), 0) + 2;
        return new String(byteData, 0, statusLineEnd);
    }
}
