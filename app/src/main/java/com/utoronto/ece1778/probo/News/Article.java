package com.utoronto.ece1778.probo.News;

import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.v4.graphics.ColorUtils;
import android.text.SpannableString;
import android.text.style.BackgroundColorSpan;
import android.util.Log;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.utoronto.ece1778.probo.Login.User;
import com.utoronto.ece1778.probo.Utils.Tuple;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

public class Article {
    public static final String TAG = "ARTICLE";

    public static final int
            ARTICLE_ANNOTATION_VALID = -1,
            ARTICLE_ANNOTATION_ERROR_INTERNAL = 0,
            ARTICLE_ANNOTATION_ERROR_ALREADY_SUBMITTED = 1;

    private String id;
    private String author;
    private String imageUrl;
    private String headline;
    private String description;
    private String body;
    private Date datetime;

    private ArrayList<Annotation> headlineAnnotations;
    private ArrayList<Annotation> bodyAnnotations;

    private HashMap<String, ArrayList<Annotation>> annotationsMap;
    private HashMap<String, Tuple<Integer, Integer>> annotationStats;

    public static final int
            ARTICLE_ERROR_NOT_FOUND = 0;

    public Article(String id) {
        this.id = id;

        this.annotationsMap = new HashMap<>();
        this.annotationStats = new HashMap<>();

        this.headlineAnnotations = new ArrayList<>();
        this.bodyAnnotations = new ArrayList<>();
    }

    public String getId() {
        return this.id;
    }

    public String getAuthor() {
        return this.author;
    }

    public String getImageUrl() {
        return this.imageUrl;
    }

    public SpannableString getHeadline(boolean showHeatmap) {
        return showHeatmap ? this.getAnnotatedText(
                this.headline,
                this.headlineAnnotations,
                0
        ) : new SpannableString(this.headline);
    }

    public String getDescription() {
        return this.description;
    }

    public SpannableString getBody(boolean showHeatmap, int startIndex, int endIndex) {
        String formattedBody = this.body;
        int indexOffset = 0;

        if (startIndex != -1 && endIndex != -1) {
            formattedBody = this.body.substring(startIndex, endIndex);
            indexOffset = startIndex;
        }
        
        return showHeatmap ? this.getAnnotatedText(
                formattedBody,
                this.bodyAnnotations,
                indexOffset
        ) : new SpannableString(formattedBody);
    }

    public int getBodyLength() {
        return this.body.length();
    }

    public Date getDatetime() {
        return this.datetime;
    }

    public boolean annotationExists(String type, int startIndex, int endIndex) {
        String key = type + ":" + startIndex + ":" + endIndex;
        return this.annotationsMap.containsKey(key);
    }

    public ArrayList<Annotation> getLocatedAnnotations(String type, int startIndex, int endIndex) {
        String key = type + ":" + startIndex + ":" + endIndex;

        return this.annotationsMap.containsKey(key) ?
                this.annotationsMap.get(key) :
                new ArrayList<Annotation>();
    }

    private int checkNewAnnotation(User user, String type, int startIndex, int endIndex) {
        if (startIndex >= endIndex) {
            return Article.ARTICLE_ANNOTATION_ERROR_INTERNAL;
        }

        String annotationKey = type + ":" + startIndex + ":" + endIndex;
        ArrayList<Annotation> annotations = this.annotationsMap.containsKey(annotationKey) ?
                                            this.annotationsMap.get(annotationKey) :
                                            new ArrayList<Annotation>();

        int numAnnotations = annotations.size();

        for (int i=0; i<numAnnotations; i++) {
            if (annotations.get(i).getUser().getUid().equals(user.getUid())) {
                return Article.ARTICLE_ANNOTATION_ERROR_ALREADY_SUBMITTED;
            }
        }

        return Article.ARTICLE_ANNOTATION_VALID;
    }

