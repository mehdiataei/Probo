package com.utoronto.ece1778.probo.Utils;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.View;

public class WrapHeightViewPager extends ViewPager {
    public WrapHeightViewPager(Context context) {
        super(context);
    }

    public WrapHeightViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // https://stackoverflow.com/a/20784791

        int numChildren = getChildCount();

        setOffscreenPageLimit(numChildren);

        int height = 0;
        for (int i=0; i<numChildren; i++) {
            View child = getChildAt(i);
            child.measure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));

            int childMeasuredHeight = child.getMeasuredHeight();

            if (childMeasuredHeight > height) {
                height = childMeasuredHeight;
            }
        }

        if (height != 0) {
            heightMeasureSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY);
        }

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }
}
