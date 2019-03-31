package com.utoronto.ece1778.probo.News;

import android.support.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
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
    private String source;
    private HashMap<String, AnnotationVote> votes;
    private int upvoteCount;
    private int downvoteCount;
    private String heading;
    private String sentence;

    private boolean loaded;

    public Annotation(String id) {
        this.id = id;
        this.loaded = false;
    }

    public Annotation(String id, Article article, User user, String type, int startIndex, int endIndex, int value,
                      String comment, String source, HashMap<String, AnnotationVote> upvotes,
                      HashMap<String, AnnotationVote> downvotes, String heading, String sentence) {

        this.id = id;
        this.article = article;
        this.user = user;
        this.type = type;
        this.startIndex = startIndex;
        this.endIndex = endIndex;
        this.value = value;
        this.comment = comment;
        this.source = source;

        this.votes = new HashMap<>();
        this.votes.putAll(upvotes);
        this.votes.putAll(downvotes);

        this.upvoteCount = upvotes.size();
        this.downvoteCount = downvotes.size();
        this.heading = heading;
        this.sentence = sentence;

        this.loaded = true;
    }


    public String getHeading() {
        return heading;
    }

    public void setHeading(String heading) {
        this.heading = heading;
    }

    public String getSentence() {
        return sentence;
    }

    public void setSentence(String sentence) {
        this.sentence = sentence;
    }

    public String getId() {
        return this.id;
    }

    public Article getArticle() {
        return this.article;
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

    public String getSource() {
        return this.source;
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

    public boolean hasLoaded() {
        return this.loaded;
    }

    public int getScore() {
        int externalUpvoteCount = this.userHasUpvoted(this.user) ?
                this.upvoteCount - 1 : this.upvoteCount;
        int externalDownvoteCount = this.userHasDownvoted(this.user) ?
                this.downvoteCount - 1 : this.downvoteCount;

        if ((externalUpvoteCount + externalDownvoteCount) == 0) {
            return 0;
        }

        return Math.round(((float) externalUpvoteCount / (externalUpvoteCount + externalDownvoteCount)) * Math.abs((float) this.value / 50) * 100);
    }

    public void load(final AnnotationCallback cb) {
        final FirebaseFirestore db = FirebaseFirestore.getInstance();

        this.loaded = false;

        db.collection("annotations")
                .document(this.id)
                .get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(final DocumentSnapshot annotationDocumentSnapshot) {
                        db.collection("annotation_votes")
                                .whereEqualTo("annotationId", id)
                                .get()
                                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                                    @Override
                                    public void onSuccess(QuerySnapshot votesDocumentSnapshots) {
                                        Long longStartIndex = annotationDocumentSnapshot.getLong("startIndex");
                                        Long longEndIndex = annotationDocumentSnapshot.getLong("endIndex");
                                        Long longValue = annotationDocumentSnapshot.getLong("value");

                                        article = new Article(annotationDocumentSnapshot.getString("articleId"));
                                        user = new User(annotationDocumentSnapshot.getString("userId"));
                                        type = annotationDocumentSnapshot.getString("type");
                                        startIndex = longStartIndex.intValue();
                                        endIndex = longEndIndex.intValue();
                                        value = longValue.intValue();
                                        comment = annotationDocumentSnapshot.getString("comment");
                                        source = annotationDocumentSnapshot.getString("source");

                                        votes = new HashMap<>();
                                        HashMap<String, AnnotationVote> upvotes = new HashMap<>();
                                        HashMap<String, AnnotationVote> downvotes = new HashMap<>();

                                        for (DocumentSnapshot votesDocumentSnapshot : votesDocumentSnapshots) {
                                            AnnotationVote vote = new AnnotationVote(
                                                    votesDocumentSnapshot.getId(),
                                                    new User(votesDocumentSnapshot.getString("userId")),
                                                    votesDocumentSnapshot.getBoolean("value")
                                            );

                                            if (vote.getValue()) {
                                                upvotes.put(
                                                        votesDocumentSnapshot.getString("userId"),
                                                        vote
                                                );
                                            } else {
                                                downvotes.put(
                                                        votesDocumentSnapshot.getString("userId"),
                                                        vote
                                                );
                                            }
                                        }

                                        votes.putAll(upvotes);
                                        votes.putAll(downvotes);

                                        upvoteCount = upvotes.size();
                                        downvoteCount = downvotes.size();

                                        loaded = true;

                                        cb.onLoad();
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        cb.onError(e);
                                    }
                                });
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        cb.onError(e);
                    }
                });
    }

    public void save(final AnnotationSubmitCallback cb, Article article) {
        final Annotation currentAnnotation = this;
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        Map<String, Object> newAnnotation = new HashMap<>();

        newAnnotation.put("articleId", article.getId());
        newAnnotation.put("userId", this.user.getUid());
        newAnnotation.put("startIndex", this.startIndex);
        newAnnotation.put("endIndex", this.endIndex);
        newAnnotation.put("value", this.value);
        newAnnotation.put("type", this.type);
        newAnnotation.put("comment", this.comment);
        newAnnotation.put("source", this.source);
        newAnnotation.put("heading", article.getHeading());
        newAnnotation.put("sentence", this.sentence);
        newAnnotation.put("timestamp", System.currentTimeMillis());

        db.collection("annotations")
                .add(newAnnotation)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        currentAnnotation.id = documentReference.getId();
                        cb.onSubmit(currentAnnotation);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        cb.onError(e);
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

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof Annotation)) {
            return false;
        }

        Annotation otherAnnotation = (Annotation) object;

        return this.id.equals(otherAnnotation.getId());
    }

    public interface AnnotationCallback {
        void onLoad();
        void onError(Exception e);
    }

    public interface AnnotationSourceCheckerCallback {

        void onChecked();

        void onSourceError();

        void onError(Exception e);

    }

    public interface AnnotationSubmitCallback {
        void onSubmit(Annotation annotation);
        void onAnnotationError(int errorCode);
        void onError(Exception e);
    }

    public interface NotificationCallback {
        void onNotified();
        void onError(Exception e);
    }
}
