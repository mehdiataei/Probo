package com.utoronto.ece1778.probo.User;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.utoronto.ece1778.probo.News.Annotation;
import com.utoronto.ece1778.probo.News.AnnotationCardView;
import com.utoronto.ece1778.probo.News.AnnotationsRecyclerAdapter;
import com.utoronto.ece1778.probo.R;
import com.utoronto.ece1778.probo.Utils.ImageBitmap;
import com.utoronto.ece1778.probo.Utils.ImageLoader;

public class ProfileFragment extends Fragment {
    private static final String ARG_USER_ID = "userId";

    private SwipeRefreshLayout refreshLayout;
    private ProgressBar annotationsProgress;
    private LinearLayout noAnnotationsContainer;
    private RecyclerView annotationsContainer;
    private RelativeLayout profileImageProgressContainer;
    private ImageView profileImage;
    private TextView nameText;
    private TextView titleText;

    private User profileUser;

    private User.UserFragmentInteractionListener userInteractionListener;
    private ProfileFragmentInteractionListener interactionListener;

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
            profileUser = new User(getArguments().getString(ARG_USER_ID));
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_profile, container, false);

        refreshLayout = v.findViewById(R.id.refresh);
        annotationsProgress = v.findViewById(R.id.annotations_progress);
        noAnnotationsContainer = v.findViewById(R.id.no_annotations_container);
        annotationsContainer = v.findViewById(R.id.annotations_container);
        profileImageProgressContainer = v.findViewById(R.id.profile_image_progress_container);
        profileImage = v.findViewById(R.id.profile_image);
        nameText = v.findViewById(R.id.name);
        titleText = v.findViewById(R.id.title);

        User.UserCallback profileCb = new User.UserCallback() {
            @Override
            public void onLoad() {
                populate();
            }

            @Override
            public void onError(Exception error) {
            }
        };

        if (profileUser.equals(userInteractionListener.getUser())) {
            profileUser = userInteractionListener.getUser();
            profileCb.onLoad();
        } else {
            profileUser.load(profileCb);
        }

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
                    if (profileUser.equals(userInteractionListener.getUser())) {
                        userInteractionListener.updateUser(profileUser);
                    }

                    populate();
                    loadAnnotations();
                    refreshLayout.setRefreshing(false);
                }

                @Override
                public void onError(Exception error) {
                }
            };

            profileUser.load(cb);
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

        if (profileUser.getProfileImagePath() != null) {
            ImageLoader imageLoader = new ImageLoader(
                    profileUser.getProfileImagePath(),
                    getActivity().getApplicationContext()
            );

            imageLoader.load(cb);
        } else {
            profileImage.setVisibility(View.VISIBLE);
            profileImageProgressContainer.setVisibility(View.INVISIBLE);
        }

        nameText.setText(profileUser.getName());

        if (profileUser.getTitle() != null) {
            titleText.setText(profileUser.getTitle());
            titleText.setVisibility(View.VISIBLE);
        } else {
            titleText.setVisibility(View.GONE);
        }
    }

    private void loadAnnotations() {
        User.UserAnnotationsCallback cb = new User.UserAnnotationsCallback() {
            @Override
            public void onLoad() {
                if (profileUser.getAnnotations().size() > 0) {
                    noAnnotationsContainer.setVisibility(View.GONE);
                    populateAnnotations();
                } else {
                    noAnnotationsContainer.setVisibility(View.VISIBLE);
                }

                annotationsProgress.setVisibility(View.GONE);
            }

            @Override
            public void onError(Exception error) {
            }
        };

        annotationsContainer.removeAllViews();

        profileUser.loadAnnotations(cb);
    }

    private void populateAnnotations() {
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
                profileUser.updateAnnotation(annotation);
            }
        };

        AnnotationsRecyclerAdapter adapter = new AnnotationsRecyclerAdapter(
                profileUser.getAnnotations(),
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

        if (context instanceof ProfileFragmentInteractionListener) {
            interactionListener = (ProfileFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement ProfileFragmentInteractionListener");
        }
    }

    public interface ProfileFragmentInteractionListener {
        void onRouteToProfile(String userId);
    }
}
