package com.utoronto.ece1778.probo.News;

import android.drm.DrmStore;
import android.graphics.Color;
import android.os.Build;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Html;
import android.text.SpannableString;
import android.text.style.BackgroundColorSpan;
import android.util.Log;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.Priority;
import com.bumptech.glide.request.RequestOptions;
import com.utoronto.ece1778.probo.Login.User;
import com.utoronto.ece1778.probo.R;
import com.utoronto.ece1778.probo.Utils.ClickableTextView;
import com.utoronto.ece1778.probo.Utils.GlideImageLoader;
import com.utoronto.ece1778.probo.Utils.SquareImageView;

import java.text.DateFormat;
import java.util.Locale;

public class ArticleActivity extends AppCompatActivity {
    private SwipeRefreshLayout refresh;
    private Article article;

    private TextView headline;
    private ClickableTextView body;

    private User user;

    private static final int
            MENU_TRUE_BUTTON = 0,
            MENU_FALSE_BUTTON = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_article);

        refresh = findViewById(R.id.refresh);

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
//            menu.clear();

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
                    article.addHeadlineAnnotation(
                            user,
                            start,
                            end,
                            1
                    );

                    updateAnnotations();

                    mode.finish();

                    return true;
                case MENU_FALSE_BUTTON:
                    article.addHeadlineAnnotation(
                            user,
                            start,
                            end,
                            0
                    );

                    updateAnnotations();

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
                    article.addBodyAnnotation(
                            user,
                            start,
                            end,
                            1
                    );

                    updateAnnotations();

                    mode.finish();

                    return true;
                case MENU_FALSE_BUTTON:
                    article.addBodyAnnotation(
                            user,
                            start,
                            end,
                            0
                    );

                    updateAnnotations();

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
        body.setTextWithClickableSentences(article.getRawBody().replace("\\n", System.getProperty("line.separator")).replace("\\", ""));
        //body.setText(article.getBody());
    }

    public void updateAnnotations() {
        headline.setText(article.getHeadline());
        body.setText(article.getBody());
    }
}
