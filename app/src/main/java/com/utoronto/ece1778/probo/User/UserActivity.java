package com.utoronto.ece1778.probo.User;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.utoronto.ece1778.probo.News.NewsFragment;
import com.utoronto.ece1778.probo.R;
import com.utoronto.ece1778.probo.Utils.ImageBitmap;
import com.utoronto.ece1778.probo.Utils.ImageLoader;

public class UserActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
                    NewsFragment.NewsFragmentInteractionListener,
                    ProfileFragment.ProfileFragmentInteractionListener {

    public static final int
            ROUTE_NEWS = 0,
            ROUTE_PROFILE = 1;

    private User user;

    private int currentRoute;
    private Fragment currentFragment;

    private boolean userNavigationEnabled;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // check if user is not signed in
        loadUser();

        currentRoute = -1;

        DrawerLayout drawer = findViewById(R.id.drawer_layout);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);

        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        routeToNews();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.nav_home:
                routeToNews();
                break;
            case R.id.nav_profile:
                routeToProfile(user.getUid());
                break;
            case R.id.nav_account:
                break;
            case R.id.nav_preferences:
                break;
        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void routeToNews() {
        if (currentRoute == UserActivity.ROUTE_NEWS) {
            if (currentFragment instanceof NewsFragment) {
                ((NewsFragment) currentFragment).goToViewPage(0);
            }

            return;
        }

        removeCurrentFragment();

        currentRoute = UserActivity.ROUTE_NEWS;
        currentFragment = new NewsFragment();

        displayFragment(currentFragment);
    }

    private void routeToProfile(String userId) {
        if (userNavigationEnabled && currentRoute == UserActivity.ROUTE_PROFILE) {
            return;
        }

        removeCurrentFragment();

        currentRoute = UserActivity.ROUTE_PROFILE;
        currentFragment = ProfileFragment.newInstance(userId);

        displayFragment(currentFragment);
    }

    private void displayFragment(Fragment fragment) {
        FragmentManager manager = getSupportFragmentManager();
        FragmentTransaction transaction = manager.beginTransaction();
        FrameLayout frameLayout = findViewById(R.id.content_container);
        ProgressBar contentProgress = findViewById(R.id.content_progress);

        contentProgress.setVisibility(View.VISIBLE);
        frameLayout.removeAllViews();

        transaction.add(R.id.content_container, fragment);
        transaction.addToBackStack(null);
        transaction.commit();

        manager.executePendingTransactions();

        contentProgress.setVisibility(View.GONE);
    }

    private void removeCurrentFragment() {
        if (currentFragment == null) {
            return;
        }

        FragmentManager manager = getSupportFragmentManager();
        FragmentTransaction transaction = manager.beginTransaction();

        transaction.remove(currentFragment);
        transaction.commit();

        manager.executePendingTransactions();

        currentRoute = -1;
        currentFragment = null;
    }

    private void loadUser() {
        User.UserCallback cb = new User.UserCallback() {
            @Override
            public void onLoad() {
                final ImageView profileImage = findViewById(R.id.nav_header_profile_image);
                TextView nameText = findViewById(R.id.nav_header_name);
                TextView titleText = findViewById(R.id.nav_header_title);

                if (user.getProfileImagePath() != null) {
                    final ProgressBar progressBar = findViewById(R.id.nav_header_profile_image_progress);

                    ImageLoader.ImageLoaderCallback imageCb = new ImageLoader.ImageLoaderCallback() {
                        @Override
                        public void onSuccess(Bitmap image) {
                            ImageBitmap imageBitmap = new ImageBitmap(image);
                            RoundedBitmapDrawable roundedImage = imageBitmap.getCroppedRoundedBitmapDrawable(getResources());

                            profileImage.setImageDrawable(roundedImage);
                        }

                        @Override
                        public void onFailure(Exception e) {
                        }

                        @Override
                        public void onComplete() {
                            profileImage.setVisibility(View.VISIBLE);
                            progressBar.setVisibility(View.GONE);
                        }
                    };

                    ImageLoader imageLoader = new ImageLoader(user.getProfileImagePath(), getApplicationContext());
                    imageLoader.load(imageCb);
                }

                nameText.setText(user.getName());

                if (user.getTitle() != null) {
                    titleText.setText(user.getTitle());
                    titleText.setVisibility(View.VISIBLE);
                }

                enableUserNavigation();
            }

            @Override
            public void onError(Exception error) {
            }
        };

        disableUserNavigation();

        user = new User();
        user.load(cb);
    }

    private void enableUserNavigation() {
        userNavigationEnabled = true;
    }

    private void disableUserNavigation() {
        userNavigationEnabled = false;
    }

    @Override
    public void onRouteToProfile(String userId) {
        routeToProfile(userId);
    }
}
