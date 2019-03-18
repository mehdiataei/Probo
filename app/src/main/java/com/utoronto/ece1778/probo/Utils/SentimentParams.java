package com.utoronto.ece1778.probo.Utils;

public class SentimentParams {
    String body;
    String articleId;

    public SentimentParams(String body, String articleId) {
        this.body = body;
        this.articleId = articleId;
    }


    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getArticleId() {
        return articleId;
    }

    public void setArticleId(String articleId) {
        this.articleId = articleId;
    }
}