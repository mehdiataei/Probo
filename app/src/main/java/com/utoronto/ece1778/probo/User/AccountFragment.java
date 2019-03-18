package com.utoronto.ece1778.probo.User;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.utoronto.ece1778.probo.R;
import com.utoronto.ece1778.probo.Utils.Helper;
import com.utoronto.ece1778.probo.Utils.ImageBitmap;
import com.utoronto.ece1778.probo.Utils.ImageLoader;

public class AccountFragment extends Fragment {
    private static final String ARG_USER_ID = "userId";
    static final int REQUEST_IMAGE_CAPTURE = 1;

    private ProgressBar profileImageProgress;
    private ImageView profileImageView;
    private EditText nameText;
    private EditText titleText;
    private RelativeLayout errorContainer, progressContainer;
    private TextView errorText, progressText;
    private Button submitButton;

    private User user;
    private ImageCapture imageCapture;
    private boolean profileImageChanged;

    private AccountFragmentInteractionListener interactionListener;

    public AccountFragment() {
    }

    public static AccountFragment newInstance(String userId) {
        AccountFragment fragment = new AccountFragment();
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

        View v = inflater.inflate(R.layout.fragment_account, container, false);

        imageCapture = new ImageCapture(getContext());
        profileImageChanged = false;

        profileImageProgress = v.findViewById(R.id.profile_image_progress);
        profileImageView = v.findViewById(R.id.profile_image);
        nameText = v.findViewById(R.id.name);
        titleText = v.findViewById(R.id.title);
        errorContainer = v.findViewById(R.id.error_container);
        progressContainer = v.findViewById(R.id.progress_container);
        errorText = v.findViewById(R.id.error_text);
        progressText = v.findViewById(R.id.progress_text);
        submitButton = v.findViewById(R.id.submit);

        profileImageView.setOnClickListener(handleProfileImageClick);
        submitButton.setOnClickListener(handleSubmitClick);

        load();

        return v;
    }

    private View.OnClickListener handleProfileImageClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            captureImage();
        }
    };

    private View.OnClickListener handleSubmitClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            submit();
        }
    };

    private void load() {
        User.UserCallback cb = new User.UserCallback() {
            @Override
            public void onLoad() {
                populate();
                hideProgress();
                enable();
            }

            @Override
            public void onError(Exception error) {
            }
        };

        disable();
        showProgress(getString(R.string.account_loading));
        user.load(cb);
    }

    private void populate() {
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
                profileImageView.setVisibility(View.VISIBLE);
                profileImageProgress.setVisibility(View.INVISIBLE);
            }
        };

        if (user.getProfileImagePath() != null) {
            ImageLoader imageLoader = new ImageLoader(
                    user.getProfileImagePath(),
                    getActivity().getApplicationContext()
            );

            imageLoader.load(cb);
        } else {
            profileImageView.setVisibility(View.VISIBLE);
            profileImageProgress.setVisibility(View.INVISIBLE);
        }

        nameText.setText(user.getName());

        if (user.getTitle() != null) {
            titleText.setText(user.getTitle());
        }
    }

    private void submit() {
        User.UserUpdateCallback cb = new User.UserUpdateCallback() {
            @Override
            public void onUpdate(User updatedUser) {
                hideProgress();

                imageCapture.deleteImageFile();

                if (interactionListener != null) {
                    interactionListener.onAccountUpdated(updatedUser);
                }

                Toast toast = Toast.makeText(
                        getActivity().getApplicationContext(),
                        "Account updated.",
                        Toast.LENGTH_LONG
                );

                toast.show();

                enable();
            }

            @Override
            public void onUpdateError(int errorCode) {
                hideProgress();
                enable();

                switch (errorCode) {
                    case User.UPDATE_ERROR_EMPTY_NAME:
                        showError(getString(R.string.account_error_empty_name));
                        break;
                    default:
                        showError(getString(R.string.account_error_general));
                        break;
                }
            }

            @Override
            public void onProgress(int progressCode) {
                hideError();

                switch (progressCode) {
                    case User.UPDATE_PROGRESS_UPLOADING_PROFILE_IMAGE:
                        showProgress(getString(R.string.account_progress_uploading_profile_image));
                        break;
                    case User.UPDATE_PROGRESS_SAVING:
                        showProgress(getString(R.string.account_progress_saving));
                        break;
                }
            }

            @Override
            public void onError(Exception error) {
                hideProgress();
                enable();
                showError(getString(R.string.account_error_general));
            }
        };

        disable();
        Helper.hideKeyboard(getActivity().getApplicationContext(), getActivity().getCurrentFocus());

        hideError();

        ImageBitmap profileImageBitmap = imageCapture.getCapturedImage();

        profileImageBitmap.centerCrop();
        profileImageBitmap.resizeSquare(1024);

        user.update(
                cb,
                profileImageChanged ? profileImageBitmap.getBitmap() : null,
                nameText.getText().toString(),
                titleText.getText().toString(),
                profileImageChanged
        );
    }

    private void captureImage() {
        Intent intent = imageCapture.getCaptureIntent();
        startActivityForResult(intent, REQUEST_IMAGE_CAPTURE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {
            ImageBitmap imageBitmap = imageCapture.getCapturedImage();
            displayCapturedProfileImage(imageBitmap);
        }
    }

    public void displayCapturedProfileImage(ImageBitmap imageBitmap) {
        profileImageChanged = true;

        RoundedBitmapDrawable roundedBitmapDrawable = imageBitmap.getCroppedRoundedBitmapDrawable(getResources());
        profileImageView.setImageDrawable(roundedBitmapDrawable);
    }

    private void showError(String errorMessage) {
        errorText.setText(errorMessage);
        errorContainer.setVisibility(View.VISIBLE);
    }

    private void hideError() {
        errorContainer.setVisibility(View.GONE);
    }

    private void showProgress(String progressMessage) {
        progressText.setText(progressMessage);
        progressContainer.setVisibility(View.VISIBLE);
    }

    private void hideProgress() {
        progressContainer.setVisibility(View.GONE);
    }

    private void enable() {
        profileImageView.setEnabled(true);
        nameText.setEnabled(true);
        titleText.setEnabled(true);
        submitButton.setEnabled(true);
    }

    private void disable() {
        profileImageView.setEnabled(false);
        nameText.setEnabled(false);
        titleText.setEnabled(false);
        submitButton.setEnabled(false);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof AccountFragmentInteractionListener) {
            interactionListener = (AccountFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement AccountFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        interactionListener = null;
    }

    public interface AccountFragmentInteractionListener {
        void onAccountUpdated(User user);
    }
}
