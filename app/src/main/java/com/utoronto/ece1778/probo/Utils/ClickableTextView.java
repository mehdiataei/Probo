package com.utoronto.ece1778.probo.Utils;

import android.content.Context;
import android.graphics.Color;
import android.os.AsyncTask;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.View;

import java.lang.ref.WeakReference;
import java.util.List;

import androidx.annotation.NonNull;

/**
 * Defines a TextView widget where user can click on different words to see different actions
 */
public class ClickableTextView extends android.support.v7.widget.AppCompatTextView {
    private ClickableTextViewInterface clickableTextViewInterface;

    public ClickableTextView(Context context) {
        super(context);
    }

    public ClickableTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ClickableTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setTextWithClickableSentences(SpannableString annotations, ClickableTextViewInterface clickableTextViewInterface,
                                              int offsetIndex) {

        String text = annotations.toString();

        ExtractSentences extractSentences = new ExtractSentences(text);
        List<String> sentences = extractSentences.getSentences();

        setMovementMethod(LongClickLinkMovementMethod.getInstance());

        SetTextParams params = new SetTextParams(
                text,
                sentences,
                annotations,
                clickableTextViewInterface,
                offsetIndex
        );

        new SetText(this).execute(params);
    }

    private static SpannableStringBuilder addClickableSentence(String str, List<String> clickableSentences, SpannableString annotations,
                                                               final ClickableTextViewInterface clickableInterface, final int offset) {

        SpannableStringBuilder ssb = new SpannableStringBuilder(annotations);

        for (final String clickableSentence : clickableSentences) {
            int idx = str.indexOf(clickableSentence);

            while (idx != -1) {
                final int idx1 = idx;
                final int idx2 = idx1 + clickableSentence.length();

                ssb.setSpan(
                        new NoUnderlineClickableSpan() {
                            @Override
                            public void onClick(@NonNull View widget) {
                                clickableInterface.onTextViewClick(
                                        clickableSentence,
                                        offset + idx1,
                                        offset + idx2
                                );
                            }

                            @Override
                            public void onLongClick(View widget) {
                                clickableInterface.onTextViewLongClick(
                                        clickableSentence,
                                        offset + idx1,
                                        offset + idx2
                                );
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

    public static class SetText extends AsyncTask<SetTextParams, Void, SpannableStringBuilder> {
        private WeakReference<ClickableTextView> clickableTextViewWeakReference;

        SetText(ClickableTextView textViewReference) {
            this.clickableTextViewWeakReference = new WeakReference<>(textViewReference);
        }

        protected SpannableStringBuilder doInBackground(SetTextParams... params) {
            return ClickableTextView.addClickableSentence(
                    params[0].getOriginalString(),
                    params[0].getClickableSentences(),
                    params[0].getAnnotations(),
                    params[0].getClickableInterface(),
                    params[0].getOffset()
            );
        }

        protected void onPostExecute(SpannableStringBuilder results) {
            ClickableTextView clickableTextView = this.clickableTextViewWeakReference.get();
            clickableTextView.setText(results);
        }
    }

    public static class SetTextParams {
        private String originalString;
        private List<String> clickableSentences;
        private SpannableString annotations;
        private ClickableTextViewInterface clickableInterface;
        private int offset;

        SetTextParams(String originalString, List<String> clickableSentences, SpannableString annotations,
                      ClickableTextViewInterface clickableInterface, int offset) {

            this.originalString = originalString;
            this.clickableSentences = clickableSentences;
            this.annotations = annotations;
            this.clickableInterface = clickableInterface;
            this.offset = offset;
        }

        public String getOriginalString() {
            return this.originalString;
        }

        public List<String> getClickableSentences() {
            return this.clickableSentences;
        }

        public SpannableString getAnnotations() {
            return this.annotations;
        }

        public ClickableTextViewInterface getClickableInterface() {
            return this.clickableInterface;
        }

        public int getOffset() {
            return this.offset;
        }
    }

    //a version of ClickableSpan without the underline
    public static abstract class NoUnderlineClickableSpan extends LongClickableSpan {
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
        void onTextViewLongClick(String quote, int startIndex, int endIndex);
    }
}