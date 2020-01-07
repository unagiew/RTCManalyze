package jp.ntrip.client;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * JAVA Propertiesのラッパークラス<br>
 * propertiesの書式  key=value,value,...
 */
public class PropertiesUtil {
    private String filePath;
    private Properties prop = new Properties();

    public PropertiesUtil(String filePath) {
        this.filePath = filePath;
    }

    /**
     * ファイルからPropertiesを読み込む。<br>
     * 指定ファイルが存在しない場合は、同ファイルを新たに作成する。
     */
    public void readPropertiesFile() {
        File file = new File(filePath);

        try {
            if(!file.createNewFile()) {
                System.out.println("Property file already exists.");
                prop.load(Files.newBufferedReader(Paths.get(filePath)));
            }
        } catch (IOException e) {
            // エラー処理を追加
            e.printStackTrace();
        }
    }

    /**
     * Propertiesの内容をファイルに書き込む。
     */
    public void writeProperties() {
        try (PrintWriter writer = new PrintWriter(new File(filePath))) {
            prop.store(writer, "NTRIP Client properties");
        } catch (IOException e) {
            // エラー処理を追加
            e.printStackTrace();
        }
    }

    /**
     * keyに対するvalueのListを返す。<br>
     * keyがマッチしない場合は要素0のListを返す。
     * @param key propertiesのkey
     * @return List valueのリスト
     */
    public List<String> getProperties(String key) {
        String[] properties = prop.getProperty(key, "").split(",", 0);
        if(properties[0].equals("")) {
            return new ArrayList<>();
        }
        return new ArrayList<>(Arrays.asList(properties));
    }

    /**
     * valueをカンマ区切りの文字列にし、keyに対し設定する<br>
     * keyおよびvalueがnull、value数が0であった場合はそのままreturn
     * @param key 検索キー
     * @param values 検索キーに対する値のリスト
     */
    public void setProperties(String key, List<String> values) {
        if(key == null || values == null || values.size() <= 0) {
            return;
        }

        StringBuilder dstValues = new StringBuilder();
        for(String value : values) {
            dstValues.append(value).append(",");
        }
        prop.setProperty(key, dstValues.deleteCharAt(dstValues.length() - 1).toString());
    }

    /**
     * keyにマッチするvalueを返す<br>
     * keyとvalueが1対1の場合に使用。1対Nの場合はgetPropertiesを使用。
     * @param key 検索キー
     * @return value
     */
    public String getProperty(String key) {
        return prop.getProperty(key, "");
    }

    /**
     * propertiesを設定する<br>
     * keyとvalueが1対1の場合に使用。1対Nの場合はsetPropertiesを使用。
     * @param key 設定キー
     * @param value 値
     */
    public void setProperty(String key, String value) {
        prop.setProperty(key, value);
    }
}