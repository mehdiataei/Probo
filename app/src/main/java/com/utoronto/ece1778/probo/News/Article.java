package com.utoronto.ece1778.probo.News;

import android.animation.ArgbEvaluator;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.v4.graphics.ColorUtils;
import android.text.SpannableString;
import android.text.style.BackgroundColorSpan;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.utoronto.ece1778.probo.Login.User;
import com.utoronto.ece1778.probo.Utils.Tuple;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class Article {

    public static final String TAG = "ARTICLE";

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
    }

    ;

    public String getAuthor() {
        return this.author;
    }

    ;

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

    public void addHeadlineAnnotation(User user, int startIndex, int endIndex, int value, String comment, BackgroundColorSpan backgroundColorSpan) {
        if (startIndex >= endIndex) {
            return;
        }

        Annotation annotation = new Annotation(
                user,
                Annotation.TYPE_HEADLINE,
                startIndex,
                endIndex,
                value,
                comment,
                backgroundColorSpan
        );

        this.headlineAnnotations.add(annotation);
        annotation.save(this);
    }

    public void addBodyAnnotation(User user, int startIndex, int endIndex, int value, String comment, BackgroundColorSpan backgroundColorSpan) {
        if (startIndex >= endIndex) {
            return;
        }

        Annotation annotation = new Annotation(
                user,
                Annotation.TYPE_BODY,
                startIndex,
                endIndex,
                value,
                comment,
                backgroundColorSpan
        );

        this.bodyAnnotations.add(annotation);
        annotation.save(this);
    }

    private static SpannableString addAnnotations(String original, ArrayList<Annotation> annotations) {
        SpannableString str = new SpannableString(original);

        for (Annotation annotation : annotations) {
            str.setSpan(
                    annotation.getBackgroundColorSpan(),
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

                        // <startIndex, (numOfTrue, numOfFalse)>
                        HashMap<Integer, Tuple<Integer, Integer>> freq = new HashMap<>();

                        Integer totalNumOfAnnotations = 0;


                        for (DocumentSnapshot snapshot : queryDocumentSnapshots) {
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
                                Long value = (Long) valueObj;


                                Integer incTrue = value == 1 ? 1 : 0;
                                Integer incFalse = value == 0 ? 1 : 0;


                                Integer numOfTrue = freq.containsKey(start.intValue()) ? freq.get(start.intValue()).getX() : 0;
                                Integer numOfFalse = freq.containsKey(start.intValue()) ? freq.get(start.intValue()).getY() : 0;

                                freq.put(start.intValue(),
                                        new Tuple<>(numOfTrue + incTrue, numOfFalse + incFalse));

                                totalNumOfAnnotations++;
                            }
                        }


                        for (DocumentSnapshot snapshot : queryDocumentSnapshots) {
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


                                Integer numOfTrues = freq.get(start.intValue()).getX();
                                Integer numOfFalses = freq.get(start.intValue()).getY();
                                Integer total = numOfFalses + numOfTrues;

                                float interpolate = numOfTrues / (float) total;

                                Log.d(TAG, "onSuccess: numOfTrues: " + numOfTrues);
                                Log.d(TAG, "onSuccess: total: " + total);
                                Log.d(TAG, "onSuccess: totalNumOfAnnotations: " + totalNumOfAnnotations);


                                float alpha = total / (float) totalNumOfAnnotations;

                                Log.d(TAG, "onSuccess: alpha: " + alpha);

                                Log.d(TAG, "onSuccess: interpolate before: " + interpolate);
//                                interpolate = numOfTrues > numOfFalses ? interpolate : Math.abs(1 - interpolate);


                                int green = Color.GREEN;
                                int red = Color.RED;

                                int color = interpolateColor(red, green, interpolate);

                                int colorAlpha = ColorUtils.setAlphaComponent(color, Math.round(alpha * 255));

                                Log.d(TAG, "onSuccess: interpolate after: " + interpolate);


                                Log.d(TAG, "onSuccess: color: " + color);

                                Log.d(TAG, "onSuccess: red: " + red);
                                Log.d(TAG, "onSuccess: green: " + green);


                                BackgroundColorSpan backgroundColorSpan = new BackgroundColorSpan(colorAlpha);
                                int testColor = backgroundColorSpan.getBackgroundColor();
                                Log.d(TAG, "onSuccess: testColor: " + testColor);

                                Annotation annotation = new Annotation(
                                        new User(userId),
                                        type,
                                        start.intValue(),
                                        end.intValue(),
                                        value.intValue(),
                                        comment,
                                        backgroundColorSpan
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
                .

                        addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                cb.onError(e);
                            }
                        });
    }


    private float interpolate(float a, float b, float proportion) {
        return (a + ((b - a) * proportion));
    }

    /**
     * Returns an interpolated color, between <code>a</code> and <code>b</code>
     * proportion = 0, results in color a
     * proportion = 1, results in color b
     */
    private int interpolateColor(int a, int b, float proportion) {

        if (proportion > 1 || proportion < 0) {
            throw new IllegalArgumentException("proportion must be [0 - 1]");
        }
        float[] hsva = new float[3];
        float[] hsvb = new float[3];
        float[] hsv_output = new float[3];

        Color.colorToHSV(a, hsva);
        Color.colorToHSV(b, hsvb);
        for (int i = 0; i < 3; i++) {
            hsv_output[i] = interpolate(hsva[i], hsvb[i], proportion);
        }

        int alpha_a = Color.alpha(a);
        int alpha_b = Color.alpha(b);
        float alpha_output = interpolate(alpha_a, alpha_b, proportion);

        return Color.HSVToColor((int) alpha_output, hsv_output);
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