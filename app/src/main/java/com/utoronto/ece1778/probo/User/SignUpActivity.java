package com.utoronto.ece1778.probo.User;

import android.content.Intent;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.utoronto.ece1778.probo.Utils.Helper;
import com.utoronto.ece1778.probo.R;
import com.utoronto.ece1778.probo.Utils.ImageBitmap;

public class SignUpActivity extends AppCompatActivity {
    static final int REQUEST_IMAGE_CAPTURE = 1;

    private ImageCapture imageCapture;

    private ImageView profileImage;
    private EditText emailInput;
    private EditText passwordInput;
    private EditText rePasswordInput;
    private EditText nameInput;
    private Button submitButton;
    private Button goBackButton;

    private RelativeLayout errorContainer;
    private TextView errorText;

    private RelativeLayout progressContainer;
    private TextView progressText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        imageCapture = new ImageCapture(getApplicationContext());

        profileImage = findViewById(R.id.profile_image);
        emailInput = findViewById(R.id.email);
        passwordInput = findViewById(R.id.password);
        rePasswordInput = findViewById(R.id.re_password);
        nameInput = findViewById(R.id.name);
        submitButton = findViewById(R.id.submit);
        goBackButton = findViewById(R.id.back);

        errorContainer = findViewById(R.id.error_container);
        errorText = findViewById(R.id.error_text);

        progressContainer = findViewById(R.id.progress_container);
        progressText = findViewById(R.id.progress_text);

        profileImage.setOnClickListener(handleProfileImageClick);
        submitButton.setOnClickListener(handleSubmitClick);
        goBackButton.setOnClickListener(handleBackClick);
    }

    private View.OnClickListener handleProfileImageClick = new View.OnClickListener() {
        public void onClick(View v) {
            captureImage();
        }
    };

    private View.OnClickListener handleSubmitClick = new View.OnClickListener() {
        public void onClick(View v) {
            signUp();
        }
    };

    private View.OnClickListener handleBackClick = new View.OnClickListener() {
        public void onClick(View v) {
            switchToSignIn();
        }
    };

    private void captureImage() {
        Intent intent = imageCapture.getCaptureIntent();
        startActivityForResult(intent, REQUEST_IMAGE_CAPTURE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            ImageBitmap imageBitmap = imageCapture.getCapturedImage();
            displayCapturedProfileImage(imageBitmap);
        }
    }

    public void displayCapturedProfileImage(ImageBitmap imageBitmap) {
        RoundedBitmapDrawable roundedBitmapDrawable = imageBitmap.getCroppedRoundedBitmapDrawable(getResources());
        profileImage.setImageDrawable(roundedBitmapDrawable);
    }

    private void signUp() {
        UserSignUpCallback cb = new UserSignUpCallback() {
            @Override
            public void onSignedUp(User user) {
                hideProgress();

                imageCapture.deleteImageFile();

                Toast toast = Toast.makeText(
                        getApplicationContext(),
                        "Account created",
                        Toast.LENGTH_LONG
                );

                toast.show();
            }

            @Override
            public void onSignUpError(int errorCode) {
                hideProgress();
                enable();

                switch (errorCode) {
                    case User.SIGN_UP_ERROR_EMPTY_EMAIL:
                        showError(getString(R.string.sign_up_error_empty_email));
                        break;
                    case User.SIGN_UP_ERROR_EMPTY_PASSWORD:
                        showError(getString(R.string.sign_up_error_empty_password));
                        break;
                    case User.SIGN_UP_ERROR_INCORRECT_RE_PASSWORD:
                        showError(getString(R.string.sign_up_error_incorrect_re_password));
                        break;
                    case User.SIGN_UP_ERROR_EMPTY_NAME:
                        showError(getString(R.string.sign_up_error_empty_name));
                        break;
                    case User.SIGN_UP_ERROR_WEAK_PASSWORD:
                        showError(getString(R.string.sign_up_error_weak_password));
                        break;
                    case User.SIGN_UP_ERROR_INVALID_EMAIL:
                        showError(getString(R.string.sign_up_error_invalid_email));
                        break;
                    case User.SIGN_UP_ERROR_USER_EXISTS:
                        showError(getString(R.string.sign_up_error_user_exists));
                        break;
                    default:
                        showError(getString(R.string.sign_up_error_general));
                        break;
                }
            }

            @Override
            public void onProgress(int progressCode) {
                hideError();

                switch (progressCode) {
                    case User.SIGN_UP_PROGRESS_CREATING:
                        showProgress(getString(R.string.sign_up_progress_creating));
                        break;
                    case User.SIGN_UP_PROGRESS_UPLOADING_PROFILE_IMAGE:
                        showProgress(getString(R.string.sign_up_progress_uploading_profile_image));
                        break;
                    case User.SIGN_UP_PROGRESS_SAVING:
                        showProgress(getString(R.string.sign_up_progress_saving));
                        break;
                }
            }

            @Override
            public void onError(Exception error) {
                hideProgress();
                enable();
                showError(getString(R.string.sign_up_error_general));
            }
        };

        disable();
        Helper.hideKeyboard(getApplicationContext(), getCurrentFocus());

        hideError();

        ImageBitmap profileImageBitmap = imageCapture.getCapturedImage();

        profileImageBitmap.centerCrop();
        profileImageBitmap.resizeSquare(1024);

        User.signUp(
                cb,
                profileImageBitmap.getBitmap(),
                emailInput.getText().toString(),
                passwordInput.getText().toString(),
                rePasswordInput.getText().toString(),
                nameInput.getText().toString()
        );
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

    private void disable() {
        profileImage.setEnabled(false);
        emailInput.setEnabled(false);
        passwordInput.setEnabled(false);
        rePasswordInput.setEnabled(false);
        nameInput.setEnabled(false);
        submitButton.setEnabled(false);
        goBackButton.setEnabled(false);
    }

    private void enable() {
        profileImage.setEnabled(true);
        emailInput.setEnabled(true);
        passwordInput.setEnabled(true);
        rePasswordInput.setEnabled(true);
        nameInput.setEnabled(true);
        submitButton.setEnabled(true);
        goBackButton.setEnabled(true);
    }

    public void switchToSignIn() {
        imageCapture.deleteImageFile();

        Intent intent = new Intent(this, SignInActivity.class);
        startActivity(intent);
    }
}
