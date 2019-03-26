package com.utoronto.ece1778.probo.User;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.util.Log;
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

import com.google.firebase.auth.FirebaseAuth;
import com.utoronto.ece1778.probo.News.Annotation;
import com.utoronto.ece1778.probo.News.NewsFragment;
import com.utoronto.ece1778.probo.R;
import com.utoronto.ece1778.probo.Utils.Helper;
import com.utoronto.ece1778.probo.Utils.ImageBitmap;
import com.utoronto.ece1778.probo.Utils.ImageLoader;

import java.lang.reflect.Method;

public class UserActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
                    NewsFragment.NewsFragmentInteractionListener,
                    ProfileFragment.ProfileFragmentInteractionListener,
                    NotificationsFragment.NotificationsFragmentInteractionListener,
                    AccountFragment.AccountFragmentInteractionListener {

    public static final int
            ROUTE_NEWS = 0,
            ROUTE_PROFILE = 1,
            ROUTE_NOTIFICATIONS = 2,
            ROUTE_ACCOUNT = 3;

    private User user;

    private int currentRoute;
    private Fragment currentFragment;
    private String currentProfileUserId;

    private boolean userNavigationEnabled;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        loadUser();

        currentRoute = -1;

        DrawerLayout drawer = findViewById(R.id.drawer_layout);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close) {

            public void onDrawerOpened(View drawerView) {
                Helper.hideKeyboard(getApplicationContext(), drawerView);
                super.onDrawerOpened(drawerView);
            }
        };

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
            return;
        } else if (currentFragment instanceof NewsFragment &&
                    ((NewsFragment) currentFragment).onBackPressed()) {

            return;
        } else if (!(currentFragment instanceof NewsFragment)) {
            routeToNews();
            return;
        }

        super.onBackPressed();
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
            case R.id.nav_notifications:
                routeToNotifications();
                break;
            case R.id.nav_account:
                routeToAccount();
                break;
            case R.id.nav_preferences:
                break;
            case R.id.nav_sign_out:
                signOut();
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
        if (userNavigationEnabled && currentRoute == UserActivity.ROUTE_PROFILE &&
            userId.equals(currentProfileUserId)) {

            return;
        }

        removeCurrentFragment();

        currentRoute = UserActivity.ROUTE_PROFILE;
        currentFragment = ProfileFragment.newInstance(userId);
        currentProfileUserId = userId;

        displayFragment(currentFragment);
    }

    private void routeToNotifications() {
        if (currentRoute == UserActivity.ROUTE_NOTIFICATIONS) {
            return;
        }

        removeCurrentFragment();

        currentRoute = UserActivity.ROUTE_NOTIFICATIONS;
        currentFragment = new NotificationsFragment();

        displayFragment(currentFragment);
    }

    private void routeToAccount() {
        if (userNavigationEnabled && currentRoute == UserActivity.ROUTE_ACCOUNT) {
            return;
        }

        removeCurrentFragment();

        currentRoute = UserActivity.ROUTE_ACCOUNT;
        currentFragment = AccountFragment.newInstance(user.getUid());

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
        if (!User.isSignedIn()) {
            routeToSignIn();
            return;
        }

        User.UserCallback cb = new User.UserCallback() {
            @Override
            public void onLoad() {
                user.subscribeToAnnotations();

                populateUser();
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

    private void populateUser() {
        final ProgressBar progressBar = findViewById(R.id.nav_header_profile_image_progress);
        final ImageView profileImage = findViewById(R.id.nav_header_profile_image);
        TextView nameText = findViewById(R.id.nav_header_name);
        TextView titleText = findViewById(R.id.nav_header_title);

        if (user.getProfileImagePath() != null) {
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
        } else {
            profileImage.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.GONE);
        }

        nameText.setText(user.getName());

        if (user.getTitle() != null) {
            titleText.setText(user.getTitle());
            titleText.setVisibility(View.VISIBLE);
        }
    }

    private void signOut() {
        FirebaseAuth.getInstance().signOut();
        routeToSignIn();
    }

    private void enableUserNavigation() {
        userNavigationEnabled = true;
    }

    private void disableUserNavigation() {
        userNavigationEnabled = false;
    }

    @Override
    public void onAccountUpdated(User user) {
        this.user = user;
        populateUser();
    }

    @Override
    public void onAnnotationVote(Annotation annotation) {
    }

    @Override
    public void onRouteToProfile(String userId) {
        routeToProfile(userId);
    }

    private void routeToSignIn() {
        Intent intent = new Intent(this, SignInActivity.class);
        startActivity(intent);
        finish();
    }
}
