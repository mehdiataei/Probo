package com.utoronto.ece1778.probo.News;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.utoronto.ece1778.probo.Models.NewsItem;
import com.utoronto.ece1778.probo.R;
import com.utoronto.ece1778.probo.User.SignInActivity;
import com.utoronto.ece1778.probo.Utils.MyRecyclerViewAdapter;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;


public class ArticlesFragment extends Fragment {


    private static final String TAG = "ArticlesFragment";

    private MyRecyclerViewAdapter adapter;
    RecyclerView recyclerView;

    private static final int NUM_OF_COLUMNS = 1;

    private Context mContext;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;

    private FirebaseFirestore db;

    private ArrayList<NewsItem> news;

    private ArticlesFragmentInteractionListener interactionListener;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_articles, container, false);
        db = FirebaseFirestore.getInstance();

        mContext = getActivity();

        setupFirebaseAuth();
        setupGridView(this);
        recyclerView = view.findViewById(R.id.rvNumbers);

        return view;
    }

    /**
     * Firebase Auth setup
     */

    private void setupFirebaseAuth() {
        Log.d(TAG, "setupFirebaseAuth: setting up firebase auth.");

        mAuth = FirebaseAuth.getInstance();

        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                Log.d(TAG, "onAuthStateChanged: State changed.");

                checkCurrentUser(user);

                if (user != null) {
                    // User is signed in
                    Log.d(TAG, "onAuthStateChanged:signed_in:" + user.getUid());


                } else {

                    // User is signed out
                    Log.d(TAG, "onAuthStateChanged:signed_out");
                }
            }

        };

    }


    private void setupGridView(final ArticlesFragment fragment) {
        Log.d(TAG, "setupGridView: Setting up news grid.");

        news = new ArrayList<>();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db
                .collection(getString(R.string.dbname_news))
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {

                            if (task.getResult() != null) {

                                for (QueryDocumentSnapshot document : task.getResult()) {

                                    news.add(document.toObject(NewsItem.class));
                                }

                                // Sort news chronologically
                                news.sort(new Comparator<NewsItem>() {
                                    @Override
                                    public int compare(NewsItem o1, NewsItem o2) {

                                        try {

                                            Date o1_date = new SimpleDateFormat("yyyyMMdd_HHmmss").parse(o1.getDate_created());
                                            Date o2_date = new SimpleDateFormat("yyyyMMdd_HHmmss").parse(o2.getDate_created());

                                            int compare = o2_date.compareTo(o1_date);

                                            return compare;

                                        } catch (ParseException e) {
                                            e.printStackTrace();
                                            return 0;
                                        }

                                    }
                                });

                                // set up the RecyclerView
                                recyclerView.setLayoutManager(new GridLayoutManager(mContext, NUM_OF_COLUMNS, GridLayoutManager.VERTICAL, false));
                                adapter = new MyRecyclerViewAdapter(mContext, news);
                                recyclerView.setAdapter(adapter);
                                adapter.setClickListener(handleArticleClick);


                            }


                        }
                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.d(TAG, "onFailure: Failed to initialize the grid.");
            }
        });

    }

    private MyRecyclerViewAdapter.ItemClickListener handleArticleClick = new MyRecyclerViewAdapter.ItemClickListener() {
        @Override
        public void onItemClick(View view, int position) {
            interactionListener.onRouteToArticle(news.get(position).getNews_id());
        }
    };

    private void checkCurrentUser(FirebaseUser user) {
        Log.d(TAG, "checkCurrentUser: checking if user is logged in.");

        if (user == null) {
            Intent intent = new Intent(mContext, SignInActivity.class);
            startActivity(intent);
        }
    }


    @Override
    public void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(mAuthListener);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mAuthListener != null) {
            mAuth.removeAuthStateListener(mAuthListener);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        Fragment parentFragment = getParentFragment();

        if (parentFragment instanceof ArticlesFragmentInteractionListener) {
            interactionListener = (ArticlesFragmentInteractionListener) parentFragment;
        } else {
            throw new RuntimeException(parentFragment.toString()
                    + " must implement ArticlesFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        interactionListener = null;
    }

    public interface ArticlesFragmentInteractionListener {
        void onRouteToArticle(String articleId);
    }
}