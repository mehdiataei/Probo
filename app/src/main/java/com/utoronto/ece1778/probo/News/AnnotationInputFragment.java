package com.utoronto.ece1778.probo.News;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.utoronto.ece1778.probo.R;
import com.utoronto.ece1778.probo.Utils.Helper;
import com.xw.repo.BubbleSeekBar;

import static android.support.constraint.Constraints.TAG;

public class AnnotationInputFragment extends Fragment {
    private static final String
            ARG_IS_FACT = "isFact",
            ARG_QUOTE = "quote",
            ARG_TYPE = "type",
            ARG_START_INDEX = "startIndex",
            ARG_END_INDEX = "endIndex",
            ARG_VALUE = "value";

    private boolean isFact;

    private String
            annotationQuote,
            annotationType;

    private int
            annotationStartIndex,
            annotationEndIndex,
            annotationValue;

    private AnnotationInputFragmentInteractionListener interactionListener;
    private ImageButton closeButton;
    private BubbleSeekBar valueSeekBar;
    private TextView title;
    private EditText commentText;
    private EditText sourceText;
    private CheckBox primarySource;
    private CheckBox subscribe;
    private RelativeLayout errorContainer;
    private TextView errorText;
    private Button submitButton;
    private RelativeLayout progressContainer;
    private TextView progressText;

    public AnnotationInputFragment() {
    }

    public static AnnotationInputFragment newInstance(boolean isFact, String quote, String type,
                                                      int startIndex, int endIndex, int value) {

        AnnotationInputFragment fragment = new AnnotationInputFragment();
        Bundle args = new Bundle();

        args.putBoolean(ARG_IS_FACT, isFact);
        args.putString(ARG_QUOTE, quote);
        args.putString(ARG_TYPE, type);
        args.putInt(ARG_START_INDEX, startIndex);
        args.putInt(ARG_END_INDEX, endIndex);
        args.putInt(ARG_VALUE, value);

        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            isFact = getArguments().getBoolean(ARG_IS_FACT);
            annotationQuote = getArguments().getString(ARG_QUOTE);
            annotationType = getArguments().getString(ARG_TYPE);
            annotationStartIndex = getArguments().getInt(ARG_START_INDEX);
            annotationEndIndex = getArguments().getInt(ARG_END_INDEX);
            annotationValue = getArguments().getInt(ARG_VALUE);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_annotation_input, container, false);

        TextView fragmentTitle = v.findViewById(R.id.fragment_title);
        closeButton = v.findViewById(R.id.close);
        valueSeekBar = v.findViewById(R.id.value_seekbar);
        TextView quote = v.findViewById(R.id.quote);
        title = v.findViewById(R.id.title);
        commentText = v.findViewById(R.id.input);
        sourceText = v.findViewById(R.id.source);
        primarySource = v.findViewById(R.id.primary_source);
        subscribe = v.findViewById(R.id.subscribe);
        errorContainer = v.findViewById(R.id.error_container);
        errorText = v.findViewById(R.id.error_text);
        submitButton = v.findViewById(R.id.submit);
        progressText = v.findViewById(R.id.progress_textview);
        progressContainer = v.findViewById(R.id.progress_container);

        if (isFact) {
            fragmentTitle.setText(getString(R.string.annotation_input_title_fact));
        } else {
            fragmentTitle.setText(getString(R.string.annotation_input_title_opinion));
        }

        quote.setText(annotationQuote);

        valueSeekBar.setOnProgressChangedListener(handleSeekBarChange);
        closeButton.setOnClickListener(handleCloseClick);
        submitButton.setOnClickListener(handleSubmitClick);

        valueSeekBar.setCustomSectionTextArray(new BubbleSeekBar.CustomSectionTextArray() {
            @NonNull
            @Override
            public SparseArray<String> onCustomize(int sectionCount, @NonNull SparseArray<String> array) {
                array.clear();

                String negativeText = isFact ?
                        getString(R.string.annotation_input_false) :
                        getString(R.string.annotation_input_biased);

                String positiveText = isFact ?
                        getString(R.string.annotation_input_true) :
                        getString(R.string.annotation_input_unbiased);

                array.put(0, negativeText);
                array.put(5, positiveText);

                return array;
            }
        });

