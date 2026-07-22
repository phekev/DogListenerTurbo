package com.example.doglistener.audio;

public enum ResponseLevel {

    FIRST("response_one"),
    SECOND("response_two"),
    PROLONGED("response_three");

    private final String directoryName;

    ResponseLevel(String directoryName) {
        this.directoryName = directoryName;
    }

    public String getDirectoryName() {
        return directoryName;
    }
}