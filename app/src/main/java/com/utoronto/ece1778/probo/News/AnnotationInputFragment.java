package com.utoronto.ece1778.probo.News;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.style.BackgroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;

import com.utoronto.ece1778.probo.R;
import com.utoronto.ece1778.probo.Utils.Helper;

public class AnnotationInputFragment extends Fragment {
    private static final String
            ARG_QUOTE = "quote",
            ARG_TYPE = "type",
            ARG_START_INDEX = "startIndex",
            ARG_END_INDEX = "endIndex",
            ARG_VALUE = "value";

    private String
            annotationQuote,
            annotationType;

    private int
            annotationStartIndex,
            annotationEndIndex,
            annotationValue;

    private AnnotationInputFragmentInteractionListener interactionListener;
    private ImageButton closeButton;
    private TextView title;
    private EditText input;
    private RelativeLayout errorContainer;
    private TextView errorText;
    private Button submitButton;
    private RelativeLayout progressContainer;

    public AnnotationInputFragment() {}

    public static AnnotationInputFragment newInstance(String quote, String type, int startIndex, int endIndex, int value) {
        AnnotationInputFragment fragment = new AnnotationInputFragment();
        Bundle args = new Bundle();

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

        Switch typeSwitch = v.findViewById(R.id.type);
        closeButton = v.findViewById(R.id.close);
        TextView quote = v.findViewById(R.id.quote);
        title = v.findViewById(R.id.title);
        input = v.findViewById(R.id.input);
        errorContainer = v.findViewById(R.id.error_container);
        errorText = v.findViewById(R.id.error_text);
        submitButton = v.findViewById(R.id.submit);
        progressContainer = v.findViewById(R.id.progress_container);

        quote.setText(annotationQuote);

        int titleStringId = annotationValue == 1 ?
                R.string.annotation_input_title_true :
                R.string.annotation_input_title_false;

        title.setText(getString(titleStringId));

        typeSwitch.setOnCheckedChangeListener(handleTypeSwitch);
        closeButton.setOnClickListener(handleCloseClick);
        submitButton.setOnClickListener(handleSubmitClick);

        return v;
    }

    private CompoundButton.OnCheckedChangeListener handleTypeSwitch = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (isChecked) {
                annotationValue = 0;
                title.setText(getString(R.string.annotation_input_title_false));
            } else {
                annotationValue = 1;
                title.setText(getString(R.string.annotation_input_title_true));
            }
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

    public void submit() {
        String comment = input.getText().toString();

        if (comment.length() == 0) {
            showError(getString(R.string.annotation_input_error_no_comment));
            return;
        }

        hideError();
        disable();
        showProgress();

        if (interactionListener != null) {
            Annotation.AnnotationSubmitCallback cb = new Annotation.AnnotationSubmitCallback() {
                @Override
                public void onSubmit() {
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
                    input.getText().toString()
            );
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
        input.setEnabled(true);
        submitButton.setEnabled(true);
    }

    public void disable() {
        closeButton.setEnabled(false);
        input.setEnabled(false);
        submitButton.setEnabled(false);
    }

    public void showProgress() {
        progressContainer.setVisibility(View.VISIBLE);
    }

    public void hideProgress() {
        progressContainer.setVisibility(View.GONE);
    }

    public interface AnnotationInputFragmentInteractionListener {
        void onAnnotationSubmit(Annotation.AnnotationSubmitCallback cb, String type, int startIndex, int endIndex, int value, String comment);
        void onAnnotationClose();
    }
}
