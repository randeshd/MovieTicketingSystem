package com.movieticketing.model;

import java.time.LocalDateTime;

public class Setting {

    public static final String HALF_TICKET_PERCENT = "HALF_TICKET_PERCENT";

    private String key;
    private String value;
    private LocalDateTime updatedAt;

    public Setting(String key, String value, LocalDateTime updatedAt) {
        this.key = key;
        this.value = value;
        this.updatedAt = updatedAt;
    }

    public double getValueAsDouble() {
        return Double.parseDouble(value);
    }

    public String getKey() { return key; }
    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}