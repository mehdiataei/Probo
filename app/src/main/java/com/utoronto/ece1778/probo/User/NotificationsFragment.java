package com.utoronto.ece1778.probo.User;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import com.utoronto.ece1778.probo.News.Annotation;
import com.utoronto.ece1778.probo.News.AnnotationCardView;
import com.utoronto.ece1778.probo.News.AnnotationsRecyclerAdapter;
import com.utoronto.ece1778.probo.R;

import java.util.ArrayList;
import java.util.Collections;

import jp.co.recruit_lifestyle.android.widget.WaveSwipeRefreshLayout;

public class NotificationsFragment extends Fragment {
    private SwipeRefreshLayout swipeRefreshLayout;
    private LinearLayout noNotificationsContainer;
    private RecyclerView annotationsContainer;
    private ProgressBar progress;

    private User.UserFragmentInteractionListener userInteractionListener;
    private NotificationsFragmentInteractionListener interactionListener;

    public NotificationsFragment() {
    }

    private SwipeRefreshLayout.OnRefreshListener handleRefresh = new SwipeRefreshLayout.OnRefreshListener() {
        @Override
        public void onRefresh() {
            load();
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_notifications, container, false);

        swipeRefreshLayout = v.findViewById(R.id.refresh);
        swipeRefreshLayout.setColorSchemeColors(ResourcesCompat.getColor(getResources(), R.color.colorPrimary, null));


        noNotificationsContainer = v.findViewById(R.id.no_notifications_container);
        annotationsContainer = v.findViewById(R.id.annotations_container);
        progress = v.findViewById(R.id.progress_spinner);

        swipeRefreshLayout.setOnRefreshListener(handleRefresh);

        load();

        return v;
    }

    private void populate() {
        if (userInteractionListener.getUser().getNotifications().size() > 0) {
            noNotificationsContainer.setVisibility(View.GONE);
        } else {
            noNotificationsContainer.setVisibility(View.VISIBLE);
        }

        annotationsContainer.setVisibility(View.VISIBLE);

        ArrayList<Annotation> annotations = userInteractionListener.getUser().getNotifications();
        Collections.reverse(annotations);

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

        AnnotationsRecyclerAdapter.OnGoToAnnotationInterface onGoToAnnotationInterface = new AnnotationsRecyclerAdapter.OnGoToAnnotationInterface() {
            @Override
            public void onGoToAnnotation(Annotation annotation) {
                if (interactionListener != null) {
                    interactionListener.onGoToAnnotation(annotation);
                }
            }
        };

        AnnotationsRecyclerAdapter adapter = new AnnotationsRecyclerAdapter(
                annotations,
                userInteractionListener.getUser(),
                onUserClickListener,
                onVoteListener,
                onGoToAnnotationInterface
        );

        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this.getContext());
        annotationsContainer.setLayoutManager(layoutManager);
        annotationsContainer.setItemAnimator(new DefaultItemAnimator());
        annotationsContainer.setAdapter(adapter);
    }

    private void load() {
        final User user = userInteractionListener.getUser();

        User.UserNotificationsCallback cb = new User.UserNotificationsCallback() {
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

        user.loadNotifications(cb);
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

        if (context instanceof NotificationsFragmentInteractionListener) {
            interactionListener = (NotificationsFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement NotificationsFragmentInteractionListener");
        }
    }

    public interface NotificationsFragmentInteractionListener {
        void onAnnotationVote(Annotation annotation);
        void onRouteToProfile(String userId);
        void onGoToAnnotation(Annotation annotation);
    }
}
