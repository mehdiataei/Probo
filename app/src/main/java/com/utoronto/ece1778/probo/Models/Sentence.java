package com.utoronto.ece1778.probo.Models;

import android.os.Parcel;
import android.os.Parcelable;

public class Sentence implements Parcelable {

    private String text;
    private String articleId;
    private String sentimentNumber;
    private String sentiment;
    private String startIndex;
    private String endIndex;



    public Sentence() {
    }

    public Sentence(String text, String articleId, String sentimentNumber, String sentiment,  String startIndex, String endIndex) {
        this.text = text;
        this.articleId = articleId;
        this.sentimentNumber = sentimentNumber;
        this.sentiment = sentiment;
        this.startIndex = startIndex;
        this.endIndex = endIndex;
    }

    protected Sentence(Parcel in) {
        text = in.readString();
        articleId = in.readString();
        sentimentNumber = in.readString();
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
        dest.writeString(sentimentNumber);
        dest.writeString(sentiment);
        dest.writeString(startIndex);
        dest.writeString(endIndex);

    }
}
