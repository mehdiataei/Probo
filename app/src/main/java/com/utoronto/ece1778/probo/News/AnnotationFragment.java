package com.utoronto.ece1778.probo.News;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.graphics.ColorUtils;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v7.widget.CardView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.utoronto.ece1778.probo.Login.User;
import com.utoronto.ece1778.probo.R;
import com.utoronto.ece1778.probo.Utils.ImageBitmap;
import com.utoronto.ece1778.probo.Utils.ImageLoader;

import org.w3c.dom.Text;

import java.util.Locale;

public class AnnotationFragment extends Fragment {
    private static final String ARG_ID = "id",
                                ARG_USER_ID = "userId",
                                ARG_COMMENT = "comment",
                                ARG_VALUE = "value",
                                ARG_UPVOTE_COUNT = "upvoteCount",
                                ARG_DOWNVOTE_COUNT = "downvoteCount",
                                ARG_USER_HAS_UPVOTED = "userHasUpvoted",
                                ARG_USER_HAS_DOWNVOTED = "userHasDownvoted";

    private User user;

    private String id;
    private String userId;
    private String comment;
    private int value;
    private int upvoteCount;
    private int downvoteCount;
    private boolean userHasUpvoted;
    private boolean userHasDownvoted;

    private CardView card;
    private ProgressBar profileImageProgress;
    private ImageView profileImage;
    private TextView nameText;
    private TextView titleText;
    private ImageButton upvoteButton;
    private ImageButton downvoteButton;
    private ProgressBar upvoteProgress;
    private ProgressBar downvoteProgress;
    private TextView upvoteCountText;
    private TextView downvoteCountText;

    private AnnotationFragmentInteractionListener interactionListener;

    public AnnotationFragment() {}

    public static AnnotationFragment newInstance(String id, String userId, String comment, int value,
                                                 int upvoteCount, int downvoteCount, boolean userHasUpvoted,
                                                 boolean userHasDownvoted) {

        AnnotationFragment fragment = new AnnotationFragment();
        Bundle args = new Bundle();

        args.putString(ARG_ID, id);
        args.putString(ARG_USER_ID, userId);
        args.putString(ARG_COMMENT, comment);
        args.putInt(ARG_VALUE, value);
        args.putInt(ARG_UPVOTE_COUNT, upvoteCount);
        args.putInt(ARG_DOWNVOTE_COUNT, downvoteCount);
        args.putBoolean(ARG_USER_HAS_UPVOTED, userHasUpvoted);
        args.putBoolean(ARG_USER_HAS_DOWNVOTED, userHasDownvoted);

        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            id = getArguments().getString(ARG_ID);
            userId = getArguments().getString(ARG_USER_ID);
            comment = getArguments().getString(ARG_COMMENT);
            value = getArguments().getInt(ARG_VALUE);
            upvoteCount = getArguments().getInt(ARG_UPVOTE_COUNT);
            downvoteCount = getArguments().getInt(ARG_DOWNVOTE_COUNT);
            userHasUpvoted = getArguments().getBoolean(ARG_USER_HAS_UPVOTED);
            userHasDownvoted = getArguments().getBoolean(ARG_USER_HAS_DOWNVOTED);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_annotation, container, false);

        card = v.findViewById(R.id.card);
        profileImageProgress = v.findViewById(R.id.profile_image_progress);
        profileImage = v.findViewById(R.id.profile_image);
        nameText = v.findViewById(R.id.name);
        titleText = v.findViewById(R.id.title);
        TextView commentText = v.findViewById(R.id.comment);
        upvoteButton = v.findViewById(R.id.upvote);
        downvoteButton = v.findViewById(R.id.downvote);
        upvoteProgress = v.findViewById(R.id.upvote_progress);
        downvoteProgress = v.findViewById(R.id.downvote_progress);
        upvoteCountText = v.findViewById(R.id.upvote_count);
        downvoteCountText = v.findViewById(R.id.downvote_count);

        User.UserCallback cb = new User.UserCallback() {
            @Override
            public void onLoad() {
                populate();
            }

            @Override
            public void onError(Exception error) {
            }
        };

        setBackgroundColor();
        commentText.setText(comment);

        updateVoteCounts();
        updateVoteDrawables();

