package com.utoronto.ece1778.probo.News;

import android.content.Context;
import android.support.v7.widget.CardView;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;

import com.utoronto.ece1778.probo.R;
import com.utoronto.ece1778.probo.Utils.Helper;

public class AnnotationMoreCardView extends CardView {
    private Context context;

    private Button moreButton;

    private OnMoreClick moreClick;

    public AnnotationMoreCardView(Context context) {
        super(context);
        this.init(context);
    }

    public AnnotationMoreCardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.init(context);
    }

    private void init(Context context) {
        this.context = context;

        View rootView = inflate(context, R.layout.annotation_more_card_view, this);
        this.moreButton = rootView.findViewById(R.id.more);

        this.applyDefaultStyles();

        this.moreButton.setOnClickListener(this.handleClick);
    }

    private void applyDefaultStyles() {
        CardView.LayoutParams params = new CardView.LayoutParams(
                CardView.LayoutParams.MATCH_PARENT,
                CardView.LayoutParams.MATCH_PARENT
        );

        this.setLayoutParams(params);
        this.setCardElevation(Helper.dpToPx(this.context, 1));
        this.setRadius(Helper.dpToPx(this.context, 16));
    }

    private View.OnClickListener handleClick = new OnClickListener() {
        @Override
        public void onClick(View v) {
            if (moreClick != null) {
                moreClick.onClick();
            }
        }
    };

    public void setNumMore(int numMore) {
        this.moreButton.setText(this.context.getString(R.string.annotation_more_fragment_button, numMore));
    }

    public void setOnMoreClickListener(OnMoreClick moreClick) {
        this.moreClick = moreClick;
    }

    public interface OnMoreClick {
        void onClick();
    }
}
