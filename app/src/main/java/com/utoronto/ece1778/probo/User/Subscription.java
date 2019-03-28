package com.utoronto.ece1778.probo.User;

import com.utoronto.ece1778.probo.News.Article;

import java.util.ArrayList;
import java.util.Date;

public class Subscription {
    private Article article;
    private String type;
    private int startIndex;
    private int endIndex;
    private Date date;

    Subscription(Article article, String type, int startIndex, int endIndex, Date date) {
        this.article = article;
        this.type = type;
        this.startIndex = startIndex;
        this.endIndex = endIndex;
        this.date = date;
    }

    public Article getArticle() {
        return this.article;
    }

    public String getType() {
        return this.type;
    }

    public int getStartIndex() {
        return this.startIndex;
    }

    public int getEndIndex() {
        return this.endIndex;
    }

    public Date getDate() {
        return this.date;
    }

    public String getTopic() {
        return this.article.getId() + "-" + this.startIndex + "-" + this.endIndex;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof Subscription)) {
            return false;
        }

        Subscription otherSubscription = (Subscription) object;

        return this.getTopic().equals(otherSubscription.getTopic());
    }
}
