package com.utoronto.ece1778.probo.News;

import android.graphics.Color;
import android.support.annotation.NonNull;
import android.text.style.BackgroundColorSpan;
import android.util.Log;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.utoronto.ece1778.probo.Login.User;

import java.util.HashMap;
import java.util.Map;

public class Annotation {
    public static final String
        TYPE_HEADLINE = "headline",
        TYPE_BODY = "body";

    private User user;
    private String type;
    private int startIndex;
    private int endIndex;
    private int value;
    private String comment;

    public Annotation(User user, String type, int startIndex, int endIndex, int value, String comment) {
        this.user = user;
        this.type = type;
        this.startIndex = startIndex;
        this.endIndex = endIndex;
        this.value = value;
        this.comment = comment;
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
}

interface AnnotationSubmitCallback {
    void onSubmit();
    void onAnnotationError(int errorCode);
    void onError(Exception e);
}