        user = new User(userId);
        user.load(cb);

        upvoteButton.setOnClickListener(handleVoteClick);
        downvoteButton.setOnClickListener(handleVoteClick);

        return v;
    }

    private View.OnClickListener handleVoteClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            handleVote((ImageButton) v, v.getId() == R.id.upvote);
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
                profileImageProgress.setVisibility(View.INVISIBLE);
                profileImage.setVisibility(View.VISIBLE);
            }
        };

        if (user.getProfileImagePath() != null) {
            ImageLoader imageLoader = new ImageLoader(
                    user.getProfileImagePath(),
                    getActivity().getApplicationContext()
            );

            imageLoader.load(cb);
        } else {
            profileImageProgress.setVisibility(View.INVISIBLE);
            profileImage.setVisibility(View.VISIBLE);
        }

        nameText.setText(user.getName());

        if (user.getTitle() != null) {
            titleText.setText(user.getTitle());
            titleText.setVisibility(View.VISIBLE);
        }
    }

    private void setBackgroundColor() {
        int color = value == 1 ? Color.GREEN : Color.RED;
        int formattedColor = ColorUtils.setAlphaComponent(color, 125);
        card.setCardBackgroundColor(formattedColor);
    }

    private void handleVote(final ImageButton button, final boolean value) {
        if (interactionListener != null) {
            AnnotationVoteCallback cb = new AnnotationVoteCallback() {
                @Override
                public void onSubmit(boolean hasVote, boolean currentValue, int numUpvotes, int numDownvotes) {
                    upvoteCount = numUpvotes;
                    downvoteCount = numDownvotes;

                    userHasUpvoted = hasVote && currentValue;
                    userHasDownvoted = hasVote && !currentValue;

                    updateVoteCounts();
                    hideVoteButtonLoading(button);
                    updateVoteDrawables();
                    enableVoteButtons();
                }

                @Override
                public void onError(Exception e) {
                    Toast toast = Toast.makeText(
                            getActivity().getApplicationContext(),
                            getString(R.string.annotation_fragment_error_general),
                            Toast.LENGTH_LONG
                    );

                    toast.show();
                }
            };

            disableVoteButtons();
            showVoteButtonLoading(button);
            interactionListener.onAnnotationVote(cb, id, value);
        }
    }

    private void updateVoteDrawables() {
        if (userHasUpvoted) {
            upvoteButton.setImageResource(R.drawable.thumb_up_icon_light);
            downvoteButton.setImageResource(R.drawable.thumb_down_icon);
        } else if (userHasDownvoted) {
            upvoteButton.setImageResource(R.drawable.thumb_up_icon);
            downvoteButton.setImageResource(R.drawable.thumb_down_icon_light);
        } else {
            upvoteButton.setImageResource(R.drawable.thumb_up_icon);
            downvoteButton.setImageResource(R.drawable.thumb_down_icon);
        }
    }

    private void updateVoteCounts() {
        upvoteCountText.setText(String.format(Locale.CANADA, "%d", upvoteCount));
        downvoteCountText.setText(String.format(Locale.CANADA, "%d", downvoteCount));
    }

    private void enableVoteButtons() {
        upvoteButton.setEnabled(true);
        downvoteButton.setEnabled(true);
    }

    private void disableVoteButtons() {
        upvoteButton.setEnabled(false);
        downvoteButton.setEnabled(false);
    }

    private void showVoteButtonLoading(ImageButton button) {
        ProgressBar progress = button.getId() == R.id.upvote ? upvoteProgress : downvoteProgress;

        progress.setVisibility(View.VISIBLE);
        button.setVisibility(View.INVISIBLE);
    }

    private void hideVoteButtonLoading(ImageButton button) {
        ProgressBar progress = button.getId() == R.id.upvote ? upvoteProgress : downvoteProgress;

        progress.setVisibility(View.INVISIBLE);
        button.setVisibility(View.VISIBLE);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (context instanceof AnnotationFragmentInteractionListener) {
            interactionListener = (AnnotationFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement AnnotationFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        interactionListener = null;
    }

    public interface AnnotationFragmentInteractionListener {
        void onAnnotationVote(AnnotationVoteCallback cb, String id, boolean value);
    }
}
