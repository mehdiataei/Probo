package com.utoronto.ece1778.probo.User;

import com.utoronto.ece1778.probo.News.Annotation;

import java.util.Date;

public class Subscription {
    private User user;
    private Annotation annotation;
    private Date date;

    private enum SubscriptionTypes {
        USER,
        ANNOTATION
    };

    private SubscriptionTypes subscriptionType;

    Subscription(User user) {
        this.user = user;

        this.subscriptionType = SubscriptionTypes.USER;
    }

    Subscription(Annotation annotation, Date date) {
        this.annotation = annotation;
        this.date = date;

        this.subscriptionType = SubscriptionTypes.ANNOTATION;
    }

    public User getUser() {
        return this.user;
    }

    public Annotation getAnnotation() {
        return this.annotation;
    }

    public Date getDate() {
        return this.date;
    }

    public String getTopic() {
        if (this.subscriptionType == SubscriptionTypes.USER) {
            return this.user.getUid();
        }

        return this.annotation.getArticle().getId() + "-" +
                this.annotation.getType() + "-" +
                this.annotation.getStartIndex() + "-" +
                this.annotation.getEndIndex();
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
