package com.utoronto.ece1778.probo.News;

import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;

import com.utoronto.ece1778.probo.User.User;
import com.utoronto.ece1778.probo.R;

import java.util.ArrayList;

public class AnnotationsActivity extends AppCompatActivity
        implements AnnotationFragment.AnnotationFragmentInteractionListener {

    private User user;

    private Article article;
    private String type;
    private int startIndex;
    private int endIndex;

    private SwipeRefreshLayout swipeRefreshLayout;
    private ProgressBar spinner;
    private ScrollView scrollView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_annotations);

        swipeRefreshLayout = findViewById(R.id.refresh);
        spinner = findViewById(R.id.progress_spinner);
        scrollView = findViewById(R.id.scroll);

        Bundle extras = getIntent().getExtras();

        user = new User();

        if (extras != null) {
            article = new Article(extras.getString("articleId"));
            type = extras.getString("type");
            startIndex = extras.getInt("startIndex");
            endIndex = extras.getInt("endIndex");

            load();
        }

        swipeRefreshLayout.setOnRefreshListener(handleRefresh);
    }

    private SwipeRefreshLayout.OnRefreshListener handleRefresh = new SwipeRefreshLayout.OnRefreshListener() {
        @Override
        public void onRefresh() {
            ArticleCallback cb = new ArticleCallback() {
                @Override
                public void onLoad() {
                    populate();

                    swipeRefreshLayout.setRefreshing(false);
                }

                @Override
                public void onArticleError(int errorCode) {
                }

                @Override
                public void onError(Exception e) {
                }
            };

            article.load(cb);
        }
    };

    private void load() {
        ArticleCallback cb = new ArticleCallback() {
            @Override
            public void onLoad() {
                populate();

                scrollView.setVisibility(View.VISIBLE);
                spinner.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onArticleError(int errorCode) {
            }

            @Override
            public void onError(Exception e) {
            }
        };

        article.load(cb);
    }

    private void populate() {
        ArrayList<Annotation> annotations = article.getLocatedAnnotations(type, startIndex, endIndex);
        FragmentManager manager = getSupportFragmentManager();
        FragmentTransaction transaction = manager.beginTransaction();

        LinearLayout linearLayout = findViewById(R.id.annotations_container);
        linearLayout.removeAllViews();

        for (Annotation annotation : annotations) {
            AnnotationFragment annotationFragment = AnnotationFragment.newInstance(
                    annotation.getId(),
                    annotation.getUser().getUid(),
                    annotation.getComment(),
                    annotation.getValue(),
                    annotation.getUpvoteCount(),
                    annotation.getDownvoteCount(),
                    annotation.userHasUpvoted(user),
                    annotation.userHasDownvoted(user)
            );

            transaction.add(R.id.annotations_container, annotationFragment);
        }

        transaction.commit();
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
}
