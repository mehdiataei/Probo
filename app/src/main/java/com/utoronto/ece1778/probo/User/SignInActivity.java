package com.utoronto.ece1778.probo.User;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.utoronto.ece1778.probo.Utils.Helper;
import com.utoronto.ece1778.probo.R;

public class SignInActivity extends AppCompatActivity {
    private EditText emailInput;
    private EditText passwordInput;
    private Button submitButton;
    private Button signUpButton;

    private RelativeLayout errorContainer;
    private TextView errorText;

    private RelativeLayout progressContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);

        emailInput = findViewById(R.id.email);
        passwordInput = findViewById(R.id.password);
        submitButton = findViewById(R.id.submit);
        signUpButton = findViewById(R.id.sign_up);

        errorContainer = findViewById(R.id.error_container);
        errorText = findViewById(R.id.error_text);

        progressContainer = findViewById(R.id.progress_container);

        submitButton.setOnClickListener(handleSubmitClick);
        signUpButton.setOnClickListener(handleSignUpClick);
    }

    private View.OnClickListener handleSubmitClick = new View.OnClickListener() {
        public void onClick(View v) {
            signIn();
        }
    };

    private View.OnClickListener handleSignUpClick = new View.OnClickListener() {
        public void onClick(View v) {
            switchToSignUp();
        }
    };

    private void signIn() {
        User.UserSignInCallback cb = new User.UserSignInCallback() {
            @Override
            public void onSignedIn(User user) {
                hideProgress();

                Toast toast = Toast.makeText(
                        getApplicationContext(),
                        "Signed in.",
                        Toast.LENGTH_LONG
                );

                toast.show();
                Intent intent = new Intent(SignInActivity.this, UserActivity.class);
                startActivity(intent);
            }

            @Override
            public void onSignInError(int errorCode) {
                hideProgress();
                enable();

                switch (errorCode) {
                    case User.SIGN_IN_ERROR_EMPTY_EMAIL:
                        showError(getString(R.string.sign_in_error_empty_email));
                        break;
                    case User.SIGN_IN_ERROR_EMPTY_PASSWORD:
                        showError(getString(R.string.sign_in_error_empty_password));
                        break;
                    case User.SIGN_IN_ERROR_INVALID_CREDENTIALS:
                        showError(getString(R.string.sign_in_error_invalid_credentials));
                        break;
                    case User.SIGN_IN_ERROR_NO_USER:
                        showError(getString(R.string.sign_in_error_no_user));
                        break;
                    default:
                        showError(getString(R.string.sign_in_error_general));
                        break;
                }
            }

            @Override
            public void onError(Exception error) {
                hideProgress();
                enable();
                showError(getString(R.string.sign_in_error_general));
            }
        };

        disable();
        Helper.hideKeyboard(getApplicationContext(), getCurrentFocus());

        hideError();
        showProgress();

        User.signIn(
                cb,
                emailInput.getText().toString(),
                passwordInput.getText().toString()
        );
    }

    private void showError(String errorMessage) {
        errorText.setText(errorMessage);
        errorContainer.setVisibility(View.VISIBLE);
    }

    private void hideError() {
        errorContainer.setVisibility(View.GONE);
    }

    private void showProgress() {
        progressContainer.setVisibility(View.VISIBLE);
    }

    private void hideProgress() {
        progressContainer.setVisibility(View.GONE);
    }

    private void disable() {
        emailInput.setEnabled(false);
        passwordInput.setEnabled(false);
        submitButton.setEnabled(false);
        signUpButton.setEnabled(false);
    }

    private void enable() {
        emailInput.setEnabled(true);
        passwordInput.setEnabled(true);
        submitButton.setEnabled(true);
        signUpButton.setEnabled(true);
    }

    public void switchToSignUp() {
        Intent intent = new Intent(this, SignUpActivity.class);
        startActivity(intent);
    }
}
