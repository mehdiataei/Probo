package com.utoronto.ece1778.probo.News;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.utoronto.ece1778.probo.R;


import android.support.v4.app.FragmentTransaction;
import android.util.Log;

public class NewsActivity extends AppCompatActivity {


    private static final String TAG = "NewsActivity";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_news);

        init();
    }

    private void init() {
        Log.d(TAG, "init: inflating " + getString(R.string.news_fragment));

        NewsFragment fragment = new NewsFragment();
        FragmentTransaction transaction = NewsActivity.this.getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.container, fragment);
        transaction.addToBackStack(getString(R.string.news_fragment));
        transaction.commit();
    }


}