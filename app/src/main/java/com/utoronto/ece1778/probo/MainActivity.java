package com.utoronto.ece1778.probo;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.utoronto.ece1778.probo.User.SignInActivity;
import com.utoronto.ece1778.probo.User.User;
import com.utoronto.ece1778.probo.News.NewsActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent intent;

        if (User.isSignedIn()) {
            intent = new Intent(this, NewsActivity.class);
        } else {
            intent = new Intent(this, SignInActivity.class);
        }

        startActivity(intent);
    }
}
