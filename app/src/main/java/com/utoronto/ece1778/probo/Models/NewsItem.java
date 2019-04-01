package com.utoronto.ece1778.probo.Models;

import android.os.Parcel;
import android.os.Parcelable;

public class NewsItem implements Parcelable {


    private String heading;
    private String description;
    private String image_url;
    private String body;
    private String news_id;
    private String author;
    private String date_created;
    private String confidence_score;


    public NewsItem() {
    }

    protected NewsItem(Parcel in) {
        heading = in.readString();
        description = in.readString();
        image_url = in.readString();
        body = in.readString();
        news_id = in.readString();
        author = in.readString();
        date_created = in.readString();

    }

    public static final Creator<NewsItem> CREATOR = new Creator<NewsItem>() {
        @Override
        public NewsItem createFromParcel(Parcel in) {
            return new NewsItem(in);
        }

        @Override
        public NewsItem[] newArray(int size) {
            return new NewsItem[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    public NewsItem(String heading, String description, String image_url, String body, String news_id, String author, String confidence_score) {
        this.heading = heading;
        this.description = description;
        this.image_url = image_url;
        this.body = body;
        this.news_id = news_id;
        this.author = author;
        this.confidence_score = confidence_score;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {

        dest.writeString(heading);
        dest.writeString(description);
        dest.writeString(image_url);
        dest.writeString(body);
        dest.writeString(news_id);
        dest.writeString(author);
        dest.writeString(date_created);
        dest.writeString(confidence_score);

    }

    @Override
    public String toString() {
        return "NewsItem{" +
                "heading='" + heading + '\'' +
                ", description='" + description + '\'' +
                ", image_url='" + image_url + '\'' +
                ", body='" + body + '\'' +
                ", news_id='" + news_id + '\'' +
                ", author='" + author + '\'' +
                ", date_created='" + date_created + '\'' +
                ", confidence_score='" + confidence_score + '\'' +
                '}';
    }


    public String getDate_created() {
        return date_created;
    }

    public void setDate_created(String date_created) {
        this.date_created = date_created;
    }

    public String getHeading() {
        return heading;
    }

    public void setHeading(String heading) {
        this.heading = heading;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getImage_url() {
        return image_url;
    }

    public void setImage_url(String image_url) {
        this.image_url = image_url;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getNews_id() {
        return news_id;
    }

    public void setNews_id(String news_id) {
        this.news_id = news_id;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getConfidence_score() {
        return confidence_score;
    }

    public void setConfidence_score(String confidence_score) {
        this.confidence_score = confidence_score;
    }
}
