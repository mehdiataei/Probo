package com.utoronto.ece1778.probo.User;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
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

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.utoronto.ece1778.probo.News.Annotation;
import com.utoronto.ece1778.probo.News.AnnotationCardView;
import com.utoronto.ece1778.probo.News.AnnotationsRecyclerAdapter;
import com.utoronto.ece1778.probo.R;

import java.util.ArrayList;

public class NotificationsFragment extends Fragment {
    private SwipeRefreshLayout swipeRefreshLayout;
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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_notifications, container, false);

        swipeRefreshLayout = v.findViewById(R.id.refresh);
        noNotificationsContainer = v.findViewById(R.id.no_notifications_container);
        annotationsContainer = v.findViewById(R.id.annotations_container);
        progress = v.findViewById(R.id.progress_spinner);

        swipeRefreshLayout.setOnRefreshListener(handleRefresh);

        load();

        return v;
    }

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

    private SwipeRefreshLayout.OnRefreshListener handleRefresh = new SwipeRefreshLayout.OnRefreshListener() {
        @Override
        public void onRefresh() {
            load();
        }
    };

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
