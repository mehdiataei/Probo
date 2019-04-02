package com.utoronto.ece1778.probo.Utils;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bumptech.glide.Priority;
import com.bumptech.glide.request.RequestOptions;
import com.utoronto.ece1778.probo.Models.NewsItem;
import com.utoronto.ece1778.probo.R;

import java.util.List;

import static android.support.constraint.Constraints.TAG;

public class MyRecyclerViewAdapter extends RecyclerView.Adapter<MyRecyclerViewAdapter.ViewHolder> {

    private List<NewsItem> mData;
    private LayoutInflater mInflater;
    private ItemClickListener mClickListener;

    // data is passed into the constructor
    public MyRecyclerViewAdapter(Context context, List<NewsItem> data) {
        this.mInflater = LayoutInflater.from(context);
        this.mData = data;
    }

    // inflates the cell layout from xml when needed
    @Override
    @NonNull
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.news_item, parent, false);
        return new ViewHolder(view);
    }

    // binds the data to the TextView in each cell
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        //holder.myImageView.setImageBitmap(mData.get(position));
        Log.d(TAG, "onBindViewHolder: Called.");

        RequestOptions options = new RequestOptions()
                .centerCrop()
                .priority(Priority.HIGH);

        new GlideImageLoader(holder.myImageView,
                holder.myProgressBar).load(mData.get(position).getImage_url(), options);

        holder.myHeading.setText(mData.get(position).getHeading());
        holder.myDescription.setText(mData.get(position).getDescription());

        if (mData.get(position).getCredibility().equals("true")) {

            holder.myRel.setVisibility(View.GONE);
        }


    }

    // total number of cells
    @Override
    public int getItemCount() {
        return mData.size();
    }

    // stores and recycles views as they are scrolled off screen
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        ImageView myImageView;
        ProgressBar myProgressBar;
        TextView myDescription;
        TextView myHeading;
        RelativeLayout myRel;

        ViewHolder(View itemView) {
            super(itemView);
            myImageView = itemView.findViewById(R.id.images);
            myProgressBar = itemView.findViewById(R.id.gridImageProgressbar);

            myHeading = itemView.findViewById(R.id.header);
            myDescription = itemView.findViewById(R.id.description);
            myRel = itemView.findViewById(R.id.alart_rel_layout);

            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            if (mClickListener != null) mClickListener.onItemClick(view, getAdapterPosition());
        }
    }

    // convenience method for getting data at click position
    public NewsItem getItem(int id) {
        return mData.get(id);
    }

    // allows clicks events to be caught
    public void setClickListener(ItemClickListener itemClickListener) {
        this.mClickListener = itemClickListener;
    }

    // parent activity will implement this method to respond to click events
    public interface ItemClickListener {
        void onItemClick(View view, int position);
    }


}