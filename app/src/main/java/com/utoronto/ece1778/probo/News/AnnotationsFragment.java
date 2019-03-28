package com.utoronto.ece1778.probo.News;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import com.utoronto.ece1778.probo.R;
import com.utoronto.ece1778.probo.User.User;

public class AnnotationsFragment extends Fragment {
    private static final String
            ARG_ARTICLE_ID = "articleId",
            ARG_TYPE = "type",
            ARG_START_INDEX = "startIndex",
            ARG_END_INDEX = "endIndex";

    private Article article;
    private String type;
    private int startIndex;
    private int endIndex;

    private SwipeRefreshLayout swipeRefreshLayout;
    private ProgressBar spinner;
    private RecyclerView annotationsContainer;

    private User.UserFragmentInteractionListener userInteractionListener;
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
        swipeRefreshLayout.setOnRefreshListener(handleRefresh);
        annotationsContainer = v.findViewById(R.id.annotations_container);

        load();

        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        load();
    }

    private SwipeRefreshLayout.OnRefreshListener handleRefresh = new SwipeRefreshLayout.OnRefreshListener() {
        @Override
        public void onRefresh() {
            Article.ArticleCallback cb = new Article.ArticleCallback() {
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
        Article.ArticleCallback cb = new Article.ArticleCallback() {
            @Override
            public void onLoad() {
                populate();

                annotationsContainer.setVisibility(View.VISIBLE);
                spinner.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onArticleError(int errorCode) {
            }

            @Override
            public void onError(Exception e) {
            }
        };

        annotationsContainer.setVisibility(View.INVISIBLE);
        spinner.setVisibility(View.VISIBLE);

        article.load(cb);
    }

    private void populate() {
        AnnotationCardView.OnUserClickListener onUserClickListener = new AnnotationCardView.OnUserClickListener() {
            @Override
            public void onClick(User user) {
                if (interactionListener != null) {
                    interactionListener.onRouteToProfile(user.getUid());
                }
            }
        };

        AnnotationCardView.OnVoteListener onVoteListener = new AnnotationCardView.OnVoteListener() {
            @Override
            public void onVote(Annotation annotation) {
                if (interactionListener != null) {
                    interactionListener.onAnnotationVote(annotation);
                }
            }
        };

        AnnotationsRecyclerAdapter adapter = new AnnotationsRecyclerAdapter(
                article.getLocatedAnnotations(type, startIndex, endIndex),
                userInteractionListener.getUser(),
                onUserClickListener,
                onVoteListener
        );

        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this.getContext());
        annotationsContainer.setLayoutManager(layoutManager);
        annotationsContainer.setItemAnimator(new DefaultItemAnimator());
        annotationsContainer.setAdapter(adapter);
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

        if (parentFragment instanceof AnnotationsFragmentInteractionListener) {
            interactionListener = (AnnotationsFragmentInteractionListener) parentFragment;
        } else {
            throw new RuntimeException(parentFragment.toString()
                    + " must implement AnnotationsFragmentInteractionListener");
        }
    }

    public interface AnnotationsFragmentInteractionListener {
        void onAnnotationVote(Annotation annotation);
        void onRouteToProfile(String userId);
    }
}
