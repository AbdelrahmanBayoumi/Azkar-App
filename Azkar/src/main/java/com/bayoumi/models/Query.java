package com.bayoumi.models;

public class Query {
    private final String key;
    private final String value;

    public Query(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }
}
