package com.utoronto.ece1778.probo.News;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;

import com.utoronto.ece1778.probo.R;
import com.utoronto.ece1778.probo.User.User;

import java.util.ArrayList;

public class AnnotationsFragment extends Fragment
        implements AnnotationFragment.AnnotationFragmentInteractionListener {

    private static final String
            ARG_ARTICLE_ID = "articleId",
            ARG_TYPE = "type",
            ARG_START_INDEX = "startIndex",
            ARG_END_INDEX = "endIndex";

    private User user;

    private Article article;
    private String type;
    private int startIndex;
    private int endIndex;

    private SwipeRefreshLayout swipeRefreshLayout;
    private ProgressBar spinner;
    private ScrollView scrollView;
    private LinearLayout linearLayout;

    private AnnotationsFragmentInteractionListener interactionListener;

    public AnnotationsFragment() {
    }

    public static AnnotationsFragment newInstance(String articleId, String type, int startIndex, int endIndex) {
        AnnotationsFragment fragment = new AnnotationsFragment();
        Bundle args = new Bundle();

        args.putString(ARG_ARTICLE_ID, articleId);
        args.putString(ARG_TYPE, type);
        args.putInt(ARG_START_INDEX, startIndex);
        args.putInt(ARG_END_INDEX, endIndex);

        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            article = new Article(getArguments().getString(ARG_ARTICLE_ID));
            type = getArguments().getString(ARG_TYPE);
            startIndex = getArguments().getInt(ARG_START_INDEX);
            endIndex = getArguments().getInt(ARG_END_INDEX);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_annotations, container, false);

        swipeRefreshLayout = v.findViewById(R.id.refresh);
        spinner = v.findViewById(R.id.progress_spinner);
        scrollView = v.findViewById(R.id.scroll);
        linearLayout = v.findViewById(R.id.annotations_container);

        swipeRefreshLayout.setOnRefreshListener(handleRefresh);

        user = new User();

        load();

        return v;
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
        FragmentManager manager = getChildFragmentManager();
        FragmentTransaction transaction = manager.beginTransaction();

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

    public void onAnnotationVote(AnnotationVote.AnnotationVoteCallback cb, String id, boolean value) {
        for (Annotation annotation : article.getAnnotations()) {
            if (annotation.getId().equals(id)) {
                annotation.vote(cb, user, value);
                return;
            }
        }
    }

    @Override
    public void onRouteToProfile(String userId) {
        if (interactionListener != null) {
            interactionListener.onRouteToProfile(userId);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        Fragment parentFragment = getParentFragment();

        if (parentFragment instanceof AnnotationsFragmentInteractionListener) {
            interactionListener = (AnnotationsFragmentInteractionListener) parentFragment;
        } else {
            throw new RuntimeException(parentFragment.toString()
                    + " must implement AnnotationsFragmentInteractionListener");
        }
    }

    public interface AnnotationsFragmentInteractionListener {
        void onRouteToProfile(String userId);
    }
}
