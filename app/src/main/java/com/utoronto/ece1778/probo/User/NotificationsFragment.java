package com.utoronto.ece1778.probo.User;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
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

import jp.co.recruit_lifestyle.android.widget.WaveSwipeRefreshLayout;

public class NotificationsFragment extends Fragment {
    private WaveSwipeRefreshLayout swipeRefreshLayout;
    private LinearLayout noNotificationsContainer;
    private RecyclerView annotationsContainer;
    private ProgressBar progress;
    private ImageButton clearButton;

    private User.UserFragmentInteractionListener userInteractionListener;
    private NotificationsFragmentInteractionListener interactionListener;

    public NotificationsFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    private WaveSwipeRefreshLayout.OnRefreshListener handleRefresh = new WaveSwipeRefreshLayout.OnRefreshListener() {
        @Override
        public void onRefresh() {
            load();
        }
    };

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.notifications_action_menu, menu);

        MenuItem item = menu.findItem(R.id.clear_action);
        clearButton = item.getActionView().findViewById(R.id.clear);

        clearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clear();
            }
        });

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onDestroyView() {
        setHasOptionsMenu(false);
        super.onDestroyView();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_notifications, container, false);

        swipeRefreshLayout = v.findViewById(R.id.refresh);
        swipeRefreshLayout.setWaveARGBColor(255, 55, 64, 70);

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
                userInteractionListener.getUser().getNotifications(),
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

    private void clear() {
        final User user = userInteractionListener.getUser();

        User.UserClearNotificationsCallback cb = new User.UserClearNotificationsCallback() {
            @Override
            public void onClear() {
                userInteractionListener.updateUser(user);
                populate();
                clearButton.setEnabled(true);
            }

            @Override
            public void onError(Exception error) {
            }
        };

        clearButton.setEnabled(false);
        user.clearNotifications(cb);
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
    }
}
