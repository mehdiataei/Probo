package com.utoronto.ece1778.probo.Models;

import android.os.Parcel;
import android.os.Parcelable;

public class Sentence implements Parcelable {

    private String text;
    private String articleId;
    private String sentiment;
    private String startIndex;
    private String endIndex;



    public Sentence() {
    }

    public Sentence(String text, String articleId, String sentiment,  String startIndex, String endIndex) {
        this.text = text;
        this.articleId = articleId;
        this.sentiment = sentiment;
        this.startIndex = startIndex;
        this.endIndex = endIndex;
    }

    protected Sentence(Parcel in) {
        text = in.readString();
        articleId = in.readString();
        sentiment = in.readString();
        startIndex = in.readString();
        endIndex = in.readString();

    }

    public static final Parcelable.Creator<Sentence> CREATOR = new Parcelable.Creator<Sentence>() {
        @Override
        public Sentence createFromParcel(Parcel in) {
            return new Sentence(in);
        }

        @Override
        public Sentence[] newArray(int size) {
            return new Sentence[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {

        dest.writeString(text);
        dest.writeString(articleId);
        dest.writeString(sentiment);
        dest.writeString(startIndex);
        dest.writeString(endIndex);

    }


    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getArticleId() {
        return articleId;
    }

    public void setArticleId(String articleId) {
        this.articleId = articleId;
    }

    public String getSentiment() {
        return sentiment;
    }

    public void setSentiment(String sentiment) {
        this.sentiment = sentiment;
    }

    public String getStartIndex() {
        return startIndex;
    }

    public void setStartIndex(String startIndex) {
        this.startIndex = startIndex;
    }

    public String getEndIndex() {
        return endIndex;
    }

    public void setEndIndex(String endIndex) {
        this.endIndex = endIndex;
    }

    public static Creator<Sentence> getCREATOR() {
        return CREATOR;
    }
}
