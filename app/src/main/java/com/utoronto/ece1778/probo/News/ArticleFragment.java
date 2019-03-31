package com.utoronto.ece1778.probo.News;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.text.SpannableString;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.Priority;
import com.bumptech.glide.request.RequestOptions;
import com.github.glomadrian.materialanimatedswitch.MaterialAnimatedSwitch;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.ibm.watson.developer_cloud.http.ServiceCallback;
import com.ibm.watson.developer_cloud.natural_language_understanding.v1.NaturalLanguageUnderstanding;
import com.ibm.watson.developer_cloud.natural_language_understanding.v1.model.AnalysisResults;
import com.ibm.watson.developer_cloud.natural_language_understanding.v1.model.AnalyzeOptions;
import com.ibm.watson.developer_cloud.natural_language_understanding.v1.model.Features;
import com.ibm.watson.developer_cloud.natural_language_understanding.v1.model.KeywordsOptions;
import com.ibm.watson.developer_cloud.natural_language_understanding.v1.model.KeywordsResult;
import com.ibm.watson.developer_cloud.natural_language_understanding.v1.model.SentimentOptions;
import com.ibm.watson.developer_cloud.natural_language_understanding.v1.model.TargetedSentimentResults;
import com.ibm.watson.developer_cloud.service.security.IamOptions;
import com.tbuonomo.creativeviewpager.CreativeViewPager;
import com.tbuonomo.creativeviewpager.adapter.CreativePagerAdapter;
import com.utoronto.ece1778.probo.Models.Sentence;
import com.utoronto.ece1778.probo.R;
import com.utoronto.ece1778.probo.User.User;
import com.utoronto.ece1778.probo.Utils.ClickableTextView;
import com.utoronto.ece1778.probo.Utils.ExtractSentences;
import com.utoronto.ece1778.probo.Utils.GlideImageLoader;
import com.utoronto.ece1778.probo.Utils.Helper;
import com.utoronto.ece1778.probo.Utils.ImageBitmap;
import com.utoronto.ece1778.probo.Utils.ImageLoader;
import com.utoronto.ece1778.probo.Utils.SentimentParams;
import com.utoronto.ece1778.probo.Utils.SquareImageView;

import net.ricecode.similarity.JaroWinklerStrategy;
import net.ricecode.similarity.SimilarityStrategy;
import net.ricecode.similarity.StringSimilarityService;
import net.ricecode.similarity.StringSimilarityServiceImpl;

import java.lang.ref.WeakReference;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;

import static com.utoronto.ece1778.probo.News.Article.TAG;