    public void addHeadlineAnnotation(AnnotationSubmitCallback cb, User user, int startIndex, int endIndex, int value, String comment) {
        int errorCode = this.checkNewAnnotation(user, Annotation.TYPE_HEADLINE, startIndex, endIndex);

        if (errorCode != Article.ARTICLE_ANNOTATION_VALID) {
            cb.onAnnotationError(errorCode);
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
        this.addAnnotationMap(annotation);

        this.updateAnnotationCounts(annotation);

        annotation.save(cb, this);
    }

    public void addBodyAnnotation(AnnotationSubmitCallback cb, User user, int startIndex, int endIndex, int value, String comment) {
        int errorCode = this.checkNewAnnotation(user, Annotation.TYPE_BODY, startIndex, endIndex);

        if (errorCode != Article.ARTICLE_ANNOTATION_VALID) {
            cb.onAnnotationError(errorCode);
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
        this.addAnnotationMap(annotation);

        this.updateAnnotationCounts(annotation);

        annotation.save(cb, this);
    }

    public void addAnnotationMap(Annotation annotation) {
        String key = annotation.getType() + ":" + annotation.getStartIndex() + ":" + annotation.getEndIndex();
        ArrayList<Annotation> arrayList = this.annotationsMap.get(key);

        if (arrayList == null) {
            arrayList = new ArrayList<>();
        }

        arrayList.add(annotation);

        this.annotationsMap.put(key, arrayList);
    }

    private void updateAnnotationCounts(Annotation annotation) {
        String key = annotation.getType() + ":" +
                annotation.getStartIndex() + ":" +
                annotation.getEndIndex();

        Tuple<Integer, Integer> counts = this.annotationStats.containsKey(key) ?
                                            this.annotationStats.get(key) :
                                            new Tuple<>(0, 0);

        int numTrue = annotation.getValue() == 1 ? counts.getX() + 1 : counts.getX();
        int numFalse = annotation.getValue() == 0 ? counts.getY() + 1 : counts.getY();

        this.annotationStats.put(key, new Tuple<>(numTrue, numFalse));
    }

    private SpannableString getAnnotatedText(String original, ArrayList<Annotation> annotations, int indexOffset) {
        SpannableString str = new SpannableString(original);

        int originalLength = original.length();
        int totalHeadlineAnnotations = this.headlineAnnotations.size();
        int totalBodyAnnotations = this.bodyAnnotations.size();

        for (Annotation annotation : annotations) {
            int formattedStartIndex = annotation.getStartIndex() - indexOffset;
            int formattedEndIndex = annotation.getEndIndex() - indexOffset;

            if (formattedStartIndex < 0 || formattedEndIndex < 0 ||
                formattedStartIndex > originalLength || formattedEndIndex > originalLength) {
                continue;
            }

            String annotationKey = annotation.getType() + ":" +
                    annotation.getStartIndex() + ":" + annotation.getEndIndex();

            Tuple<Integer, Integer> counts = this.annotationStats.containsKey(annotationKey) ?
                                                this.annotationStats.get(annotationKey) :
                                                new Tuple<>(0, 0);

            int total = counts.getX() + counts.getY();

            float interpolation = total > 0 ? counts.getX() / (float) total : 0;
            float alpha = 0;

            if (annotation.getType().equals(Annotation.TYPE_HEADLINE) && totalHeadlineAnnotations > 0) {
                alpha = total / (float) totalHeadlineAnnotations;
            } else if (annotation.getType().equals(Annotation.TYPE_BODY) && totalBodyAnnotations > 0) {
                alpha = total / (float) totalBodyAnnotations;
            }

            int color = interpolateColor(Color.RED, Color.GREEN, interpolation);
            int colorAlpha = ColorUtils.setAlphaComponent(color, Math.round(alpha * 255));
            BackgroundColorSpan backgroundColorSpan = new BackgroundColorSpan(colorAlpha);

            str.setSpan(
                    backgroundColorSpan,
                    formattedStartIndex,
                    formattedEndIndex,
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

                                if (body != null) {
                                    body = body.replace("\\n", System.getProperty("line.separator"));
                                }

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

                                String annotationKey = type + ":" + start.toString() + ":" + end.toString();

                                Tuple<Integer, Integer> stats = annotationStats.containsKey(annotationKey) ?
                                                                    annotationStats.get(annotationKey) :
                                                                    new Tuple<>(0, 0);

                                int numTrue = value == 1 ? stats.getX() + 1 : stats.getX();
                                int numFalse = value == 0 ? stats.getY() + 1 : stats.getY();

                                annotationStats.put(annotationKey, new Tuple<>(numTrue, numFalse));

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

                                addAnnotationMap(annotation);
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
