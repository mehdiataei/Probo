package com.utoronto.ece1778.probo.News;

import android.drm.DrmStore;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Build;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Html;
import android.text.SpannableString;
import android.text.style.BackgroundColorSpan;
import android.util.Log;
import android.view.ActionMode;
import android.view.Display;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.TranslateAnimation;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;

import com.bumptech.glide.Priority;
import com.bumptech.glide.request.RequestOptions;
import com.utoronto.ece1778.probo.Login.User;
import com.utoronto.ece1778.probo.R;
import com.utoronto.ece1778.probo.Utils.ClickableTextView;
import com.utoronto.ece1778.probo.Utils.GlideImageLoader;
import com.utoronto.ece1778.probo.Utils.SquareImageView;
import com.utoronto.ece1778.probo.Utils.WrapHeightViewPager;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Locale;

public class ArticleActivity extends AppCompatActivity
        implements AnnotationInputFragment.AnnotationInputFragmentInteractionListener,
                    ClickableTextView.ClickableTextViewInterface {

    private boolean showHeatmap;

    private SwipeRefreshLayout refresh;
    private FrameLayout annotationContainer;
    private Article article;

    private TextView headline;
    private ClickableTextView body;
    private ClickableTextView bodyOverflow;
    private WrapHeightViewPager annotationsContainer;

    private AnnotationInputFragment annotationInputFragment;

    private User user;

    private static final int
            MENU_TRUE_BUTTON = 0,
            MENU_FALSE_BUTTON = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_article);

        showHeatmap = false;

        refresh = findViewById(R.id.refresh);
        annotationContainer = findViewById(R.id.annotation_container);

        headline = findViewById(R.id.headline);
        body = findViewById(R.id.body);
        bodyOverflow = findViewById(R.id.body_overflow);
        annotationsContainer = findViewById(R.id.annotations_container);

        //body.setLongClickable(true);

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
                    Log.d("PROBO_APP", "erroCode: " + errorCode);
                }

                @Override
                public void onError(Exception e) {
                    Log.d("PROBO_APP", "err", e);
                }
            };

            article = new Article(extras.getString("articleId"));
            article.load(cb);
        }

        headline.setCustomSelectionActionModeCallback(handleHeadlineTextSelect);

        refresh.setOnRefreshListener(handleRefresh);

        //showAnnotations();
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

    private ActionMode.Callback handleHeadlineTextSelect = new ActionMode.Callback() {
        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            menu.removeItem(android.R.id.selectAll);
            menu.removeItem(android.R.id.cut);
            menu.removeItem(android.R.id.copy);
            menu.removeItem(android.R.id.shareText);

            return true;
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            menu.add(0, MENU_TRUE_BUTTON, 0, getString(R.string.article_menu_true))
                    .setIcon(R.drawable.thumb_up_icon);

            menu.add(0, MENU_FALSE_BUTTON, 1, getString(R.string.article_menu_false))
                    .setIcon(R.drawable.thumb_down_icon);

            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {}

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            int start = 0;
            int end = headline.getText().length();

            if (headline.isFocused()) {
                final int selectionStart = headline.getSelectionStart();
                final int selectionEnd = headline.getSelectionEnd();

                start = Math.max(0, Math.min(selectionStart, selectionEnd));
                end = Math.max(0, Math.max(selectionStart, selectionEnd));
            }

            switch (item.getItemId()) {
                case MENU_TRUE_BUTTON:
                    showAnnotationInput(article.getHeadline(false).toString(), Annotation.TYPE_HEADLINE, start, end,1);
                    mode.finish();
                    return true;
                case MENU_FALSE_BUTTON:
                    showAnnotationInput(article.getHeadline(false).toString(), Annotation.TYPE_HEADLINE, start, end,0);
                    mode.finish();
                    return true;
                default:
                    break;
            }

            return false;
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

        headline.setText(article.getHeadline(showHeatmap));
        body.setTextWithClickableSentences(article.getBody(showHeatmap, -1, -1));
    }

    public void updateAnnotations() {
        headline.setText(article.getHeadline(showHeatmap));
        body.setTextWithClickableSentences(article.getBody(showHeatmap, -1, -1));

        bodyOverflow.setVisibility(View.GONE);
        bodyOverflow.setTextWithClickableSentences(new SpannableString(""));
    }

    private void showLocatedAnnotations(String type, int startIndex, int endIndex) {
        ArrayList<Annotation> annotations = article.getLocatedAnnotations(type, startIndex, endIndex);

        annotationsContainer.setPageTransformer(true, new ZoomOutPageTransformer());

        AnnotationPagerAdapter adapter = new AnnotationPagerAdapter(getSupportFragmentManager(), annotations);
        annotationsContainer.setAdapter(adapter);

        splitBody(endIndex);
        annotationsContainer.setVisibility(View.VISIBLE);
    }

    private void splitBody(int index) {
        SpannableString firstSection = article.getBody(showHeatmap, 0, index);
        SpannableString secondSection = article.getBody(showHeatmap, index, article.getBodyLength());

        body.setTextWithClickableSentences(firstSection);
        bodyOverflow.setTextWithClickableSentences(secondSection);
        bodyOverflow.setVisibility(View.VISIBLE);
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
        updateAnnotations();
    }

    @Override
    public void onAnnotationSubmit(String type, int startIndex, int endIndex, int value, String comment) {
        AnnotationSubmitCallback cb = new AnnotationSubmitCallback() {
            @Override
            public void onSubmit() {
                updateAnnotations();
                annotationInputFragment.hideProgress();
                hideAnnotationInput();
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
    public void onAnnotationClose() {
        hideAnnotationInput();
    }

    @Override
    public void onTextViewClick(String quote, int startIndex, int endIndex) {
        //showAnnotationInput(quote, Annotation.TYPE_BODY, startIndex, endIndex,1);
        showLocatedAnnotations(Annotation.TYPE_BODY, startIndex, endIndex);
    }

    private class AnnotationPagerAdapter extends FragmentStatePagerAdapter {
        private ArrayList<Annotation> annotations;

        AnnotationPagerAdapter(FragmentManager fm, ArrayList<Annotation> annotations) {
            super(fm);
            this.annotations = annotations;
        }

        @Override
        public Fragment getItem(int position) {
            Annotation annotation = this.annotations.get(position);

            return AnnotationFragment.newInstance(
                    annotation.getUser().getUid(),
                    annotation.getComment(),
                    annotation.getValue()
            );
        }

        @Override
        public int getCount() {
            return this.annotations.size();
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
