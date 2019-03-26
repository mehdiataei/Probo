package com.utoronto.ece1778.probo.User;

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
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import com.utoronto.ece1778.probo.News.Annotation;
import com.utoronto.ece1778.probo.News.AnnotationCardView;
import com.utoronto.ece1778.probo.News.AnnotationsRecyclerAdapter;
import com.utoronto.ece1778.probo.R;

public class NotificationsFragment extends Fragment {
    private User user;

    private SwipeRefreshLayout swipeRefreshLayout;
    private ProgressBar spinner;
    private LinearLayout noNotificationsContainer;
    private RecyclerView annotationsContainer;

    private NotificationsFragmentInteractionListener interactionListener;

    public NotificationsFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_notifications, container, false);

        swipeRefreshLayout = v.findViewById(R.id.refresh);
        spinner = v.findViewById(R.id.progress_spinner);
        noNotificationsContainer = v.findViewById(R.id.no_notifications_container);
        annotationsContainer = v.findViewById(R.id.annotations_container);

        swipeRefreshLayout.setOnRefreshListener(handleRefresh);

        user = new User();

        User.UserCallback cb = new User.UserCallback() {
            @Override
            public void onLoad() {
                load();
            }

            @Override
            public void onError(Exception error) {
            }
        };

        user.load(cb);

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
        }
    };

    private void load() {
        User.UserAnnotationsCallback cb = new User.UserAnnotationsCallback() {
            @Override
            public void onLoad() {
                if (user.getSubscriptions().size() > 0) {
                    noNotificationsContainer.setVisibility(View.GONE);
                    populate();
                } else {
                    noNotificationsContainer.setVisibility(View.VISIBLE);
                }

                annotationsContainer.setVisibility(View.VISIBLE);
                spinner.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onError(Exception error) {
            }
        };

        annotationsContainer.setVisibility(View.INVISIBLE);
        spinner.setVisibility(View.VISIBLE);

        user.loadSubscriptions(cb);
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
                user.getSubscriptions(),
                user,
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
    }
}
