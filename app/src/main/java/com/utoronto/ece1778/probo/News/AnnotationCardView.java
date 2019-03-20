package com.utoronto.ece1778.probo.News;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.support.v4.graphics.ColorUtils;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v7.widget.CardView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.utoronto.ece1778.probo.R;
import com.utoronto.ece1778.probo.User.User;
import com.utoronto.ece1778.probo.Utils.Helper;
import com.utoronto.ece1778.probo.Utils.ImageBitmap;
import com.utoronto.ece1778.probo.Utils.ImageLoader;

import java.util.Locale;

public class AnnotationCardView extends CardView {
    private Context context;

    private View rootView;
    private CardView cardView;
    private ProgressBar progress;
    private TextView commentTextView;
    private ProgressBar profileImageProgress;
    private ImageView profileImageView;
    private Button nameButton;
    private TextView titleTextView;
    private ImageButton downvoteButton, upvoteButton;
    private ProgressBar downvoteProgress, upvoteProgress;
    private TextView downvoteTextView, upvoteTextView;

    private Annotation annotation;
    private User user;

    private OnVoteListener onVoteClick;
    private OnUserClickListener onUserClick;

    public AnnotationCardView(Context context) {
        super(context);
        this.init(context);
    }

    public AnnotationCardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.init(context);
    }

    private void init(Context context) {
        this.context = context;

        this.rootView = inflate(context, R.layout.annotation_card_view, this);
        this.progress = this.rootView.findViewById(R.id.progress);
        this.commentTextView = this.rootView.findViewById(R.id.comment);
        this.profileImageProgress = this.rootView.findViewById(R.id.profile_image_progress);
        this.profileImageView = this.rootView.findViewById(R.id.profile_image);
        this.nameButton = this.rootView.findViewById(R.id.name);
        this.titleTextView = this.rootView.findViewById(R.id.title);
        this.downvoteButton = this.rootView.findViewById(R.id.downvote);
        this.upvoteButton = this.rootView.findViewById(R.id.upvote);
        this.downvoteProgress = this.rootView.findViewById(R.id.downvote_progress);
        this.upvoteProgress = this.rootView.findViewById(R.id.upvote_progress);
        this.downvoteTextView = this.rootView.findViewById(R.id.downvote_count);
        this.upvoteTextView = this.rootView.findViewById(R.id.upvote_count);

        this.applyDefaultStyles();

        this.profileImageView.setOnClickListener(this.handleUserClick);
        this.nameButton.setOnClickListener(this.handleUserClick);

        this.downvoteButton.setOnClickListener(this.handleVoteClick);
        this.upvoteButton.setOnClickListener(this.handleVoteClick);
    }

    private void applyDefaultStyles() {
        CardView.LayoutParams params = new CardView.LayoutParams(
                CardView.LayoutParams.MATCH_PARENT,
                CardView.LayoutParams.MATCH_PARENT
        );

        this.setLayoutParams(params);
        this.setCardElevation(Helper.dpToPx(this.context, 10));
        this.setRadius(Helper.dpToPx(this.context, 15));
        this.setUseCompatPadding(true);
    }

    private View.OnClickListener handleUserClick = new View.OnClickListener() {
        public void onClick(View view) {
            if (onUserClick != null) {
                onUserClick.onClick(annotation.getUser());
            }
        }
    };

    private View.OnClickListener handleVoteClick = new View.OnClickListener() {
        public void onClick(View view) {
            handleVote((ImageButton) view, view.getId() == R.id.upvote);
        }
    };

    private void populate() {
        this.progress.setVisibility(View.GONE);
        this.commentTextView.setVisibility(View.VISIBLE);

        int color = this.annotation.getValue() == 1 ? Color.parseColor("#4dff88")
                                                    : Color.parseColor("#ffb3b3");
        this.setCardBackgroundColor(color);

        this.commentTextView.setText(annotation.getComment());

        updateVoteCounts();
        updateVoteDrawables();

        if (this.annotation.getUser().hasLoaded()) {
            populateUser();
            return;
        }

        User.UserCallback cb = new User.UserCallback() {
            @Override
            public void onLoad() {
                populateUser();
            }

            @Override
            public void onError(Exception error) {
            }
        };

        this.annotation.getUser().load(cb);
    }

    private void populateUser() {
        ImageLoader.ImageLoaderCallback cb = new ImageLoader.ImageLoaderCallback() {
            @Override
            public void onSuccess(Bitmap image) {
                ImageBitmap imageBitmap = new ImageBitmap(image);
                RoundedBitmapDrawable roundedImage = imageBitmap.getCroppedRoundedBitmapDrawable(getResources());

                profileImageView.setImageDrawable(roundedImage);
            }

            @Override
            public void onFailure(Exception e) {}

            @Override
            public void onComplete() {
                profileImageProgress.setVisibility(View.INVISIBLE);
                profileImageView.setVisibility(View.VISIBLE);
            }
        };

        if (annotation.getUser().getProfileImagePath() != null) {
            ImageLoader imageLoader = new ImageLoader(
                    annotation.getUser().getProfileImagePath(),
                    this.context.getApplicationContext()
            );

            imageLoader.load(cb);
        } else {
            this.profileImageProgress.setVisibility(View.INVISIBLE);
            this.profileImageView.setVisibility(View.VISIBLE);
        }

        this.nameButton.setText(annotation.getUser().getName());

        if (annotation.getUser().getTitle() != null) {
            this.titleTextView.setText(annotation.getUser().getTitle());
            this.titleTextView.setVisibility(View.VISIBLE);
        }
    }

    private void handleVote(final ImageButton imageButton, boolean value) {
        AnnotationVote.AnnotationVoteCallback cb = new AnnotationVote.AnnotationVoteCallback() {
            @Override
            public void onSubmit(boolean hasVote, boolean currentValue, int numUpvotes, int numDownvotes) {
                updateVoteDrawables();
                updateVoteCounts();
                hideVoteButtonLoading(imageButton);
                enableVoteButtons();

                if (onVoteClick != null) {
                    onVoteClick.onVote(annotation);
                }
            }

            @Override
            public void onError(Exception e) {
                Toast toast = Toast.makeText(
                        context.getApplicationContext(),
                        context.getApplicationContext().getString(R.string.annotation_fragment_error_general),
                        Toast.LENGTH_LONG
                );

                toast.show();
            }
        };

        this.disableVoteButtons();
        this.showVoteButtonLoading(imageButton);
        this.annotation.vote(cb, this.user, value);
    }

    private void updateVoteDrawables() {
        if (this.annotation.userHasDownvoted(this.user)) {
            this.upvoteButton.setImageResource(R.drawable.thumb_up_icon);
            this.downvoteButton.setImageResource(R.drawable.thumb_down_icon_light);
        } else if (this.annotation.userHasUpvoted(this.user)) {
            this.upvoteButton.setImageResource(R.drawable.thumb_up_icon_light);
            this.downvoteButton.setImageResource(R.drawable.thumb_down_icon);
        } else {
            this.upvoteButton.setImageResource(R.drawable.thumb_up_icon);
            this.downvoteButton.setImageResource(R.drawable.thumb_down_icon);
        }
    }

    private void updateVoteCounts() {
        this.downvoteTextView.setText(String.format(Locale.CANADA, "%d", this.annotation.getDownvoteCount()));
        this.upvoteTextView.setText(String.format(Locale.CANADA, "%d", this.annotation.getUpvoteCount()));
    }

    private void enableVoteButtons() {
        this.upvoteButton.setEnabled(true);
        this.downvoteButton.setEnabled(true);
    }

    private void disableVoteButtons() {
        this.upvoteButton.setEnabled(false);
        this.downvoteButton.setEnabled(false);
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

    public void setData(Annotation annotation, User user) {
        this.annotation = annotation;
        this.user = user;

        if (annotation.hasLoaded()) {
            populate();
            return;
        }

        Annotation.AnnotationCallback cb = new Annotation.AnnotationCallback() {
            @Override
            public void onLoad() {
                populate();
            }

            @Override
            public void onError(Exception e) {
            }
        };

        annotation.load(cb);
    }

    public void setOnVoteListener(OnVoteListener onVoteClick) {
        this.onVoteClick = onVoteClick;
    }

    public void setOnUserClickListener(OnUserClickListener onUserClick) {
        this.onUserClick = onUserClick;
    }

    public interface OnVoteListener {
        void onVote(Annotation annotation);
    }

    public interface OnUserClickListener {
        void onClick(User user);
    }
}