        return v;
    }

    @Override
    public void onResume() {
        super.onResume();

        valueSeekBar.setProgress(0);
        commentText.setText("");
        sourceText.setText("");
        primarySource.setChecked(false);
        subscribe.setChecked(false);

        annotationValue = 0;
        setTitleText(annotationValue);
    }

    private BubbleSeekBar.OnProgressChangedListener handleSeekBarChange = new BubbleSeekBar.OnProgressChangedListener() {
        @Override
        public void onProgressChanged(BubbleSeekBar bubbleSeekBar, int progress, float progressFloat, boolean fromUser) {
        }

        @Override
        public void getProgressOnActionUp(BubbleSeekBar bubbleSeekBar, int progress, float progressFloat) {
        }

        @Override
        public void getProgressOnFinally(BubbleSeekBar bubbleSeekBar, int progress, float progressFloat, boolean fromUser) {
            annotationValue = progress;
            setTitleText(annotationValue);
        }
    };

    private ImageButton.OnClickListener handleCloseClick = new ImageButton.OnClickListener() {
        @Override
        public void onClick(View v) {
            disable();

            if (interactionListener != null) {
                interactionListener.onAnnotationClose();
            }
        }
    };

    private Button.OnClickListener handleSubmitClick = new Button.OnClickListener() {
        @Override
        public void onClick(View v) {
            submit();
        }
    };

    private void setTitleText(int value) {
        int
                veryNegative = isFact ?
                R.string.annotation_input_title_false_very :
                R.string.annotation_input_title_biased_very,
                negative = isFact ?
                        R.string.annotation_input_title_false :
                        R.string.annotation_input_title_biased,
                somewhatNegative = isFact ?
                        R.string.annotation_input_title_false_somewhat :
                        R.string.annotation_input_title_biased_somewhat,
                somewhatPositive = isFact ?
                        R.string.annotation_input_title_true_somewhat :
                        R.string.annotation_input_title_unbiased_somewhat,
                positive = isFact ?
                        R.string.annotation_input_title_true :
                        R.string.annotation_input_title_unbiased,
                veryPositive = isFact ?
                        R.string.annotation_input_title_true_very :
                        R.string.annotation_input_title_unbiased_very,
                defaultStringId = isFact ?
                        R.string.annotation_input_title_default_fact :
                        R.string.annotation_input_title_default_opinion;

        switch (value) {
            case -50:
                title.setText(getString(veryNegative));
                break;
            case -30:
                title.setText(getString(negative));
                break;
            case -10:
                title.setText(getString(somewhatNegative));
                break;
            case 10:
                title.setText(getString(somewhatPositive));
                break;
            case 30:
                title.setText(getString(positive));
                break;
            case 50:
                title.setText(getString(veryPositive));
                break;
            default:
                title.setText(defaultStringId);
                break;
        }
    }

    public void submit() {
        final String comment = commentText.getText().toString();
        final String source = sourceText.getText().toString();

        if (annotationValue == 0) {
            String valueErrorText = isFact ?
                    getString(R.string.annotation_input_error_no_value_fact) :
                    getString(R.string.annotation_input_error_no_value_opinion);

            showError(valueErrorText);
            return;
        }

        if (comment.length() == 0) {
            showError(getString(R.string.annotation_input_error_no_comment));
            return;
        }

        if (isFact && source.length() == 0 && !primarySource.isChecked()) {
            showError(getString(R.string.annotation_input_error_no_source));
            return;
        }

        hideError();
        disable();

        if (interactionListener != null) {
            Annotation.AnnotationSourceCheckerCallback sourceCb = new Annotation.AnnotationSourceCheckerCallback() {
                @Override
                public void onChecked() {
                    hideError();
                    disable();
                    showProgress(getString(R.string.annotation_input_progress));

                    Annotation.AnnotationSubmitCallback cb = new Annotation.AnnotationSubmitCallback() {
                        @Override
                        public void onSubmit(Annotation annotation) {
                            hideProgress();
                            interactionListener.onAnnotationClose();
                        }

                        @Override
                        public void onAnnotationError(int errorCode) {
                            switch (errorCode) {
                                case Article.ARTICLE_ANNOTATION_ERROR_ALREADY_SUBMITTED:
                                    showError(getString(R.string.annotation_input_error_already_submitted));
                                    break;
                                case Article.ARTICLE_ANNOTATION_ERROR_INTERNAL:
                                    showError(getString(R.string.annotation_input_error_general));
                                default:
                                    showError(getString(R.string.annotation_input_error_general));
                            }
                        }

                        @Override
                        public void onError(Exception e) {
                            showError(getString(R.string.article_add_annotation_error));
                        }
                    };


                    interactionListener.onAnnotationSubmit(
                            cb,
                            annotationType,
                            annotationStartIndex,
                            annotationEndIndex,
                            annotationValue,
                            comment,
                            source,
                            subscribe.isChecked()
                    );


                }

                @Override
                public void onSourceError() {

                    showError(getString(R.string.annotation_input_error_source_invalid));

                }

                @Override
                public void onError(Exception e) {

                    Log.d(TAG, "onError: " + e);

                }
            };

            if (primarySource.isChecked()) {
                sourceCb.onChecked();
            } else {
                showProgress(getString(R.string.annotation_input_checking_source));

                interactionListener.onAnnotationSourceChecker(
                        sourceCb,
                        source
                );
            }
        }
    }

    public void showError(String errorMessage) {
        hideProgress();
        enable();

        errorText.setText(errorMessage);
        errorContainer.setVisibility(View.VISIBLE);
    }

    public void hideError() {
        errorContainer.setVisibility(View.GONE);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        Fragment parentFragment = getParentFragment();

        if (parentFragment instanceof AnnotationInputFragmentInteractionListener) {
            interactionListener = (AnnotationInputFragmentInteractionListener) parentFragment;
        } else {
            throw new RuntimeException(parentFragment.toString()
                    + " must implement AnnotationInputFragmentInteractionListener");
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Helper.hideKeyboard(getActivity().getApplicationContext(), getActivity().getCurrentFocus());
    }

    public void enable() {
        closeButton.setEnabled(true);
        valueSeekBar.setEnabled(true);
        commentText.setEnabled(true);
        sourceText.setEnabled(true);
        primarySource.setEnabled(true);
        subscribe.setEnabled(true);
        submitButton.setEnabled(true);
    }

    public void disable() {
        closeButton.setEnabled(false);
        valueSeekBar.setEnabled(false);
        commentText.setEnabled(false);
        sourceText.setEnabled(false);
        primarySource.setEnabled(false);
        subscribe.setEnabled(false);
        submitButton.setEnabled(false);
    }

    public void showProgress(String s) {

        progressText.setText(s);

        progressContainer.setVisibility(View.VISIBLE);
    }

    public void hideProgress() {
        progressContainer.setVisibility(View.GONE);
    }

    public interface AnnotationInputFragmentInteractionListener {
        void onAnnotationSubmit(Annotation.AnnotationSubmitCallback cb, String type, int startIndex, int endIndex, int value, String comment, String source, boolean subscribe);
        void onAnnotationClose();
        void onAnnotationSourceChecker(Annotation.AnnotationSourceCheckerCallback cb, String source);
    }
}