public class ArticleFragment extends Fragment
        implements AnnotationsFragment.AnnotationsFragmentInteractionListener {

    private static final String
            ARG_ARTICLE_ID = "articleId",
            ARG_ANNOTATION_ID = "annotationId",
            ARG_ANNOTATION_TYPE = "annotationType",
            ARG_ANNOTATION_START_INDEX = "annotationStartIndex",
            ARG_ANNOTATION_END_INDEX = "annotationEndIndex";

    private final int MAX_NUM_INLINE_ANNOTATION_TILES = 5;

    private boolean showHeatmap = true;

    int currentHeadlineAnnotationStartIndex = -1;
    int currentHeadlineAnnotationEndIndex = -1;
    int currentBodyAnnotationStartIndex = -1;
    int currentBodyAnnotationEndIndex = -1;

    private SwipeRefreshLayout refresh;
    private Article article;

    private String intentAnnotationId, intentAnnotationType;
    private int intentAnnotationStartIndex, intentAnnotationEndIndex;

    MaterialAnimatedSwitch.OnCheckedChangeListener handleHeatmapSwitch = new MaterialAnimatedSwitch.OnCheckedChangeListener() {

        @Override
        public void onCheckedChanged(boolean isChecked) {
            toggleHeatmap(isChecked);
        }

    };
    private ClickableTextView headline, body, bodyOverflow;
    private CreativeViewPager headlineAnnotationsContainer, bodyAnnotationsContainer;
    private ProgressBar progressBar;
    private SquareImageView image;
    private TextView author;
    private TextView datetime;

    private User.UserFragmentInteractionListener userInteractionListener;
    private ArticleFragmentInteractionListener interactionListener;

    public ArticleFragment() {
    }

    public static ArticleFragment newInstance(String articleId) {
        ArticleFragment fragment = new ArticleFragment();
        Bundle args = new Bundle();
        args.putString(ARG_ARTICLE_ID, articleId);
        fragment.setArguments(args);
        return fragment;
    }

    public static ArticleFragment newInstance(String articleId, String annotationId, String annotationType,
                                              int annotationStartIndex, int annotationEndIndex) {

        ArticleFragment fragment = new ArticleFragment();
        Bundle args = new Bundle();
        args.putString(ARG_ARTICLE_ID, articleId);
        args.putString(ARG_ANNOTATION_ID, annotationId);
        args.putString(ARG_ANNOTATION_TYPE, annotationType);
        args.putInt(ARG_ANNOTATION_START_INDEX, annotationStartIndex);
        args.putInt(ARG_ANNOTATION_END_INDEX, annotationEndIndex);
        fragment.setArguments(args);
        return fragment;
    }

    public Article getArticle() {
        return this.article;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        if (getArguments() != null) {
            article = new Article(getArguments().getString(ARG_ARTICLE_ID));
            intentAnnotationId = getArguments().getString(ARG_ANNOTATION_ID);
            intentAnnotationType = getArguments().getString(ARG_ANNOTATION_TYPE);
            intentAnnotationStartIndex = getArguments().getInt(ARG_ANNOTATION_START_INDEX);
            intentAnnotationEndIndex = getArguments().getInt(ARG_ANNOTATION_END_INDEX);
        }
    }

    private MaterialAnimatedSwitch heatmapSwitch;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_article, container, false);

        refresh = v.findViewById(R.id.refresh);

        headline = v.findViewById(R.id.headline);
        body = v.findViewById(R.id.body);
        bodyOverflow = v.findViewById(R.id.body_overflow);
        headlineAnnotationsContainer = v.findViewById(R.id.headline_annotations_container);
        bodyAnnotationsContainer = v.findViewById(R.id.annotations_container);
        progressBar = v.findViewById(R.id.progress_spinner);
        image = v.findViewById(R.id.image);
        author = v.findViewById(R.id.author);
        datetime = v.findViewById(R.id.datetime);

        final Article.ArticleCallback cb = new Article.ArticleCallback() {
            @Override
            public void onLoad() {
                boolean specificAnnotation = intentAnnotationId != null &&
                        intentAnnotationType != null &&
                        intentAnnotationStartIndex >= 0 &&
                        intentAnnotationEndIndex >= 0;

                populateArticle(!specificAnnotation);

                if (heatmapSwitch != null) {
                    heatmapSwitch.setEnabled(true);
                    heatmapSwitch.post(new Runnable() {
                        @Override
                        public void run() {
                            heatmapSwitch.toggle();
                        }
                    });
                }

                if (specificAnnotation) {
                    showLocatedAnnotations(
                            intentAnnotationId,
                            intentAnnotationType,
                            intentAnnotationStartIndex,
                            intentAnnotationEndIndex
                    );
                }
            }

            @Override
            public void onArticleError(int errorCode) {
                Log.d("PROBO_APP", "errorCode: " + errorCode);
            }

            @Override
            public void onError(Exception e) {
                Log.d("PROBO_APP", "err", e);
            }
        };

        article.load(cb);

        refresh.setOnRefreshListener(handleRefresh);

        return v;
    }

    @Override
    public void onDestroyView() {
        setHasOptionsMenu(false);
        super.onDestroyView();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.article_action_menu, menu);

        MenuItem item = menu.findItem(R.id.heatmap_action);
        heatmapSwitch = item.getActionView().findViewById(R.id.heatmap_switch);

        heatmapSwitch.setOnCheckedChangeListener(handleHeatmapSwitch);

        if (article.hasLoaded()) {
            heatmapSwitch.setEnabled(true);

        }

        super.onCreateOptionsMenu(menu, inflater);
    }

    private SwipeRefreshLayout.OnRefreshListener handleRefresh = new SwipeRefreshLayout.OnRefreshListener() {
        @Override
        public void onRefresh() {
            Article.ArticleCallback cb = new Article.ArticleCallback() {
                @Override
                public void onLoad() {
                    populateArticle(true);
                    refresh.setRefreshing(false);
                }

                @Override
                public void onArticleError(int errorCode) {
                    Log.d("PROBO_APP", "erroCode: " + errorCode);
                }

                @Override
                public void onError(Exception e) {
                    Log.d("PROBO_APP", "err", e);
                }
            };

            article.load(cb);
        }
    };

    public void populateArticle(boolean showArticleText) {
        RequestOptions options = new RequestOptions()
                .centerCrop()
                .priority(Priority.HIGH);

        new GlideImageLoader(image, progressBar).load(article.getImageUrl(), options);

        DateFormat dateFormat = DateFormat.getDateTimeInstance(
                DateFormat.SHORT,
                DateFormat.SHORT,
                Locale.getDefault()
        );

        author.setText(article.getAuthor());
        datetime.setText(dateFormat.format(article.getDatetime()));

        updateArticleText(showArticleText);
    }

    public void updateArticleText(boolean updateBody) {
        ArticleFragment.UpdateTextParams params = new ArticleFragment.UpdateTextParams(
                article,
                showHeatmap,
                updateBody
        );

        new ArticleFragment.UpdateText(this).execute(params);
    }

    private void showLocatedAnnotations(String type, int startIndex, int endIndex) {
        ArrayList<Annotation> annotations = article.getLocatedAnnotations(type, startIndex, endIndex);

        ArticleFragment.AnnotationPagerAdapter adapter = new ArticleFragment.AnnotationPagerAdapter(
                getContext(),
                annotations,
                type,
                startIndex,
                endIndex
        );

        if (type.equals(Annotation.TYPE_BODY)) {
            splitBody(endIndex);
        }

        CreativeViewPager container = type.equals(Annotation.TYPE_HEADLINE) ?
                headlineAnnotationsContainer : bodyAnnotationsContainer;

        container.setVisibility(View.VISIBLE);
        container.setCreativeViewPagerAdapter(adapter);
        container.setCurrentItem(0);

        if (type.equals(Annotation.TYPE_HEADLINE)) {
            currentHeadlineAnnotationStartIndex = startIndex;
            currentHeadlineAnnotationEndIndex = endIndex;
        } else if (type.equals(Annotation.TYPE_BODY)) {
            currentBodyAnnotationStartIndex = startIndex;
            currentBodyAnnotationEndIndex = endIndex;
        }
    }

    private void showLocatedAnnotations(String id, String type, int startIndex, int endIndex) {
        showLocatedAnnotations(type, startIndex, endIndex);

        ArrayList<Annotation> annotations = article.getLocatedAnnotations(type, startIndex, endIndex);
        int index = 0;

        for (Annotation annotation : annotations) {
            if (annotation.getId().equals(id)) {
                break;
            }

            index++;
        }

        CreativeViewPager container = type.equals(Annotation.TYPE_HEADLINE) ?
                headlineAnnotationsContainer : bodyAnnotationsContainer;

        container.setCurrentItem(index);
    }

    private void splitBody(int index) {
        ArticleFragment.SplitTextParams params = new ArticleFragment.SplitTextParams(article, showHeatmap, index);
        new ArticleFragment.SplitText(this).execute(params);
    }

    private void showAnnotationInput(float sentiment, String quote, String type, int startIndex, int endIndex, int value) {
        if (interactionListener != null) {
            interactionListener.onAnnotationInput(sentiment, quote, type, startIndex, endIndex, value);
        }
    }

    public void toggleHeatmap(boolean show) {
        showHeatmap = show;
        updateArticleText(true);
    }

    public void onAnnotationSubmit(final Annotation.AnnotationSubmitCallback cb, String type,
                                   int startIndex, int endIndex, final int value, final String comment,
                                   final String source, String sentence, final boolean subscribe) {

        Annotation.AnnotationSubmitCallback submitCb = new Annotation.AnnotationSubmitCallback() {
            @Override
            public void onSubmit(final Annotation annotation) {
                User.UserSubscribeCallback subscribeCb = new User.UserSubscribeCallback() {
                    @Override
                    public void onUpdate() {
                        cb.onSubmit(annotation);
                        updateArticleText(true);
                    }

                    @Override
                    public void onError(Exception error) {
                    }
                };

                if (subscribe) {
                    User user = userInteractionListener.getUser();
                    user.subscribe(
                            subscribeCb,
                            annotation
                    );
                    userInteractionListener.updateUser(user);
                } else {
                    subscribeCb.onUpdate();
                }
            }

            @Override
            public void onAnnotationError(int errorCode) {
                cb.onAnnotationError(errorCode);
            }

            @Override
            public void onError(Exception e) {
                cb.onError(e);
            }
        };

        if (type.equals(Annotation.TYPE_HEADLINE)) {
            article.addHeadlineAnnotation(
                    submitCb,
                    userInteractionListener.getUser(),
                    startIndex,
                    endIndex,
                    value,
                    comment,
                    source,
                    sentence
            );
        } else {
            article.addBodyAnnotation(
                    submitCb,
                    userInteractionListener.getUser(),
                    startIndex,
                    endIndex,
                    value,
                    comment,
                    source,
                    sentence
            );
        }
    }


    public void onAnnotationSourceChecker(final Annotation.AnnotationSourceCheckerCallback cb, String source) {

        startSourceChecker(source, cb);


    }

    @Override
    public void onAnnotationVote(Annotation annotation) {
        article.updateAnnotation(annotation);
    }

    public void onFollow(User updatedUser) {
        userInteractionListener.updateUser(updatedUser);
    }

    public void onMoreAnnotations(String type, int startIndex, int endIndex) {
        if (interactionListener != null) {
            interactionListener.onMoreAnnotations(type, startIndex, endIndex);
        }
    }

    @Override
    public void onRouteToProfile(String userId) {
        if (interactionListener != null) {
            interactionListener.onRouteToProfile(userId);
        }
    }

    public void handleTextViewClick(String quote, String type, int startIndex, int endIndex) {
        boolean annotationExists = article.annotationExists(type, startIndex, endIndex);

        if (!showHeatmap || !annotationExists) {
            return;
        }

        if (type.equals(Annotation.TYPE_HEADLINE) &&
                (startIndex != currentHeadlineAnnotationStartIndex ||
                        endIndex != currentHeadlineAnnotationEndIndex)) {

            showLocatedAnnotations(type, startIndex, endIndex);
        } else if (type.equals(Annotation.TYPE_BODY) &&
                (startIndex != currentBodyAnnotationStartIndex ||
                        endIndex != currentBodyAnnotationEndIndex)) {

            currentBodyAnnotationStartIndex = startIndex;
            currentBodyAnnotationEndIndex = endIndex;

            showLocatedAnnotations(type, startIndex, endIndex);
        } else {
            updateArticleText(true);
        }
    }

    public void handleTextViewLongClick(final String quote, final String type, final int startIndex, final int endIndex) {
        if (!showHeatmap) {
            return;
        }

        Log.d(TAG, "handleTextViewLongClick: startIndex: " + startIndex);

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("news")
                .document(article.getId())
                .collection("sentences")
                .whereEqualTo("startIndex", Integer.toString(startIndex))
                .get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
            @Override
            public void onSuccess(QuerySnapshot queryDocumentSnapshots) {


                for (DocumentSnapshot documentSnapshot : queryDocumentSnapshots) {

                    String sentiment = documentSnapshot.getString("sentiment");

                    Helper.vibrate(getActivity().getApplicationContext(), 100);
                    showAnnotationInput(Float.parseFloat(sentiment), quote, type, startIndex, endIndex, 1);

                }

            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {

                Log.d(TAG, "onFailure: No sentiment found.");

                Helper.vibrate(getActivity().getApplicationContext(), 100);
                showAnnotationInput(0, quote, type, startIndex, endIndex, 1);

            }
        });

    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (context instanceof User.UserFragmentInteractionListener) {
            userInteractionListener = (User.UserFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement User.UserFragmentInteractionListener");
        }

        Fragment parentFragment = getParentFragment();

        if (parentFragment instanceof ArticleFragmentInteractionListener) {
            interactionListener = (ArticleFragmentInteractionListener) parentFragment;
        } else {
            throw new RuntimeException(parentFragment.toString()
                    + " must implement ArticleFragmentInteractionListener");
        }
    }

    public interface ArticleFragmentInteractionListener {
        void onAnnotationInput(float sentiment, String quote, String type, int startIndex, int endIndex, int value);

        void onMoreAnnotations(String type, int startIndex, int endIndex);

        void onRouteToProfile(String userId);
    }

    private void startSentimentAnalysis() {

        SentimentParams params = new SentimentParams(article.getRawBody(), article.getId());

        SentimentAnalysis task = new SentimentAnalysis(this);
        task.execute(params);

    }


    private class SourceChecker extends AsyncTask<SourceCheckerParams, Void, Void> {


        @Override
        protected Void doInBackground(SourceCheckerParams... SourceCheckerParams) {

            final Annotation.AnnotationSourceCheckerCallback cb = SourceCheckerParams[0].getCb();
            final String source = SourceCheckerParams[0].getSource();

            IamOptions options = new IamOptions.Builder()
                    .apiKey("48kgp2fHd9HVz6fe3f0TsQjTWysYNx9iKc2eyh2aUoQ-")
                    .build();

            NaturalLanguageUnderstanding naturalLanguageUnderstanding = new NaturalLanguageUnderstanding("2018-11-16", options);
            naturalLanguageUnderstanding.setEndPoint("https://gateway.watsonplatform.net/natural-language-understanding/api");


            final KeywordsOptions keywords = new KeywordsOptions.Builder()
                    .sentiment(false)
                    .emotion(false)
                    .limit(20)
                    .build();

            Features features = new Features.Builder()
                    .keywords(keywords)
                    .build();


            AnalyzeOptions parameters = new AnalyzeOptions.Builder().url(source)
                    .features(features)
                    .build();

            naturalLanguageUnderstanding
                    .analyze(parameters).enqueue(new ServiceCallback<AnalysisResults>() {
                @Override
                public void onResponse(AnalysisResults response) {

                    List<KeywordsResult> listOfKeywords = response.getKeywords();
                    final List<String> listOfArticlesKeywords = new ArrayList<>();
                    final List<String> listOfSourceKeywords = new ArrayList<>();

                    Log.d(TAG, "onResponse: response of the source checking: " + response);


                    for (KeywordsResult k : listOfKeywords) {

                        listOfSourceKeywords.add(k.getText());

                    }

                    FirebaseFirestore db = FirebaseFirestore.getInstance();

                    db.collection("news").document(article.getId()).collection("keywords").get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {

                            for (QueryDocumentSnapshot document : task.getResult()) {

                                listOfArticlesKeywords.add(document.getString("text"));

                            }

                            boolean checked = false;

                            for (String sourceStr : listOfArticlesKeywords) {

                                for (String targetStr : listOfSourceKeywords) {


                                    SimilarityStrategy strategy = new JaroWinklerStrategy();

                                    StringSimilarityService service = new StringSimilarityServiceImpl(strategy);
                                    double score = service.score(sourceStr, targetStr);

                                    Log.d(TAG, "onComplete: source: " + sourceStr + " target: " + targetStr + " Jaro score: " + score);


                                    if (score > 0.9) {

                                        cb.onChecked();
                                        checked = true;

                                    }

                                }

                            }

                            if (!checked) {

                                cb.onSourceError();
                            }


                        }
                    });


                }


                @Override
                public void onFailure(Exception e) {

                    cb.onError(e);

                }


            });


            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
        }
    }


    private void startSourceChecker(String source, Annotation.AnnotationSourceCheckerCallback cb) {

        SourceCheckerParams params = new SourceCheckerParams(cb, source);

        SourceChecker task = new SourceChecker();
        task.execute(params);

    }


    private static class UpdateText extends AsyncTask<ArticleFragment.UpdateTextParams, Void, ArticleFragment.UpdateTextResults> {
        private WeakReference<ArticleFragment> activityReference;

        UpdateText(ArticleFragment context) {
            activityReference = new WeakReference<>(context);
        }

        protected ArticleFragment.UpdateTextResults doInBackground(ArticleFragment.UpdateTextParams... params) {
            Article currentArticle = params[0].getCurrentArticle();
            boolean displayHeatmap = params[0].getDisplayHeatmap();
            boolean updateBody = params[0].getUpdateBody();

            return new ArticleFragment.UpdateTextResults(
                    currentArticle.getHeadline(displayHeatmap),
                    currentArticle.getBody(displayHeatmap, -1, -1),
                    updateBody
            );
        }

        protected void onPreExecute() {
            ArticleFragment activity = activityReference.get();

            activity.headlineAnnotationsContainer.setVisibility(View.GONE);
            activity.bodyAnnotationsContainer.setVisibility(View.GONE);
        }

        protected void onPostExecute(ArticleFragment.UpdateTextResults results) {
            final ArticleFragment activity = activityReference.get();


            if (activity.article.getAnalysed().equals("false")) {
                activity.startSentimentAnalysis();
            }

            ClickableTextView.ClickableTextViewInterface headlineCb = new ClickableTextView.ClickableTextViewInterface() {
                @Override
                public void onTextViewClick(String quote, int startIndex, int endIndex) {
                    activity.handleTextViewClick(quote, Annotation.TYPE_HEADLINE, startIndex, endIndex);
                }

                @Override
                public void onTextViewLongClick(String quote, int startIndex, int endIndex) {
                    activity.handleTextViewLongClick(quote, Annotation.TYPE_HEADLINE, startIndex, endIndex);
                }
            };

            ClickableTextView.ClickableTextViewInterface bodyCb = new ClickableTextView.ClickableTextViewInterface() {
                @Override
                public void onTextViewClick(String quote, int startIndex, int endIndex) {
                    activity.handleTextViewClick(quote, Annotation.TYPE_BODY, startIndex, endIndex);
                }

                @Override
                public void onTextViewLongClick(String quote, int startIndex, int endIndex) {
                    activity.handleTextViewLongClick(quote, Annotation.TYPE_BODY, startIndex, endIndex);
                }
            };

            activity.headline.setTextWithClickableSentences(results.getHeadlineResult(), headlineCb, 0);

            activity.currentHeadlineAnnotationStartIndex = -1;
            activity.currentHeadlineAnnotationEndIndex = -1;

            if (results.getUpdateBody()) {
                activity.body.setTextWithClickableSentences(results.getBodyResult(), bodyCb, 0);

                activity.bodyOverflow.setVisibility(View.GONE);
                activity.bodyOverflow.setTextWithClickableSentences(new SpannableString(""), bodyCb, 0);

                activity.currentBodyAnnotationStartIndex = -1;
                activity.currentBodyAnnotationEndIndex = -1;
            }
        }
    }

    private static class UpdateTextParams {
        private Article currentArticle;
        private boolean displayHeatmap;
        private boolean updateBody;

        UpdateTextParams(Article currentArticle, boolean displayHeatmap, boolean updateBody) {
            this.currentArticle = currentArticle;
            this.displayHeatmap = displayHeatmap;
            this.updateBody = updateBody;
        }

        public Article getCurrentArticle() {
            return this.currentArticle;
        }

        public boolean getDisplayHeatmap() {
            return this.displayHeatmap;
        }

        public boolean getUpdateBody() {
            return this.updateBody;
        }
    }

    private static class UpdateTextResults {
        private SpannableString headlineResult;
        private SpannableString bodyResult;
        private boolean updateBody;

        UpdateTextResults(SpannableString headlineResult, SpannableString bodyResult, boolean updateBody) {
            this.headlineResult = headlineResult;
            this.bodyResult = bodyResult;
            this.updateBody = updateBody;
        }

        public SpannableString getHeadlineResult() {
            return this.headlineResult;
        }

        public SpannableString getBodyResult() {
            return this.bodyResult;
        }

        public boolean getUpdateBody() {
            return this.updateBody;
        }
    }

    private static class SplitText extends AsyncTask<ArticleFragment.SplitTextParams, Void, ArticleFragment.SplitTextResults> {
        private WeakReference<ArticleFragment> activityReference;

        SplitText(ArticleFragment context) {
            activityReference = new WeakReference<>(context);
        }

        protected ArticleFragment.SplitTextResults doInBackground(ArticleFragment.SplitTextParams... params) {
            Article currentArticle = params[0].getCurrentArticle();
            boolean displayHeatmap = params[0].getDisplayHeatmap();
            int index = params[0].getIndex();

            return new ArticleFragment.SplitTextResults(
                    index,
                    currentArticle.getBody(displayHeatmap, 0, index),
                    currentArticle.getBody(displayHeatmap, index, currentArticle.getBodyLength())
            );
        }

        protected void onPostExecute(ArticleFragment.SplitTextResults results) {
            final ArticleFragment activity = activityReference.get();

            final int TEXT_MARGIN = 32;
            int index = results.getIndex();

            SpannableString firstSection = results.getFirstSection();
            SpannableString secondSection = results.getSecondSection();

            if (firstSection.toString().lastIndexOf("\n") != index - 1) {
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

                params.setMargins(0, 0, 0, TEXT_MARGIN);
                activity.body.setLayoutParams(params);
            }

            if (secondSection.toString().indexOf("\n") != 0) {
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

                params.setMargins(0, TEXT_MARGIN, 0, 0);
                activity.bodyOverflow.setLayoutParams(params);
            }

            ClickableTextView.ClickableTextViewInterface cb = new ClickableTextView.ClickableTextViewInterface() {
                @Override
                public void onTextViewClick(String quote, int startIndex, int endIndex) {
                    activity.handleTextViewClick(quote, Annotation.TYPE_BODY, startIndex, endIndex);
                }

                @Override
                public void onTextViewLongClick(String quote, int startIndex, int endIndex) {
                    activity.handleTextViewLongClick(quote, Annotation.TYPE_BODY, startIndex, endIndex);
                }
            };

            activity.body.setTextWithClickableSentences(firstSection, cb, 0);

            activity.bodyOverflow.setTextWithClickableSentences(secondSection, cb, index);
            activity.bodyOverflow.setVisibility(View.VISIBLE);
        }
    }

    private static class SplitTextParams {
        private Article currentArticle;
        private boolean displayHeatmap;
        private int index;

        SplitTextParams(Article currentArticle, boolean displayHeatmap, int index) {
            this.currentArticle = currentArticle;
            this.displayHeatmap = displayHeatmap;
            this.index = index;
        }

        public Article getCurrentArticle() {
            return this.currentArticle;
        }

        public boolean getDisplayHeatmap() {
            return this.displayHeatmap;
        }

        public int getIndex() {
            return this.index;
        }
    }

    private static class SplitTextResults {
        private int index;
        private SpannableString firstSection;
        private SpannableString secondSection;

        SplitTextResults(int index, SpannableString firstSection, SpannableString secondSection) {
            this.index = index;
            this.firstSection = firstSection;
            this.secondSection = secondSection;
        }

        public int getIndex() {
            return this.index;
        }

        public SpannableString getFirstSection() {
            return this.firstSection;
        }

        public SpannableString getSecondSection() {
            return this.secondSection;
        }
    }

    private class AnnotationPagerAdapter extends ViewPager implements CreativePagerAdapter {
        private ArrayList<Annotation> annotations;
        private String type;
        private int startIndex;
        private int endIndex;

        AnnotationPagerAdapter(Context context, ArrayList<Annotation> annotations, String type, int startIndex, int endIndex) {
            super(context);

            this.annotations = annotations;
            this.type = type;
            this.startIndex = startIndex;
            this.endIndex = endIndex;
        }

        @Override
        public View instantiateHeaderItem(LayoutInflater inflater, ViewGroup container, int position) {
            int rawSize = this.annotations.size();
            int size = this.getCount();

            if (this.hasOverflow(rawSize) && position == size - 1) {
                AnnotationMoreCardView annotationMoreCardView = new AnnotationMoreCardView(getContext());
                annotationMoreCardView.setNumMore(rawSize - MAX_NUM_INLINE_ANNOTATION_TILES + 1);
                annotationMoreCardView.setOnMoreClickListener(new AnnotationMoreCardView.OnMoreClick() {
                    @Override
                    public void onClick() {
                        onMoreAnnotations(type, startIndex, endIndex);
                    }
                });

                return annotationMoreCardView;
            } else {
                AnnotationCardView annotationCardView = new AnnotationCardView(getContext());
                LinearLayout innerLayout = annotationCardView.findViewById(R.id.inner);

                innerLayout.setPadding(
                        0,
                        Helper.dpToPx(getContext(), 42),
                        0,
                        0
                );

                annotationCardView.hideProfileImage();
                annotationCardView.setData(this.annotations.get(position), userInteractionListener.getUser());

                annotationCardView.setOnUserClickListener(new AnnotationCardView.OnUserClickListener() {
                    @Override
                    public void onClick(User user) {
                        onRouteToProfile(user.getUid());
                    }
                });

                annotationCardView.setOnVoteListener(new AnnotationCardView.OnVoteListener() {
                    @Override
                    public void onVote(Annotation annotation) {
                        onAnnotationVote(annotation);
                    }
                });

                annotationCardView.setOnFollowListener(new AnnotationCardView.OnFollowListener() {
                    @Override
                    public void onUpdate(User updatedUser) {
                        onFollow(updatedUser);
                    }
                });

                return annotationCardView;
            }
        }

        @Override
        public View instantiateContentItem(LayoutInflater inflater, ViewGroup container, final int position) {
            final Annotation annotation = this.annotations.get(position);
            final User annotationUser = annotation.getUser();

            View contentRoot = inflater.inflate(R.layout.annotation_card_view_pager_profile, container, false);
            final ImageView imageView = contentRoot.findViewById(R.id.profile_image);

            if (this.hasOverflow(this.annotations.size()) && position == this.getCount() - 1) {
                imageView.setVisibility(View.GONE);
            } else {
                imageView.setImageResource(R.drawable.profile_image_default_dark);
                imageView.setBackgroundResource(R.drawable.oval_border_light);

                imageView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        onRouteToProfile(annotation.getUser().getUid());
                    }
                });

                User.UserCallback userCb = new User.UserCallback() {
                    @Override
                    public void onLoad() {
                        ImageLoader.ImageLoaderCallback imageCb = new ImageLoader.ImageLoaderCallback() {
                            @Override
                            public void onSuccess(Bitmap image) {
                                ImageBitmap imageBitmap = new ImageBitmap(image);
                                RoundedBitmapDrawable roundedImage = imageBitmap.getCroppedRoundedBitmapDrawable(getResources());

                                imageView.setImageDrawable(roundedImage);
                            }

                            @Override
                            public void onFailure(Exception e) {
                            }

                            @Override
                            public void onComplete() {
                            }
                        };

                        ImageLoader imageLoader = new ImageLoader(annotationUser.getProfileImagePath(), getContext());

                        if (annotationUser.getProfileImagePath() != null) {
                            imageLoader.load(imageCb);
                        }
                    }

                    @Override
                    public void onError(Exception error) {
                    }
                };

                annotationUser.load(userCb);
            }

            return contentRoot;
        }

        @Override
        public boolean isUpdatingBackgroundColor() {
            return false;
        }

        @Override
        public Bitmap requestBitmapAtPosition(int position) {
            return null;
        }

        private boolean hasOverflow(int size) {
            return size - MAX_NUM_INLINE_ANNOTATION_TILES > 1;
        }

        @Override
        public int getCount() {
            int size = this.annotations.size();

            return this.hasOverflow(size) ?
                    Math.min(size, MAX_NUM_INLINE_ANNOTATION_TILES) : size;
        }
    }

    public class SourceCheckerParams {

        Annotation.AnnotationSourceCheckerCallback cb;
        String source;

        public SourceCheckerParams(Annotation.AnnotationSourceCheckerCallback cb, String source) {
            this.cb = cb;
            this.source = source;
        }


        public Annotation.AnnotationSourceCheckerCallback getCb() {
            return cb;
        }

        public void setCb(Annotation.AnnotationSourceCheckerCallback cb) {
            this.cb = cb;
        }

        public String getSource() {
            return source;
        }

        public void setSource(String source) {
            this.source = source;
        }
    }

    private class SentimentAnalysis extends AsyncTask<SentimentParams, Void, ArrayList<Sentence>> {


        private WeakReference<ArticleFragment> activityReference;

        public SentimentAnalysis(ArticleFragment context) {

            this.activityReference = new WeakReference<>(context);
        }

        @Override
        protected ArrayList<Sentence> doInBackground(SentimentParams... sentimentParams) {


            final String body = sentimentParams[0].getBody().replaceAll("\n\n", " ");
            final String rawBody = sentimentParams[0].getBody().replaceAll("\n\n", "  ");
            final String articleId = sentimentParams[0].getArticleId();


            final ArrayList<Sentence> sTemp = new ArrayList<>();

            ExtractSentences extractSentences = new ExtractSentences(body);

            List<String> sentencesList = extractSentences.getSentences();

            sentencesList.removeAll(Collections.singleton(null));
            sentencesList.removeAll(Collections.singleton("\n"));

            ListIterator<String> it = sentencesList.listIterator();
            while (it.hasNext()) {
                it.set(it.next().replace("\n", ""));

            }


            IamOptions options = new IamOptions.Builder()
                    .apiKey("48kgp2fHd9HVz6fe3f0TsQjTWysYNx9iKc2eyh2aUoQ-")
                    .build();

            NaturalLanguageUnderstanding naturalLanguageUnderstanding = new NaturalLanguageUnderstanding("2018-11-16", options);
            naturalLanguageUnderstanding.setEndPoint("https://gateway.watsonplatform.net/natural-language-understanding/api");

            SentimentOptions sentiment = new SentimentOptions.Builder().targets(sentencesList)
                    .build();

            KeywordsOptions keywords = new KeywordsOptions.Builder()
                    .sentiment(true)
                    .emotion(true)
                    .limit(20)
                    .build();

            Features features = new Features.Builder()
                    .sentiment(sentiment)
                    .keywords(keywords)
                    .build();


            AnalyzeOptions parameters = new AnalyzeOptions.Builder().text(body)
                    .features(features)
                    .build();

            naturalLanguageUnderstanding
                    .analyze(parameters).enqueue(new ServiceCallback<AnalysisResults>() {
                @Override
                public void onResponse(AnalysisResults response) {


                    List<TargetedSentimentResults> listOfSentiments = response.getSentiment().getTargets();
                    List<KeywordsResult> listOfKeywords = response.getKeywords();

                    Log.d(TAG, "onResponse: response of the whole doc: " + response);

                    Log.d(TAG, "onResponse: Size of the sentiment list: " + listOfSentiments.size());

                    for (TargetedSentimentResults r : listOfSentiments) {

                        Log.d(TAG, "onResponse: Receiving interate response.");

                        String sentence = r.getText();
                        String score = r.getScore().toString();

                        Log.d(TAG, "onResponse: score: " + score);

                        final int startIndex = rawBody.indexOf(sentence);
                        final int endIndex = startIndex + sentence.length();


                        Sentence s = new Sentence(sentence, articleId, score, String.valueOf(startIndex), String.valueOf(endIndex));
                        sTemp.add(s);

                    }


                    FirebaseFirestore db = FirebaseFirestore.getInstance();

                    Map<String, Object> docData = new HashMap<>();
                    docData.put("analysed", "true");


                    for (Sentence s : sTemp) {

                        Log.d(TAG, "onPostExecute: Adding NLP results to the database...");


                        db.collection("news").document(article.getId()).collection("sentences").add(s).addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                            @Override
                            public void onSuccess(DocumentReference documentReference) {
                                Log.d("Probo_app", "onSuccess: Added sentence to the database.");
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.d("Probo_app", "onFailure: Failed adding sentence to the database.");
                            }
                        });

                    }

                    for (KeywordsResult k : listOfKeywords) {

                        Map<String, Object> keywordsInfo = new HashMap<>();
                        keywordsInfo.put("text", k.getText());
                        keywordsInfo.put("relevance", k.getRelevance());
                        keywordsInfo.put("sentiment", k.getSentiment());
                        keywordsInfo.put("emotion", k.getEmotion());


                        db.collection("news").document(article.getId()).collection("keywords").add(keywordsInfo).addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                            @Override
                            public void onSuccess(DocumentReference documentReference) {

                                Log.d(TAG, "onSuccess: Added keyword.");

                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {

                                Log.d(TAG, "onFailure: Failed adding keyword.");

                            }
                        });

                    }


                    db.collection("news").document(article.getId()).update(docData);

                }


                @Override
                public void onFailure(Exception e) {

                }


            });


            return sTemp;
        }

        @Override
        protected void onPostExecute(ArrayList<Sentence> sentences) {
            super.onPostExecute(sentences);

            Log.d("Probo_app", "onTaskCompleted: Sentiment Analysis completed.");

        }


    }
}
