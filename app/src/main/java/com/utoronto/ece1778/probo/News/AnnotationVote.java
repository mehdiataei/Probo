package com.utoronto.ece1778.probo.News;

import com.utoronto.ece1778.probo.User.User;

public class AnnotationVote {
    private String id;
    private User user;
    private boolean value;

    public AnnotationVote(String id, User user, boolean value) {
        this.id = id;
        this.user = user;
        this.value = value;
    }

    public String getId() {
        return this.id;
    }

    public User getUser() {
        return this.user;
    }

    public boolean getValue() {
        return this.value;
    }

    public interface AnnotationVoteCallback {
        void onSubmit(boolean hasVote, boolean value, int upvoteCount, int downvoteCount);
        void onError(Exception e);
    }
}
