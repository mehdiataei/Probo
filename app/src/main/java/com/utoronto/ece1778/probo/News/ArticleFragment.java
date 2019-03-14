package com.utoronto.ece1778.probo.News;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.FragmentTransaction;
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
import com.utoronto.ece1778.probo.R;
import com.utoronto.ece1778.probo.User.User;
import com.utoronto.ece1778.probo.Utils.ClickableTextView;
import com.utoronto.ece1778.probo.Utils.GlideImageLoader;
import com.utoronto.ece1778.probo.Utils.Helper;
import com.utoronto.ece1778.probo.Utils.SquareImageView;
import com.utoronto.ece1778.probo.Utils.WrapHeightViewPager;

import java.lang.ref.WeakReference;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Locale;

public class ArticleFragment extends Fragment
        implements AnnotationFragment.AnnotationFragmentInteractionListener,
                    AnnotationMoreFragment.AnnotationMoreFragmentInteractionListener {

    private static final String ARG_ARTICLE_ID = "articleId";

    private final int MAX_NUM_INLINE_ANNOTATION_TILES = 5;

    private boolean showHeatmap = false;

    int currentAnnotationStartIndex = -1;
    int currentAnnotationEndIndex = -1;

    private SwipeRefreshLayout refresh;
    private Article article;

    private Switch heatmapSwitch;
    private TextView headline;
    private ClickableTextView body, bodyOverflow;
    private WrapHeightViewPager annotationsContainer;
    private ProgressBar progressBar;
    private SquareImageView image;
    private TextView author;
    private TextView datetime;
    
    private User user;

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

    public Article getArticle() {
        return this.article;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        if (getArguments() != null) {
            article = new Article(getArguments().getString(ARG_ARTICLE_ID));
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_article, container, false);

        refresh = v.findViewById(R.id.refresh);

        headline = v.findViewById(R.id.headline);
        body = v.findViewById(R.id.body);
        bodyOverflow = v.findViewById(R.id.body_overflow);
        annotationsContainer = v.findViewById(R.id.annotations_container);
        progressBar = v.findViewById(R.id.progress_spinner);
        image = v.findViewById(R.id.image);
        author = v.findViewById(R.id.author);
        datetime = v.findViewById(R.id.datetime);

        user = new User();

        ArticleCallback cb = new ArticleCallback() {
            @Override
            public void onLoad() {
                populateArticle();

                if (heatmapSwitch != null) {
                    heatmapSwitch.setEnabled(true);
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
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.article_action_menu, menu);

        MenuItem item = menu.findItem(R.id.heatmap_action);
        heatmapSwitch = item.getActionView().findViewById(R.id.heatmap_switch);
        heatmapSwitch.setChecked(showHeatmap);
        heatmapSwitch.setOnCheckedChangeListener(handleHeatmapSwitch);

        if (article.hasLoaded()) {
            heatmapSwitch.setEnabled(true);
        }

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onDestroyView() {
        setHasOptionsMenu(false);

        super.onDestroyView();
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
        ArticleFragment.UpdateTextParams params = new ArticleFragment.UpdateTextParams(article, showHeatmap);
        new ArticleFragment.UpdateText(this).execute(params);
    }

    private void showLocatedAnnotations(String type, int startIndex, int endIndex) {
        ArrayList<Annotation> annotations = article.getLocatedAnnotations(type, startIndex, endIndex);

        annotationsContainer.setPageTransformer(true, new ArticleFragment.ZoomOutPageTransformer());

        ArticleFragment.AnnotationPagerAdapter adapter = new ArticleFragment.AnnotationPagerAdapter(
                getChildFragmentManager(),
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
        ArticleFragment.SplitTextParams params = new ArticleFragment.SplitTextParams(article, showHeatmap, index);
        new ArticleFragment.SplitText(this).execute(params);
    }

    private void showAnnotationInput(String quote, String type, int startIndex, int endIndex, int value) {
        if (interactionListener != null) {
            interactionListener.onAnnotationInput(quote, type, startIndex, endIndex, value);
        }
    }

    public void toggleHeatmap(boolean show) {
        showHeatmap = show;
        updateArticleText();
    }

    public void onAnnotationSubmit(final Annotation.AnnotationSubmitCallback cb, String type, int startIndex, int endIndex, int value, String comment) {
        Annotation.AnnotationSubmitCallback submitCb = new Annotation.AnnotationSubmitCallback() {
            @Override
            public void onSubmit() {
                cb.onSubmit();
                updateArticleText();
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
                    user,
                    startIndex,
                    endIndex,
                    value,
                    comment
            );
        } else {
            article.addBodyAnnotation(
                    submitCb,
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

        Helper.vibrate(getActivity().getApplicationContext(), 100);
        showAnnotationInput(quote, Annotation.TYPE_BODY, startIndex, endIndex,1);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        Fragment parentFragment = getParentFragment();

        if (parentFragment instanceof ArticleFragmentInteractionListener) {
            interactionListener = (ArticleFragmentInteractionListener) parentFragment;
        } else {
            throw new RuntimeException(parentFragment.toString()
                    + " must implement ArticleFragmentInteractionListener");
        }
    }

    private static class UpdateText extends AsyncTask<ArticleFragment.UpdateTextParams, Void, ArticleFragment.UpdateTextResults> {
        private WeakReference<ArticleFragment> activityReference;

        UpdateText(ArticleFragment context) {
            activityReference = new WeakReference<>(context);
        }

        protected ArticleFragment.UpdateTextResults doInBackground(ArticleFragment.UpdateTextParams... params) {
            Article currentArticle = params[0].getCurrentArticle();
            boolean displayHeatmap = params[0].getDisplayHeatmap();

            return new ArticleFragment.UpdateTextResults(
                    currentArticle.getHeadline(displayHeatmap),
                    currentArticle.getBody(displayHeatmap, -1, -1)
            );
        }

        protected void onPreExecute() {
            ArticleFragment activity = activityReference.get();
            activity.annotationsContainer.setVisibility(View.GONE);
        }

        protected void onPostExecute(ArticleFragment.UpdateTextResults results) {
            final ArticleFragment activity = activityReference.get();

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

    public interface ArticleFragmentInteractionListener {
        void onAnnotationInput(String quote, String type, int startIndex, int endIndex, int value);
        void onMoreAnnotations(String type, int startIndex, int endIndex);
        void onRouteToProfile(String userId);
    }
}
