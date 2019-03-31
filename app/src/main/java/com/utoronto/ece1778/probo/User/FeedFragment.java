package com.utoronto.ece1778.probo.User;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import com.utoronto.ece1778.probo.News.Annotation;
import com.utoronto.ece1778.probo.News.AnnotationCardView;
import com.utoronto.ece1778.probo.News.AnnotationsRecyclerAdapter;
import com.utoronto.ece1778.probo.R;

import jp.co.recruit_lifestyle.android.widget.WaveSwipeRefreshLayout;

public class FeedFragment extends Fragment {
    private WaveSwipeRefreshLayout swipeRefreshLayout;
    private LinearLayout noAnnotationsContainer;
    private RecyclerView annotationsContainer;
    private ProgressBar progress;
    private ImageButton clearButton;

    private User.UserFragmentInteractionListener userInteractionListener;
    private FeedFragmentInteractionListener interactionListener;

    public FeedFragment() {
    }

    private WaveSwipeRefreshLayout.OnRefreshListener handleRefresh = new WaveSwipeRefreshLayout.OnRefreshListener() {
        @Override
        public void onRefresh() {
            load();
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_feed, container, false);

        swipeRefreshLayout = v.findViewById(R.id.refresh);
        swipeRefreshLayout.setWaveARGBColor(255, 55, 64, 70);
        swipeRefreshLayout.setColorSchemeColors(Color.WHITE);
        noAnnotationsContainer = v.findViewById(R.id.no_annotations_container);
        annotationsContainer = v.findViewById(R.id.annotations_container);
        progress = v.findViewById(R.id.progress_spinner);

        swipeRefreshLayout.setOnRefreshListener(handleRefresh);

        load();

        return v;
    }

    private void populate() {
        if (userInteractionListener.getUser().getFeed().size() > 0) {
            noAnnotationsContainer.setVisibility(View.GONE);
        } else {
            noAnnotationsContainer.setVisibility(View.VISIBLE);
        }

        annotationsContainer.setVisibility(View.VISIBLE);

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
                userInteractionListener.getUser().getFeed(),
                userInteractionListener.getUser(),
                onUserClickListener,
                onVoteListener
        );

        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this.getContext());
        annotationsContainer.setLayoutManager(layoutManager);
        annotationsContainer.setItemAnimator(new DefaultItemAnimator());
        annotationsContainer.setAdapter(adapter);
    }

    private void load() {
        final User user = userInteractionListener.getUser();

        User.UserFeedCallback cb = new User.UserFeedCallback() {
            @Override
            public void onLoad() {
                userInteractionListener.updateUser(user);

                progress.setVisibility(View.GONE);
                swipeRefreshLayout.setRefreshing(false);

                populate();
            }

            @Override
            public void onError(Exception error) {
            }
        };

        user.loadFeed(cb);
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

        if (context instanceof FeedFragmentInteractionListener) {
            interactionListener = (FeedFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement FeedFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        interactionListener = null;
    }

    public interface FeedFragmentInteractionListener {
        void onAnnotationVote(Annotation annotation);
        void onRouteToProfile(String userId);
    }
}
