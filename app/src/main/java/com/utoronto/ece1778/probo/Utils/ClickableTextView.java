package com.utoronto.ece1778.probo.Utils;

import java.util.List;

import android.content.Context;
import android.graphics.Color;
import android.text.SpannableStringBuilder;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;


import com.utoronto.ece1778.probo.News.AnnotationInputFragment;
import com.utoronto.ece1778.probo.Utils.ExtractSentences;

import androidx.annotation.NonNull;

import static android.support.constraint.Constraints.TAG;

/**
 * Defines a TextView widget where user can click on different words to see different actions
 */
public class ClickableTextView extends android.support.v7.widget.AppCompatTextView {
    private ClickableTextViewInterface clickableTextViewInterface;

    public ClickableTextView(Context context) {
        super(context);
        attachInterface(context);
    }

    public ClickableTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        attachInterface(context);
    }

    public ClickableTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        attachInterface(context);
    }

    public void attachInterface(Context context) {
        if (context instanceof ClickableTextViewInterface) {
            this.clickableTextViewInterface = (ClickableTextViewInterface) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement AnnotationSubmitCallback");
        }
    }

    public void setTextWithClickableSentences(String text) {
        ExtractSentences extractSentences = new ExtractSentences(text);
        List<String> sentences = extractSentences.getSentences();

        setMovementMethod(LinkMovementMethod.getInstance());
        setText(addClickableSentence(text, sentences), BufferType.SPANNABLE);
    }

    private SpannableStringBuilder addClickableSentence(String str, List<String> clickableSentences) {
        SpannableStringBuilder ssb = new SpannableStringBuilder(str);

        for (final String clickableSentence : clickableSentences) {
            int idx = str.indexOf(clickableSentence);

            while (idx != -1) {
                final int idx1 = idx;
                final int idx2 = idx1 + clickableSentence.length();

                ssb.setSpan(
                        new NoUnderlineClickableSpan() {
                            @Override
                            public void onClick(@NonNull View widget) {
                                if (clickableTextViewInterface != null) {
                                    clickableTextViewInterface.onTextViewClick(clickableSentence, idx1, idx2);
                                }

                                Log.d(TAG, "onClick: " + clickableSentence + ": " + idx1 + " -> " + idx2);
                            }
                        },
                        idx1,
                        idx2,
                        0
                );

                idx = str.indexOf(clickableSentence, idx2);
            }
        }

        return ssb;
    }


    //a version of ClickableSpan without the underline
    public static abstract class NoUnderlineClickableSpan extends ClickableSpan {
        private int color = -1;

        public void setColor(int color) {
            this.color = Color.BLACK;
        }

        @Override
        public void updateDrawState(TextPaint ds) {
            ds.setUnderlineText(false);
            if (this.color != -1) {
                ds.setColor(this.color);
            }
        }
    }

    public interface ClickableTextViewInterface {
        void onTextViewClick(String quote, int startIndex, int endIndex);
    }
}