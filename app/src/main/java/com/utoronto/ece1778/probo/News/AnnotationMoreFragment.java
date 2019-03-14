package com.utoronto.ece1778.probo.News;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.utoronto.ece1778.probo.R;

public class AnnotationMoreFragment extends Fragment {
    private static final String ARG_NUM_MORE = "numMore",
                                ARG_TYPE = "type",
                                ARG_START_INDEX = "startIndex",
                                ARG_END_INDEX = "endIndex";

    private int numMore;
    private String type;
    private int startIndex;
    private int endIndex;

    private Button moreButton;

    private AnnotationMoreFragmentInteractionListener fragmentInterface;

    public AnnotationMoreFragment() {
    }

    public static AnnotationMoreFragment newInstance(int numMore, String type, int startIndex, int endIndex) {
        AnnotationMoreFragment fragment = new AnnotationMoreFragment();
        Bundle args = new Bundle();

        args.putInt(ARG_NUM_MORE, numMore);
        args.putString(ARG_TYPE, type);
        args.putInt(ARG_START_INDEX, startIndex);
        args.putInt(ARG_END_INDEX, endIndex);

        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            numMore = getArguments().getInt(ARG_NUM_MORE);
            type = getArguments().getString(ARG_TYPE);
            startIndex = getArguments().getInt(ARG_START_INDEX);
            endIndex = getArguments().getInt(ARG_END_INDEX);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_annotation_more, container, false);

        moreButton = v.findViewById(R.id.more);

        populate();

        moreButton.setOnClickListener(handleMoreClick);

        return v;
    }

    private void populate() {
        moreButton.setText(getString(R.string.annotation_more_fragment_button, numMore));
    }

    private View.OnClickListener handleMoreClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (fragmentInterface != null) {
                fragmentInterface.onMoreAnnotations(type, startIndex, endIndex);
            }
        }
    };

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        Fragment parentFragment = getParentFragment();

        if (parentFragment instanceof AnnotationMoreFragmentInteractionListener) {
            fragmentInterface = (AnnotationMoreFragmentInteractionListener) parentFragment;
        } else {
            throw new RuntimeException((parentFragment != null ? parentFragment : context).toString()
                    + " must implement AnnotationMoreFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        fragmentInterface = null;
    }

    public interface AnnotationMoreFragmentInteractionListener {
        void onMoreAnnotations(String type, int startIndex, int endIndex);
    }
}
