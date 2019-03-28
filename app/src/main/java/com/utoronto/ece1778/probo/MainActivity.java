package com.utoronto.ece1778.probo;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.utoronto.ece1778.probo.User.SignInActivity;
import com.utoronto.ece1778.probo.User.User;
import com.utoronto.ece1778.probo.User.UserActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        Bundle extras = intent.getExtras();

        if (extras != null &&
                extras.getString("articleId") != null &&
                extras.getString("annotationId") != null &&
                extras.getString("annotationType") != null &&
                extras.getString("annotationStartIndex") != null &&
                extras.getString("annotationEndIndex") != null) {

            route(
                    intent.getExtras().getString("articleId"),
                    extras.getString("annotationId"),
                    extras.getString("annotationType"),
                    Integer.parseInt(extras.getString("annotationStartIndex")),
                    Integer.parseInt(extras.getString("annotationEndIndex"))
            );
        } else {
            route();
        }
    }

    private void route() {
        Intent intent;

        if (User.isSignedIn()) {
            intent = new Intent(this, UserActivity.class);
        } else {
            intent = new Intent(this, SignInActivity.class);
        }

        startActivity(intent);
    }

    private void route(String articleId, String annotationId, String annotationType, int annotationStartIndex, int annotationEndIndex) {
        Intent intent;

        if (User.isSignedIn()) {
            intent = new Intent(this, UserActivity.class);

            intent.putExtra("articleId", articleId);
            intent.putExtra("annotationId", annotationId);
            intent.putExtra("annotationType", annotationType);
            intent.putExtra("annotationStartIndex", annotationStartIndex);
            intent.putExtra("annotationEndIndex", annotationEndIndex);
        } else {
            intent = new Intent(this, SignInActivity.class);
        }

        startActivity(intent);
    }
}
