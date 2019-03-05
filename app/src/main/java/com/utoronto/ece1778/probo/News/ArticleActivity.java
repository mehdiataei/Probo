package com.utoronto.ece1778.probo.News;

import android.drm.DrmStore;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Build;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
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
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.Priority;
import com.bumptech.glide.request.RequestOptions;
import com.utoronto.ece1778.probo.Login.User;
import com.utoronto.ece1778.probo.R;
import com.utoronto.ece1778.probo.Utils.GlideImageLoader;
import com.utoronto.ece1778.probo.Utils.SquareImageView;

import java.text.DateFormat;
import java.util.Locale;

public class ArticleActivity extends AppCompatActivity
        implements AnnotationInputFragment.AnnotationInputFragmentInteractionListener {

    private SwipeRefreshLayout refresh;
    private FrameLayout annotationContainer;
    private Article article;

    private TextView headline;
    private TextView body;

    private AnnotationInputFragment annotationInputFragment;

    private User user;

    private static final int
            MENU_TRUE_BUTTON = 0,
            MENU_FALSE_BUTTON = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_article);

        refresh = findViewById(R.id.refresh);
        annotationContainer = findViewById(R.id.annotation_container);

        headline = findViewById(R.id.headline);
        body = findViewById(R.id.body);

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
        body.setCustomSelectionActionModeCallback(handleBodyTextSelect);

        refresh.setOnRefreshListener(handleRefresh);
    }

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
                    showAnnotationInput(Annotation.TYPE_HEADLINE, start, end,1);
                    mode.finish();
                    return true;
                case MENU_FALSE_BUTTON:
                    showAnnotationInput(Annotation.TYPE_HEADLINE, start, end,0);
                    mode.finish();
                    return true;
                default:
                    break;
            }

            return false;
        }
    };

    private ActionMode.Callback handleBodyTextSelect = new ActionMode.Callback() {
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
            int end = body.getText().length();

            if (body.isFocused()) {
                final int selectionStart = body.getSelectionStart();
                final int selectionEnd = body.getSelectionEnd();

                start = Math.max(0, Math.min(selectionStart, selectionEnd));
                end = Math.max(0, Math.max(selectionStart, selectionEnd));
            }

            switch (item.getItemId()) {
                case MENU_TRUE_BUTTON:
                    showAnnotationInput(Annotation.TYPE_BODY, start, end,1);
                    mode.finish();
                    return true;
                case MENU_FALSE_BUTTON:
                    showAnnotationInput(Annotation.TYPE_BODY, start, end, 0);
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
        headline.setText(article.getHeadline());
        body.setText(article.getBody());
    }

    public void updateAnnotations() {
        headline.setText(article.getHeadline());
        body.setText(article.getBody());
    }

    private void showAnnotationInput(String type, int startIndex, int endIndex, int value) {
        Animation animation = AnimationUtils.loadAnimation(this, R.anim.annotation_input_slide_in);
        FragmentManager manager = getSupportFragmentManager();
        FragmentTransaction transaction = manager.beginTransaction();

        annotationInputFragment = AnnotationInputFragment.newInstance(
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
                annotationContainer.setVisibility(View.INVISIBLE);

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

    @Override
    public void onAnnotationSubmit(String type, int startIndex, int endIndex, int value, String comment) {
        if (type.equals(Annotation.TYPE_HEADLINE)) {
            article.addHeadlineAnnotation(
                    user,
                    startIndex,
                    endIndex,
                    value,
                    comment
            );
        } else {
            article.addBodyAnnotation(
                    user,
                    startIndex,
                    endIndex,
                    value,
                    comment
            );
        }

        updateAnnotations();
        hideAnnotationInput();
    }

    @Override
    public void onAnnotationClose() {
        hideAnnotationInput();
    }
}
