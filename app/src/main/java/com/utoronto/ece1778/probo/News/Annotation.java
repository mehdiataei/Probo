package com.utoronto.ece1778.probo.News;

import android.graphics.Color;
import android.text.style.BackgroundColorSpan;

public class Annotation {
    public static final String
        TYPE_HEADLINE = "headline",
        TYPE_BODY = "body";

    private String userId;
    private int startIndex;
    private int endIndex;
    private int value;

    public Annotation(String userId, int startIndex, int endIndex, int value) {
        this.userId = userId;
        this.startIndex = startIndex;
        this.endIndex = endIndex;
        this.value = value;
    }

    public String getUserId() {
        return this.userId;
    }

    public int getStartIndex() {
        return this.startIndex;
    }

    public int getEndIndex() {
        return this.endIndex;
    }

    public int getValue() {
        return this.value;
    }

    public BackgroundColorSpan getBackgroundColor() {
        return new BackgroundColorSpan(this.value == 1 ? Color.YELLOW : Color.RED);
    }
}
