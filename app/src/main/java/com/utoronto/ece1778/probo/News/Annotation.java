package com.utoronto.ece1778.probo.News;

import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.utoronto.ece1778.probo.User.User;

import java.util.HashMap;
import java.util.Map;

public class Annotation {
    public static final String
        TYPE_HEADLINE = "headline",
        TYPE_BODY = "body";

    private String id;
    private Article article;
    private User user;
    private String type;
    private int startIndex;
    private int endIndex;
    private int value;
    private String comment;
    private HashMap<String, AnnotationVote> votes;
    private int upvoteCount;
    private int downvoteCount;

    public Annotation(String id, Article article, User user, String type, int startIndex, int endIndex, int value,
                      String comment, HashMap<String, AnnotationVote> upvotes,
                      HashMap<String, AnnotationVote> downvotes) {

        this.id = id;
        this.article = article;
        this.user = user;
        this.type = type;
        this.startIndex = startIndex;
        this.endIndex = endIndex;
        this.value = value;
        this.comment = comment;

        this.votes = new HashMap<>();
        this.votes.putAll(upvotes);
        this.votes.putAll(downvotes);

        this.upvoteCount = upvotes.size();
        this.downvoteCount = downvotes.size();
    }

    public String getId() {
        return this.id;
    }

    public User getUser() {
        return this.user;
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

    public int getValue() {
        return this.value;
    }

    public String getComment() {
        return this.comment;
    }

    public int getUpvoteCount() {
        return this.upvoteCount;
    }

    public int getDownvoteCount() {
        return this.downvoteCount;
    }

    public boolean userHasUpvoted(User user) {
        return this.votes.containsKey(user.getUid()) && this.votes.get(user.getUid()).getValue();
    }

    public boolean userHasDownvoted(User user) {
        return this.votes.containsKey(user.getUid()) && !this.votes.get(user.getUid()).getValue();
    }

    public void save(final AnnotationSubmitCallback cb, Article article) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        Map<String, Object> newAnnotation = new HashMap<>();

        newAnnotation.put("articleId", article.getId());
        newAnnotation.put("userId", this.user.getUid());
        newAnnotation.put("startIndex", this.startIndex);
        newAnnotation.put("endIndex", this.endIndex);
        newAnnotation.put("value", this.value);
        newAnnotation.put("type", this.type);
        newAnnotation.put("comment", this.comment);
        newAnnotation.put("timestamp", System.currentTimeMillis());

        db.collection("annotations")
                .add(newAnnotation)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        cb.onSubmit();
                        Log.d("PROBO_APP", "added annotation");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        cb.onError(e);
                        Log.d("PROBO_APP", "err", e);
                    }
                });
    }

    public void vote(final AnnotationVote.AnnotationVoteCallback cb, final User user, final boolean value) {
        AnnotationVote.AnnotationVoteCallback removeCb = new AnnotationVote.AnnotationVoteCallback() {
            @Override
            public void onSubmit(boolean hadVote, boolean oldValue, int numUpvotes, int numDownvotes) {
                if (hadVote && oldValue == value) {
                    cb.onSubmit(false, oldValue, upvoteCount, downvoteCount);
                    return;
                }

                FirebaseFirestore db = FirebaseFirestore.getInstance();

                Map<String, Object> annotationVote = new HashMap<>();

                annotationVote.put("userId", user.getUid());
                annotationVote.put("annotationId", id);
                annotationVote.put("articleId", article.getId());
                annotationVote.put("value", value);
                annotationVote.put("timestamp", System.currentTimeMillis());

                db.collection("annotation_votes")
                        .add(annotationVote)
                        .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                            @Override
                            public void onSuccess(DocumentReference documentReference) {
                                AnnotationVote vote = new AnnotationVote(
                                        documentReference.getId(),
                                        user,
                                        value
                                );

                                votes.put(user.getUid(), vote);

                                if (value) {
                                    upvoteCount++;
                                } else {
                                    downvoteCount++;
                                }

                                cb.onSubmit(true, value, upvoteCount, downvoteCount);
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                cb.onError(e);
                            }
                        });
            }

            @Override
            public void onError(Exception e) {
                cb.onError(e);
            }
        };

        removeVote(removeCb, user);
    }

    private void removeVote(final AnnotationVote.AnnotationVoteCallback cb, final User user) {
        if (!this.votes.containsKey(user.getUid())) {
            cb.onSubmit(false, false, upvoteCount, downvoteCount);
            return;
        }

        final AnnotationVote vote = this.votes.get(user.getUid());

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("annotation_votes")
                .document(vote.getId())
                .delete()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        votes.remove(user.getUid());

                        if (vote.getValue()) {
                            upvoteCount--;
                        } else {
                            downvoteCount--;
                        }

                        cb.onSubmit(true, vote.getValue(), upvoteCount, downvoteCount);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        cb.onError(e);
                    }
                });
    }
}

interface AnnotationSubmitCallback {
    void onSubmit();
    void onAnnotationError(int errorCode);
    void onError(Exception e);
}
