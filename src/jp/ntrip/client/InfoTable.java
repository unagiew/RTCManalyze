package jp.ntrip.client;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class InfoTable {
    private StringProperty item = new SimpleStringProperty();
    private StringProperty value = new SimpleStringProperty();
    private IntegerProperty times = new SimpleIntegerProperty();

    public InfoTable(String item, String value, int times) {
        this.item.set(item);
        this.value.set(value);
        this.times.set(times);
    }

    public InfoTable(String item, String value) {
        this(item, value, 1);
    }

    // getter & setter
    public String getItem() {
        return item.get();
    }
    public void setItem(String item) {
        this.item.set(item);
    }
    public String getValue() {
        return value.get();
    }
    public void setValue(String value) {
        this.value.set(value);
    }
    public int getTimes() {
        return times.get();
    }
    public void setTimes(int times) {
        this.times.set(times);
    }

    // javafx編集通知用
    public StringProperty itemProperty() {
        return item;
    }
    public StringProperty valueProperty() {
        return value;
    }
    public IntegerProperty timesProperty() {
        return times;
    }
}
