package com.utoronto.ece1778.probo.User;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.utoronto.ece1778.probo.News.Annotation;
import com.utoronto.ece1778.probo.News.AnnotationFragment;
import com.utoronto.ece1778.probo.News.AnnotationVote;
import com.utoronto.ece1778.probo.R;
import com.utoronto.ece1778.probo.Utils.ImageBitmap;
import com.utoronto.ece1778.probo.Utils.ImageLoader;

public class ProfileFragment extends Fragment {

    private static final String ARG_USER_ID = "userId";

    private SwipeRefreshLayout refreshLayout;
    private ProgressBar annotationsProgress;
    private LinearLayout annotationsContainer;
    private RelativeLayout profileImageProgressContainer;
    private ImageView profileImage;
    private TextView nameText;
    private TextView titleText;

    private User user;

    public ProfileFragment() {
    }
    public static ProfileFragment newInstance(String userId) {
        ProfileFragment fragment = new ProfileFragment();
        Bundle args = new Bundle();
        args.putString(ARG_USER_ID, userId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            user = new User(getArguments().getString(ARG_USER_ID));
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_profile, container, false);

        refreshLayout = v.findViewById(R.id.refresh);
        annotationsProgress = v.findViewById(R.id.annotations_progress);
        annotationsContainer = v.findViewById(R.id.annotations_container);
        profileImageProgressContainer = v.findViewById(R.id.profile_image_progress_container);
        profileImage = v.findViewById(R.id.profile_image);
        nameText = v.findViewById(R.id.name);
        titleText = v.findViewById(R.id.title);

        User.UserCallback cb = new User.UserCallback() {
            @Override
            public void onLoad() {
                populate();
            }

            @Override
            public void onError(Exception error) {
            }
        };

        user.load(cb);
        loadAnnotations();

        refreshLayout.setOnRefreshListener(handleRefresh);

        return v;
    }

    private SwipeRefreshLayout.OnRefreshListener handleRefresh = new SwipeRefreshLayout.OnRefreshListener() {
        @Override
        public void onRefresh() {
            User.UserCallback cb = new User.UserCallback() {
                @Override
                public void onLoad() {
                    populate();
                    loadAnnotations();
                    refreshLayout.setRefreshing(false);
                }

                @Override
                public void onError(Exception error) {
                }
            };

            user.load(cb);
        }
    };

    private void populate() {
        ImageLoader.ImageLoaderCallback cb = new ImageLoader.ImageLoaderCallback() {
            @Override
            public void onSuccess(Bitmap image) {
                ImageBitmap imageBitmap = new ImageBitmap(image);
                RoundedBitmapDrawable roundedImage = imageBitmap.getCroppedRoundedBitmapDrawable(getResources());

                profileImage.setImageDrawable(roundedImage);
            }

            @Override
            public void onFailure(Exception e) {}

            @Override
            public void onComplete() {
                profileImage.setVisibility(View.VISIBLE);
                profileImageProgressContainer.setVisibility(View.INVISIBLE);
            }
        };

        if (user.getProfileImagePath() != null) {
            ImageLoader imageLoader = new ImageLoader(
                    user.getProfileImagePath(),
                    getActivity().getApplicationContext()
            );

            imageLoader.load(cb);
        } else {
            profileImage.setVisibility(View.VISIBLE);
            profileImageProgressContainer.setVisibility(View.INVISIBLE);
        }

        nameText.setText(user.getName());

        if (user.getTitle() != null) {
            titleText.setText(user.getTitle());
            titleText.setVisibility(View.VISIBLE);
        } else {
            titleText.setVisibility(View.GONE);
        }
    }

    private void loadAnnotations() {
        User.UserAnnotationsCallback cb = new User.UserAnnotationsCallback() {
            @Override
            public void onLoad() {
                populateAnnotations();
                annotationsProgress.setVisibility(View.GONE);
            }

            @Override
            public void onError(Exception error) {
            }
        };

        annotationsContainer.removeAllViews();
        annotationsProgress.setVisibility(View.VISIBLE);

        user.loadAnnotations(cb);
    }

    private void populateAnnotations() {
        FragmentManager manager = getActivity().getSupportFragmentManager();
        FragmentTransaction transaction = manager.beginTransaction();

        for (Annotation annotation : user.getAnnotations()) {
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
        for (Annotation annotation : user.getAnnotations()) {
            if (annotation.getId().equals(id)) {
                annotation.vote(cb, user, value);
                return;
            }
        }
    }
}
