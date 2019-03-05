package com.utoronto.ece1778.probo.News;

import android.graphics.Color;
import android.support.annotation.NonNull;
import android.text.SpannableString;
import android.text.style.BackgroundColorSpan;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.utoronto.ece1778.probo.Login.User;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class Article {
    private String id;
    private String author;
    private String imageUrl;
    private String headline;
    private String description;
    private String body;
    private Date datetime;

    private ArrayList<Annotation> headlineAnnotations;
    private ArrayList<Annotation> bodyAnnotations;

    public static final int
            ARTICLE_ERROR_NOT_FOUND = 0;

    public Article(String id) {
        this.id = id;

        this.headlineAnnotations = new ArrayList<>();
        this.bodyAnnotations = new ArrayList<>();
    }

    public String getId() {
        return this.id;
    };

    public String getAuthor() {
        return this.author;
    };

    public String getImageUrl() {
        return this.imageUrl;
    }

    public SpannableString getHeadline() {
        return Article.addAnnotations(
                this.headline,
                headlineAnnotations
        );
    }

    public String getDescription() {
        return this.description;
    }

    public SpannableString getBody() {
        return Article.addAnnotations(
                this.body.replace("\\n", System.getProperty("line.separator")),
                bodyAnnotations
        );
    }

    public Date getDatetime() {
        return this.datetime;
    }

    public void addHeadlineAnnotation(User user, int startIndex, int endIndex, int value, String comment) {
        if (startIndex >= endIndex) {
            return;
        }

        Annotation annotation = new Annotation(
                user,
                Annotation.TYPE_HEADLINE,
                startIndex,
                endIndex,
                value,
                comment
        );

        this.headlineAnnotations.add(annotation);
        annotation.save(this);
    }

    public void addBodyAnnotation(User user, int startIndex, int endIndex, int value, String comment) {
        if (startIndex >= endIndex) {
            return;
        }

        Annotation annotation = new Annotation(
                user,
                Annotation.TYPE_BODY,
                startIndex,
                endIndex,
                value,
                comment
        );

        this.bodyAnnotations.add(annotation);
        annotation.save(this);
    }

    private static SpannableString addAnnotations(String original, ArrayList<Annotation> annotations) {
        SpannableString str = new SpannableString(original);

        for (Annotation annotation: annotations) {
            str.setSpan(
                    annotation.getBackgroundColor(),
                    annotation.getStartIndex(),
                    annotation.getEndIndex(),
                    0
            );
        }

        return str;
    }

    public void load(final ArticleCallback cb) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("news")
                .document(this.id)
                .get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(final DocumentSnapshot documentSnapshot) {
                        if (!documentSnapshot.exists()) {
                            cb.onArticleError(Article.ARTICLE_ERROR_NOT_FOUND);
                            return;
                        }

                        ArticleAnnotationCallback annotationCb = new ArticleAnnotationCallback() {
                            @Override
                            public void onLoad() {
                                author = documentSnapshot.getString("author");
                                imageUrl = documentSnapshot.getString("image_url");
                                headline = documentSnapshot.getString("heading");
                                description = documentSnapshot.getString("description");
                                body = documentSnapshot.getString("body");

                                try {
                                    datetime = new SimpleDateFormat("yyyyMMdd_HHmmss").parse(documentSnapshot.getString("date_created"));
                                } catch (Exception e) {
                                    cb.onError(e);
                                    return;
                                }

                                cb.onLoad();
                            }

                            @Override
                            public void onError(Exception e) {
                                cb.onError(e);
                            }
                        };

                        loadAnnotations(annotationCb);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        cb.onError(e);
                    }
                });
    }

    private void loadAnnotations(final ArticleAnnotationCallback cb) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("annotations")
                .whereEqualTo("articleId", this.id)
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        for (DocumentSnapshot snapshot: queryDocumentSnapshots) {
                            String type = snapshot.getString("type");
                            Object startObj = snapshot.get("startIndex");
                            Object endObj = snapshot.get("endIndex");
                            Object valueObj = snapshot.get("value");
                            String userId = snapshot.getString("userId");
                            String comment = snapshot.getString("comment");

                            if (type != null &&
                                    startObj != null &&
                                    endObj != null &&
                                    valueObj != null &&
                                    userId != null) {

                                Long start = (Long) startObj;
                                Long end = (Long) endObj;
                                Long value = (Long) valueObj;

                                Annotation annotation = new Annotation(
                                        new User(userId),
                                        type,
                                        start.intValue(),
                                        end.intValue(),
                                        value.intValue(),
                                        comment
                                );

                                switch (type) {
                                    case "headline":
                                        headlineAnnotations.add(annotation);
                                        break;
                                    case "body":
                                        bodyAnnotations.add(annotation);
                                        break;
                                    default:
                                        break;
                                }
                            }
                        }
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
}

interface ArticleCallback {
    void onLoad();
    void onArticleError(int errorCode);
    void onError(Exception e);
}

interface ArticleAnnotationCallback {
    void onLoad();
    void onError(Exception e);
}