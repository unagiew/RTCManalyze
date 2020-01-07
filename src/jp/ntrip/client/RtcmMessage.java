package jp.ntrip.client;

import java.util.Objects;

abstract class RtcmMessage {
    protected static final int HEADER_BYTE_LENGTH = 3;

    protected byte[] data;
    protected int messageType;

    public RtcmMessage(byte[] data, int index, int length) {
        this.data = new byte[length];
        System.arraycopy(data, index, this.data, 0, length);
    }

    /**
     * メッセージタイプ Getter
     * @return メッセージタイプ
     */
    public String getMessageType() {
        return Objects.toString(messageType);
    }

    /**
     * メッセージ内容デコード
     * @return メソッドーチェン用に自クラスをかえす
     */
    abstract RtcmMessage decode();

    /**
     * Information Tableにのせる情報項目を作成
     * @return 簡易データ文字列
     */
    abstract String getSuccinctInfo();

    /**
     * AnlyzedFlowにのせるデータ解析結果を作成
     * @return 解析結果文字列
     */
    abstract String getAnalyzedString();
}
