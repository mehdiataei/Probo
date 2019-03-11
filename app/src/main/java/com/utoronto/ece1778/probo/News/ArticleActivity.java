package com.utoronto.ece1778.probo.News;

import android.content.Intent;
import android.os.AsyncTask;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.SpannableString;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;

import com.bumptech.glide.Priority;
import com.bumptech.glide.request.RequestOptions;
import com.utoronto.ece1778.probo.User.User;
import com.utoronto.ece1778.probo.R;
import com.utoronto.ece1778.probo.Utils.ClickableTextView;
import com.utoronto.ece1778.probo.Utils.GlideImageLoader;
import com.utoronto.ece1778.probo.Utils.Helper;
import com.utoronto.ece1778.probo.Utils.SquareImageView;
import com.utoronto.ece1778.probo.Utils.WrapHeightViewPager;

import java.lang.ref.WeakReference;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Locale;

public class ArticleActivity extends AppCompatActivity
        implements AnnotationInputFragment.AnnotationInputFragmentInteractionListener,
                    AnnotationFragment.AnnotationFragmentInteractionListener,
                    AnnotationMoreFragment.AnnotationMoreFragmentInteractionListener {

    private final int MAX_NUM_INLINE_ANNOTATION_TILES = 5;

    private boolean showHeatmap;

    int currentAnnotationStartIndex;
    int currentAnnotationEndIndex;

    private SwipeRefreshLayout refresh;
    private FrameLayout annotationContainer;
    private Article article;

    private TextView headline;
    private ClickableTextView body, bodyOverflow;
    private WrapHeightViewPager annotationsContainer;

    private AnnotationInputFragment annotationInputFragment;

    private User user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_article);

        showHeatmap = false;

        currentAnnotationStartIndex = -1;
        currentAnnotationEndIndex = -1;

        refresh = findViewById(R.id.refresh);
        annotationContainer = findViewById(R.id.annotation_container);

        headline = findViewById(R.id.headline);
        body = findViewById(R.id.body);
        bodyOverflow = findViewById(R.id.body_overflow);
        annotationsContainer = findViewById(R.id.annotations_container);

        user = new User();

        Bundle extras = getIntent().getExtras();

        if (extras != null) {
            ArticleCallback cb = new ArticleCallback() {
                @Override
                public void onLoad() {
                    populateArticle();
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

            article = new Article(extras.getString("articleId"));
            article.load(cb);
        }

        refresh.setOnRefreshListener(handleRefresh);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.article_action_menu, menu);

        MenuItem item = menu.findItem(R.id.heatmap_action);
        Switch heatmapSwitch = item.getActionView().findViewById(R.id.heatmap_switch);
        heatmapSwitch.setOnCheckedChangeListener(handleHeatmapSwitch);

        return super.onCreateOptionsMenu(menu);
    }

    CompoundButton.OnCheckedChangeListener handleHeatmapSwitch = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            toggleHeatmap(isChecked);
        }
    };

    private SwipeRefreshLayout.OnRefreshListener handleRefresh = new SwipeRefreshLayout.OnRefreshListener() {
        @Override
        public void onRefresh() {
            ArticleCallback cb = new ArticleCallback() {
                @Override
                public void onLoad() {
                    populateArticle();
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

    public void populateArticle() {
        ProgressBar progressBar = findViewById(R.id.progress_spinner);
        SquareImageView image = findViewById(R.id.image);
        TextView author = findViewById(R.id.author);
        TextView datetime = findViewById(R.id.datetime);

        RequestOptions options = new RequestOptions()
                .centerCrop()
                .priority(Priority.HIGH);

        new GlideImageLoader(image, progressBar).load(article.getImageUrl(), options);

        DateFormat dateFormat = DateFormat.getDateTimeInstance(
                DateFormat.SHORT,
                DateFormat.MEDIUM,
                Locale.getDefault()
        );

        author.setText(article.getAuthor());
        datetime.setText(dateFormat.format(article.getDatetime()));

        updateArticleText();
    }

    public void updateArticleText() {
        UpdateTextParams params = new UpdateTextParams(article, showHeatmap);
        new UpdateText(this).execute(params);
    }

    private void showLocatedAnnotations(String type, int startIndex, int endIndex) {
        ArrayList<Annotation> annotations = article.getLocatedAnnotations(type, startIndex, endIndex);

        annotationsContainer.setPageTransformer(true, new ZoomOutPageTransformer());

        AnnotationPagerAdapter adapter = new AnnotationPagerAdapter(
                getSupportFragmentManager(),
                annotations,
                type,
                startIndex,
                endIndex
        );

        annotationsContainer.setAdapter(adapter);

        splitBody(endIndex);
        annotationsContainer.setVisibility(View.VISIBLE);
    }

    private void splitBody(int index) {
        SplitTextParams params = new SplitTextParams(article, showHeatmap, index);
        new SplitText(this).execute(params);
    }

    private void showAnnotationInput(String quote, String type, int startIndex, int endIndex, int value) {
        Animation animation = AnimationUtils.loadAnimation(this, R.anim.annotation_input_slide_in);
        FragmentManager manager = getSupportFragmentManager();
        FragmentTransaction transaction = manager.beginTransaction();

        if (annotationInputFragment != null) {
            transaction.remove(annotationInputFragment);
        }

        annotationInputFragment = AnnotationInputFragment.newInstance(
                quote,
                type,
                startIndex,
                endIndex,
                value
        );

        transaction.add(R.id.annotation_container, annotationInputFragment);
        transaction.commit();

        annotationContainer.setVisibility(View.VISIBLE);
        annotationContainer.startAnimation(animation);
    }

    private void hideAnnotationInput() {
        Animation animation = AnimationUtils.loadAnimation(this, R.anim.annotation_input_slide_out);

        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {
                annotationContainer.setVisibility(View.GONE);

                if (annotationInputFragment != null) {
                    FragmentManager manager = getSupportFragmentManager();
                    FragmentTransaction transaction = manager.beginTransaction();
                    transaction.remove(annotationInputFragment);

                    transaction.commit();
                }
            }

            @Override
            public void onAnimationRepeat(Animation animation) {}
        });

        annotationContainer.startAnimation(animation);
    }

    public void toggleHeatmap(boolean show) {
        showHeatmap = show;
        updateArticleText();
    }

    @Override
    public void onAnnotationSubmit(String type, int startIndex, int endIndex, int value, String comment) {
        AnnotationSubmitCallback cb = new AnnotationSubmitCallback() {
            @Override
            public void onSubmit() {
                updateArticleText();
                annotationInputFragment.hideProgress();
                hideAnnotationInput();
            }

            @Override
            public void onAnnotationError(int errorCode) {
                switch (errorCode) {
                    case Article.ARTICLE_ANNOTATION_ERROR_ALREADY_SUBMITTED:
                        annotationInputFragment.showError(getString(R.string.annotation_input_error_already_submitted));
                        break;
                    case Article.ARTICLE_ANNOTATION_ERROR_INTERNAL:
                        annotationInputFragment.showError(getString(R.string.annotation_input_error_general));
                    default:
                        annotationInputFragment.showError(getString(R.string.annotation_input_error_general));
                }
            }

            @Override
            public void onError(Exception e) {
                annotationInputFragment.showError(getString(R.string.article_add_annotation_error));
            }
        };

        if (type.equals(Annotation.TYPE_HEADLINE)) {
            article.addHeadlineAnnotation(
                    cb,
                    user,
                    startIndex,
                    endIndex,
                    value,
                    comment
            );
        } else {
            article.addBodyAnnotation(
                    cb,
                    user,
                    startIndex,
                    endIndex,
                    value,
                    comment
            );
        }
    }

    @Override
    public void onAnnotationVote(AnnotationVote.AnnotationVoteCallback cb, String id, boolean value) {
        for (Annotation annotation : article.getAnnotations()) {
            if (annotation.getId().equals(id)) {
                annotation.vote(cb, user, value);
                return;
            }
        }
    }

    @Override
    public void onAnnotationClose() {
        hideAnnotationInput();
    }

    @Override
    public void onMoreAnnotations(String type, int startIndex, int endIndex) {
        Intent intent = new Intent(getApplicationContext(), AnnotationsActivity.class);

        intent.putExtra("articleId", article.getId());
        intent.putExtra("type", type);
        intent.putExtra("startIndex", startIndex);
        intent.putExtra("endIndex", endIndex);

        startActivity(intent);
    }

    public void handleTextViewClick(String quote, int startIndex, int endIndex) {
        boolean annotationExists = article.annotationExists(Annotation.TYPE_BODY, startIndex, endIndex);

        if (!showHeatmap || !annotationExists) {
            return;
        }

        if (startIndex != currentAnnotationStartIndex || endIndex != currentAnnotationEndIndex) {
            currentAnnotationStartIndex = startIndex;
            currentAnnotationEndIndex = endIndex;

            showLocatedAnnotations(Annotation.TYPE_BODY, startIndex, endIndex);
        } else {
            updateArticleText();
        }
    }

    public void handleTextViewLongClick(String quote, int startIndex, int endIndex) {
        if (!showHeatmap) {
            return;
        }

        Helper.vibrate(getApplicationContext(), 100);
        showAnnotationInput(quote, Annotation.TYPE_BODY, startIndex, endIndex,1);
    }

    private static class UpdateText extends AsyncTask<UpdateTextParams, Void, UpdateTextResults> {
        private WeakReference<ArticleActivity> activityReference;

        UpdateText(ArticleActivity context) {
            activityReference = new WeakReference<>(context);
        }

        protected UpdateTextResults doInBackground(UpdateTextParams... params) {
            Article currentArticle = params[0].getCurrentArticle();
            boolean displayHeatmap = params[0].getDisplayHeatmap();

            return new UpdateTextResults(
                    currentArticle.getHeadline(displayHeatmap),
                    currentArticle.getBody(displayHeatmap, -1, -1)
            );
        }

        protected void onPreExecute() {
            ArticleActivity activity = activityReference.get();
            activity.annotationsContainer.setVisibility(View.GONE);
        }

        protected void onPostExecute(UpdateTextResults results) {
            final ArticleActivity activity = activityReference.get();

            ClickableTextView.ClickableTextViewInterface cb = new ClickableTextView.ClickableTextViewInterface() {
                @Override
                public void onTextViewClick(String quote, int startIndex, int endIndex) {
                    activity.handleTextViewClick(quote, startIndex, endIndex);
                }

                @Override
                public void onTextViewLongClick(String quote, int startIndex, int endIndex) {
                    activity.handleTextViewLongClick(quote, startIndex, endIndex);
                }
            };

            activity.headline.setText(results.getHeadlineResult());
            activity.body.setTextWithClickableSentences(results.getBodyResult(), cb, 0);

            activity.bodyOverflow.setVisibility(View.GONE);
            activity.bodyOverflow.setTextWithClickableSentences(new SpannableString(""), cb, 0);

            activity.currentAnnotationStartIndex = -1;
            activity.currentAnnotationEndIndex = -1;
        }
    }

    private static class UpdateTextParams {
        private Article currentArticle;
        private boolean displayHeatmap;

        UpdateTextParams(Article currentArticle, boolean displayHeatmap) {
            this.currentArticle = currentArticle;
            this.displayHeatmap = displayHeatmap;
        }

        public Article getCurrentArticle() {
            return this.currentArticle;
        }

        public boolean getDisplayHeatmap() {
            return this.displayHeatmap;
        }
    }

    private static class UpdateTextResults {
        private SpannableString headlineResult;
        private SpannableString bodyResult;

        UpdateTextResults(SpannableString headlineResult, SpannableString bodyResult) {
            this.headlineResult = headlineResult;
            this.bodyResult = bodyResult;
        }

        public SpannableString getHeadlineResult() {
            return this.headlineResult;
        }

        public SpannableString getBodyResult() {
            return this.bodyResult;
        }
    }

    private static class SplitText extends AsyncTask<SplitTextParams, Void, SplitTextResults> {
        private WeakReference<ArticleActivity> activityReference;

        SplitText(ArticleActivity context) {
            activityReference = new WeakReference<>(context);
        }

        protected SplitTextResults doInBackground(SplitTextParams... params) {
            Article currentArticle = params[0].getCurrentArticle();
            boolean displayHeatmap = params[0].getDisplayHeatmap();
            int index = params[0].getIndex();

            return new SplitTextResults(
                    index,
                    currentArticle.getBody(displayHeatmap, 0, index),
                    currentArticle.getBody(displayHeatmap, index, currentArticle.getBodyLength())
            );
        }

        protected void onPostExecute(SplitTextResults results) {
            final ArticleActivity activity = activityReference.get();

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
                    activity.handleTextViewClick(quote, startIndex, endIndex);
                }

                @Override
                public void onTextViewLongClick(String quote, int startIndex, int endIndex) {
                    activity.handleTextViewLongClick(quote, startIndex, endIndex);
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

    private class AnnotationPagerAdapter extends FragmentStatePagerAdapter {
        private ArrayList<Annotation> annotations;
        private String type;
        private int startIndex;
        private int endIndex;

        AnnotationPagerAdapter(FragmentManager fm, ArrayList<Annotation> annotations,
                               String type, int startIndex, int endIndex) {

            super(fm);
            this.annotations = annotations;
            this.type = type;
            this.startIndex = startIndex;
            this.endIndex = endIndex;
        }

        @Override
        public Fragment getItem(int position) {
            int rawSize = this.annotations.size();
            int size = this.getCount();

            if (this.hasOverflow(rawSize) && position == size - 1) {
                return AnnotationMoreFragment.newInstance(
                        rawSize - MAX_NUM_INLINE_ANNOTATION_TILES + 1,
                        this.type,
                        this.startIndex,
                        this.endIndex
                );
            }

            Annotation annotation = this.annotations.get(position);

            return AnnotationFragment.newInstance(
                    annotation.getId(),
                    annotation.getUser().getUid(),
                    annotation.getComment(),
                    annotation.getValue(),
                    annotation.getUpvoteCount(),
                    annotation.getDownvoteCount(),
                    annotation.userHasUpvoted(user),
                    annotation.userHasDownvoted(user)
            );
        }

        public boolean hasOverflow(int size) {
            return size - MAX_NUM_INLINE_ANNOTATION_TILES > 1;
        }

        @Override
        public int getCount() {
            int size = this.annotations.size();

            return this.hasOverflow(size) ?
                    Math.min(size, MAX_NUM_INLINE_ANNOTATION_TILES) : size;
        }
    }

    private class ZoomOutPageTransformer implements ViewPager.PageTransformer {
        private static final float MIN_SCALE = 0.85f;
        private static final float MIN_ALPHA = 0.75f;

        public void transformPage(View view, float position) {
            int pageWidth = view.getWidth();
            int pageHeight = view.getHeight();

            if (position < -1) {
                view.setAlpha(0f);
            } else if (position <= 1) {
                float scaleFactor = Math.max(MIN_SCALE, 1 - Math.abs(position));
                float vertMargin = pageHeight * (1 - scaleFactor) / 2;
                float horzMargin = pageWidth * (1 - scaleFactor) / 2;

                if (position < 0) {
                    view.setTranslationX(horzMargin - vertMargin / 2);
                } else {
                    view.setTranslationX(-horzMargin + vertMargin / 2);
                }

                view.setScaleX(scaleFactor);
                view.setScaleY(scaleFactor);

                view.setAlpha(MIN_ALPHA + (scaleFactor - MIN_SCALE) / (1 - MIN_SCALE) * (1 - MIN_ALPHA));
            } else {
                view.setAlpha(0f);
            }
        }
    }
}
