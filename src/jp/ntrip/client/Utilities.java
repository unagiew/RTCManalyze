package jp.ntrip.client;

import java.util.Formatter;

public class Utilities {
    public static final int INDEX_NOT_FOUND = -1;

    /**
     * byte配列に対し検査値を検索し存在する場合はindexを返す<br>
     * @param dstArray  検索先配列
     * @param toFind  検索値
     * @return  先頭インデックス<br>
     * 見つからない場合および配列がnullの場合は{@code -1}<br>
     */
    public static int indexOf(final byte[] dstArray, final byte[] toFind) {
        return indexOf(dstArray, dstArray.length, toFind, 0);
    }

    /**
     * byte配列に対し検査値を検索し存在する場合はindexを返す<br>
     * 検索開始インデックスが0より小さい場合は0とする<br>
     * 検索先配列長が配列より長い場合は配列長とする
     *
     * @param dstArray  検索先配列
     * @param dstArrayLength  検索先配列長。配列長以下でも可。
     * @param toFind  検索値
     * @param startIndex  検索開始インデックス
     * @return 先頭インデックス<br>
     * 見つからない場合および配列がnullの場合は{@code -1}<br>
     */
    public static int indexOf(final byte[] dstArray, int dstArrayLength, final byte[] toFind, int startIndex) {
        if(dstArray == null || toFind == null) {
            return INDEX_NOT_FOUND;
        }
        if(startIndex < 0) {
            startIndex = 0;
        }
        if(dstArrayLength > dstArray.length) {
            dstArrayLength = dstArray.length;
        }

        for(int i = startIndex; i < dstArrayLength - toFind.length + 1; i++) {
            boolean found = true;
            for(int j = 0; j < toFind.length; j++) {
                if(dstArray[i + j] != toFind[j]) {
                    found = false;
                    break;
                }
            }
            if(found) {
                return i;
            }
        }
        return INDEX_NOT_FOUND;
    }

    /**
     * バイト配列を16進数ダンプの文字列として返す<br>
     * 出力範囲長が配列長を超える場合は配列長とする
     * @param data  バイト配列
     * @param length  出力範囲長
     * @return  16進数およびアスキーの文字列<br>
     * 配列がnullの場合は{@code "\r\n"}
     */
    public static String hexPrint(final byte[] data, int length) {
        if(data == null) {
            return "\r\n";
        }
        if(length > data.length) {
            length = data.length;
        }

        int letters = 0;	//文字数カウント
        StringBuilder binarySB = new StringBuilder();
        StringBuilder asciiSB = new StringBuilder();
        StringBuilder result = new StringBuilder();
        Formatter formatter = new Formatter(binarySB);

        while(letters < length) {
            formatter.format("%04x   ", letters);
            for(int column = 0; column < 16; column++, letters++) {
                if(letters < length) {
                    formatter.format("%02x ", data[letters]);
                    if(data[letters] >= 0x20 && data[letters] <= 0x7e) {
                        asciiSB.append((char)data[letters]);
                    } else {
                        asciiSB.append('.');
                    }
                } else {
                    binarySB.append("   ");
                }
                if(column == 7) {
                    binarySB.append(" ");
                    asciiSB.append(" ");
                }
            }
            result.append(binarySB).append(" ").append(asciiSB).append("\r\n");
            binarySB.setLength(0);
            asciiSB.setLength(0);
        }

        formatter.close();
        return result.append("\r\n").toString();
    }

    /**
     * バイト配列の指定ビット位置から指定ビット長の値を返す
     * @param data  対象配列
     * @param bitPosition  配列先頭からのビット位置 {@code target[2]}の5ビット目ならば13
     * @param bitLength  計算対象ビット長  32ビット以下
     * @return  対象ビットの値(signed)<br>
     * 対象配列がnullおよびビット長が32を超えた場合は{@code 0}
     */
    public static int bitToInt(final byte[] data, int bitPosition, int bitLength) {
        if(data == null || bitLength > 32) {
            return 0;
        }
        int sum = 0;
        for(int i = bitPosition; i < bitPosition + bitLength; i++) {
            sum = (sum << 1) + ((data[i / 8] >> (7 - i % 8)) & 0x01);
        }
        return sum;
    }

    /**
     * バイト配列の指定ビット位置から指定ビット長の値を返す
     * @param data  対象配列
     * @param bitPosition  配列先頭からのビット位置
     * @param bitLength  計算対象ビット長  64ビット以下
     * @return  対象ビットの値(signed)<br>
     * 対象配列がnullおよびビット長が64を超えた場合は{@code 0}
     */
    public static long bitToLong(byte[] data, int bitPosition, int bitLength) {
        if(data == null || bitLength > 64) {
            return 0;
        }
        long sum = 0;
        for(int i = bitPosition; i < bitPosition + bitLength; i++) {
            sum = (sum << 1) + ((data[i / 8] >> (7 - i % 8)) & 0x01);
        }
        return sum;
    }

    /**
     * バイト配列の指定ビット位置から指定ビット長の値を返す<br>
     * 対象ビットの最上位ビットを符号として扱う
     * @param data  対象配列
     * @param bitPosition  配列先頭からのビット位置
     * @param bitLength  計算対象ビット長  64ビット以下
     * @return  対象ビットの値(signed)<br>
     * 対象配列がnullおよびビット長が64を超えた場合は{@code 0}
     */
    public static long bitToLongMSB(byte[] data, int bitPosition, int bitLength) {
        if(data == null || bitLength > 64) {
            return 0;
        }
        long sum = bitToLong(data, bitPosition, bitLength);
        if(((data[bitPosition / 8] >> (7 - bitPosition % 8)) & 0x01) == 1) {
            long mask = -1L << bitLength;
            sum |= mask;
        }
        return sum;
    }
}
