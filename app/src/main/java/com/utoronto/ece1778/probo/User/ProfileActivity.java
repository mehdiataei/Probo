package com.utoronto.ece1778.probo.User;

import android.graphics.Bitmap;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
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

import java.util.ArrayList;

public class ProfileActivity extends AppCompatActivity
        implements AnnotationFragment.AnnotationFragmentInteractionListener {

    private SwipeRefreshLayout refreshLayout;

    private User user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        refreshLayout = findViewById(R.id.refresh);

        Bundle extras = getIntent().getExtras();

        if (extras != null) {
            User.UserCallback cb = new User.UserCallback() {
                @Override
                public void onLoad() {
                    populate();
                }

                @Override
                public void onError(Exception error) {
                }
            };

            user = new User(extras.getString("userId"));
            user.load(cb);

            loadAnnotations();
        }

        refreshLayout.setOnRefreshListener(handleRefresh);
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
        final RelativeLayout profileImageProgressContainer = findViewById(R.id.profile_image_progress_container);
        final ImageView profileImage = findViewById(R.id.profile_image);
        final TextView nameText = findViewById(R.id.name);
        final TextView titleText = findViewById(R.id.title);

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
            ImageLoader imageLoader = new ImageLoader(user.getProfileImagePath(), getApplicationContext());
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
        final ProgressBar annotationsProgress = findViewById(R.id.annotations_progress);
        final LinearLayout annotationsContainer = findViewById(R.id.annotations_container);

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
        FragmentManager manager = getSupportFragmentManager();
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

    @Override
    public void onAnnotationVote(AnnotationVote.AnnotationVoteCallback cb, String id, boolean value) {
        for (Annotation annotation : user.getAnnotations()) {
            if (annotation.getId().equals(id)) {
                annotation.vote(cb, user, value);
                return;
            }
        }
    }
}
