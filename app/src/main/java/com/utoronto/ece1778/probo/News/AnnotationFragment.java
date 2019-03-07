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
import android.widget.ImageView;
import android.widget.TextView;

import com.utoronto.ece1778.probo.Login.User;
import com.utoronto.ece1778.probo.R;
import com.utoronto.ece1778.probo.Utils.ImageBitmap;
import com.utoronto.ece1778.probo.Utils.ImageLoader;

import org.w3c.dom.Text;

public class AnnotationFragment extends Fragment {
    private static final String ARG_USER_ID = "userId";
    private static final String ARG_COMMENT = "comment";
    private static final String ARG_VALUE = "value";

    private User user;

    private String userId;
    private String comment;
    private int value;

    private CardView card;
    private ImageView profileImage;
    private TextView nameText;
    private TextView titleText;
    private TextView commentText;

    private AnnotationFragmentInteractionListener interactionListener;

    public AnnotationFragment() {}

    public static AnnotationFragment newInstance(String userId, String comment, int value) {
        AnnotationFragment fragment = new AnnotationFragment();
        Bundle args = new Bundle();

        args.putString(ARG_USER_ID, userId);
        args.putString(ARG_COMMENT, comment);
        args.putInt(ARG_VALUE, value);

        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            userId = getArguments().getString(ARG_USER_ID);
            comment = getArguments().getString(ARG_COMMENT);
            value = getArguments().getInt(ARG_VALUE);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_annotation, container, false);

        card = v.findViewById(R.id.card);
        profileImage = v.findViewById(R.id.profile_image);
        nameText = v.findViewById(R.id.name);
        titleText = v.findViewById(R.id.title);
        commentText = v.findViewById(R.id.comment);

        User.UserCallback cb = new User.UserCallback() {
            @Override
            public void onLoad() {
                populate();
            }

            @Override
            public void onError(Exception error) {
            }
        };

        user = new User(userId);
        user.load(cb);

        return v;
    }

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
            public void onComplete() {}
        };

        if (user.getProfileImagePath() != null) {
            ImageLoader imageLoader = new ImageLoader(
                    user.getProfileImagePath(),
                    getActivity().getApplicationContext()
            );

            imageLoader.load(cb);
        }

        nameText.setText(user.getName());

        if (user.getTitle() != null) {
            titleText.setText(user.getTitle());
            titleText.setVisibility(View.VISIBLE);
        }

        commentText.setText(comment);

        setBackgroundColor();
    }

    private void setBackgroundColor() {
        int color = value == 1 ? Color.GREEN : Color.RED;
        int formattedColor = ColorUtils.setAlphaComponent(color, 125);
        card.setCardBackgroundColor(formattedColor);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        /*if (context instanceof AnnotationFragmentInteractionListener) {
            interactionListener = (AnnotationFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement AnnotationFragmentInteractionListener");
        }*/
    }

    @Override
    public void onDetach() {
        super.onDetach();
        interactionListener = null;
    }

    public interface AnnotationFragmentInteractionListener {
    }
}
