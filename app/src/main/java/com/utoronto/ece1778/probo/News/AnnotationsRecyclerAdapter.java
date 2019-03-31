package com.utoronto.ece1778.probo.News;

import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.elyeproj.loaderviewlibrary.LoaderTextView;
import com.utoronto.ece1778.probo.R;
import com.utoronto.ece1778.probo.User.User;

import java.util.ArrayList;

public class AnnotationsRecyclerAdapter extends RecyclerView.Adapter<AnnotationsRecyclerAdapter.AnnotationViewHolder> {
    private ArrayList<Annotation> annotations;
    private User user;
    private AnnotationCardView.OnUserClickListener onUserClickListener;
    private AnnotationCardView.OnVoteListener onVoteListener;

    public AnnotationsRecyclerAdapter(ArrayList<Annotation> annotations, User user,
                                      AnnotationCardView.OnUserClickListener onUserClickListener,
                                      AnnotationCardView.OnVoteListener onVoteListener) {

        this.annotations = annotations;
        this.user = user;
        this.onUserClickListener = onUserClickListener;
        this.onVoteListener = onVoteListener;
    }

    @Override
    public AnnotationViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.annotation_recycler_row, parent, false);
        return new AnnotationViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(AnnotationViewHolder holder, int position) {
        Annotation annotation = this.annotations.get(position);
        AnnotationCardView annotationCardView = holder.getAnnotationCardView();
        LoaderTextView annotationTitle = holder.getAnnotationTitle();
        LoaderTextView annotationQoute = holder.getAnnotationQuote();


        annotationCardView.setOnUserClickListener(this.onUserClickListener);
        annotationCardView.setOnVoteListener(this.onVoteListener);

        annotationCardView.setData(annotation, this.user);

        Log.d("RecyclerAdapter", "onBindViewHolder: heading:" + annotation.getHeading());
        annotationTitle.setText(annotation.getHeading());
        annotationQoute.setText(annotation.getSentence());
    }

    @Override
    public int getItemCount() {
        return this.annotations.size();
    }

    public static class AnnotationViewHolder extends RecyclerView.ViewHolder {
        private AnnotationCardView annotationCardView;
        private LoaderTextView annotationTitle;
        private LoaderTextView annotationQuote;

        public AnnotationViewHolder(View view) {
            super(view);
            annotationCardView = view.findViewById(R.id.annotation_card);
            annotationTitle = view.findViewById(R.id.annotation_title);
            annotationQuote = view.findViewById(R.id.annotation_qoute);
        }

        public AnnotationCardView getAnnotationCardView() {
            return this.annotationCardView;
        }

        public LoaderTextView getAnnotationTitle() {
            return annotationTitle;
        }

        public LoaderTextView getAnnotationQuote() {
            return annotationQuote;
        }
    }
}
