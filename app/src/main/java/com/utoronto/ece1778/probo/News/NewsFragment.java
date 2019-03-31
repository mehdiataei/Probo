package com.utoronto.ece1778.probo.News;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.utoronto.ece1778.probo.R;

import java.util.ArrayList;

import javax.annotation.Nullable;


public class NewsFragment extends Fragment
        implements ArticlesFragment.ArticlesFragmentInteractionListener,
        ArticleFragment.ArticleFragmentInteractionListener,
        AnnotationInputFragment.AnnotationInputFragmentInteractionListener,
        AnnotationsFragment.AnnotationsFragmentInteractionListener {

    private static final String
            ARG_ARTICLE_ID = "articleId",
            ARG_ANNOTATION_ID = "annotationId",
            ARG_ANNOTATION_TYPE = "annotationType",
            ARG_ANNOTATION_START_INDEX = "annotationStartIndex",
            ARG_ANNOTATION_END_INDEX = "annotationEndIndex";

    private String intentArticleId, intentAnnotationId, intentAnnotationType;
    private int intentAnnotationStartIndex, intentAnnotationEndIndex;

    private ArrayList<Fragment> fragments;
    private ViewPager viewPager;
    private PagerAdapter pagerAdapter;

    private String currentArticleId;

    private boolean articleUpdated = false;
    private boolean articleExtensionUpdated = false;

    private int currentPageIndex;

    private NewsFragmentInteractionListener interactionListener;

    public NewsFragment() {
    }

    public static NewsFragment newInstance(String articleId, String annotationId, String annotationType,
                                           int annotationStartIndex, int annotationEndIndex) {

        NewsFragment fragment = new NewsFragment();
        Bundle args = new Bundle();

        args.putString(ARG_ARTICLE_ID, articleId);
        args.putString(ARG_ANNOTATION_ID, annotationId);
        args.putString(ARG_ANNOTATION_TYPE, annotationType);
        args.putInt(ARG_ANNOTATION_START_INDEX, annotationStartIndex);
        args.putInt(ARG_ANNOTATION_END_INDEX, annotationEndIndex);

        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        if (getArguments() != null) {
            intentArticleId = getArguments().getString(ARG_ARTICLE_ID);
            intentAnnotationId = getArguments().getString(ARG_ANNOTATION_ID);
            intentAnnotationType = getArguments().getString(ARG_ANNOTATION_TYPE);
            intentAnnotationStartIndex = getArguments().getInt(ARG_ANNOTATION_START_INDEX);
            intentAnnotationEndIndex = getArguments().getInt(ARG_ANNOTATION_END_INDEX);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_news, container, false);

        currentPageIndex = 0;

        fragments = new ArrayList<>();
        fragments.add(new ArticlesFragment());

        viewPager = v.findViewById(R.id.view_pager);
        pagerAdapter = new ScreenSlidePagerAdapter(getChildFragmentManager());

        viewPager.addOnPageChangeListener(handlePageChange);

        viewPager.setPageTransformer(true, new DepthPageTransformer());
        viewPager.setAdapter(pagerAdapter);

        if (intentArticleId != null &&
                intentAnnotationId != null &&
                intentAnnotationType != null &&
                intentAnnotationStartIndex >= 0 &&
                intentAnnotationEndIndex >= 0) {

            onRouteToArticleAnnotation(
                    intentArticleId,
                    intentAnnotationId,
                    intentAnnotationType,
                    intentAnnotationStartIndex,
                    intentAnnotationEndIndex
            );
        }

        return v;
    }

    private ViewPager.OnPageChangeListener handlePageChange = new ViewPager.OnPageChangeListener() {
        @Override
        public void onPageScrolled(int i, float v, int i1) {
        }

        @Override
        public void onPageSelected(int i) {
            if (i < currentPageIndex && fragments.size() > i + 1) {
                if (i == 0) {
                    articleUpdated = true;
                }

                fragments.subList(i + 1, fragments.size()).clear();
                pagerAdapter.notifyDataSetChanged();
            }

            currentPageIndex = i;
        }

        @Override
        public void onPageScrollStateChanged(int i) {
        }
    };

    @Override
    public void onRouteToArticle(String articleId) {
        if (fragments.size() > 1) {
            fragments.subList(1, fragments.size()).clear();
        }

        articleUpdated = !articleId.equals(currentArticleId);
        currentArticleId = articleId;

        fragments.add(ArticleFragment.newInstance(articleId));
        pagerAdapter.notifyDataSetChanged();

        viewPager.setCurrentItem(1);
    }

    public void onRouteToArticleAnnotation(String articleId, String annotationId, String annotationType,
                                           int annotationStartIndex, int annotationEndIndex) {

        if (fragments.size() > 1) {
            fragments.subList(1, fragments.size()).clear();
        }

        articleUpdated = !articleId.equals(currentArticleId);
        currentArticleId = articleId;

        fragments.add(ArticleFragment.newInstance(
                articleId,
                annotationId,
                annotationType,
                annotationStartIndex,
                annotationEndIndex
        ));

        pagerAdapter.notifyDataSetChanged();

        viewPager.setCurrentItem(1);
    }

    public boolean onBackPressed() {
        if (currentPageIndex == 0) {
            return false;
        }

        goToViewPage(currentPageIndex - 1);
        return true;
    }

    public void goToViewPage(int pageIndex) {
        viewPager.setCurrentItem(pageIndex, true);
        currentPageIndex = pageIndex;
    }

    public void onRouteToArticleExtension(Fragment fragment) {
        if (fragments.size() > 2) {
            fragments.subList(2, fragments.size()).clear();
        }

        articleExtensionUpdated = true;

        fragments.add(fragment);
        pagerAdapter.notifyDataSetChanged();

        viewPager.setCurrentItem(2);
    }

    @Override
    public void onAnnotationInput(final float sentiment, final String quote, final String type, final int startIndex, final int endIndex, final int value) {


        Boolean isFact = !(Math.abs(sentiment) > 0.5);

        if (fragments.size() > 1 && fragments.get(1) instanceof ArticleFragment) {
            AnnotationInputFragment annotationInputFragment = AnnotationInputFragment.newInstance(
                    isFact,
                    quote,
                    type,
                    startIndex,
                    endIndex,
                    value
            );

            onRouteToArticleExtension(annotationInputFragment);
        }


    }

    @Override
    public void onAnnotationSubmit(Annotation.AnnotationSubmitCallback cb, String type,
                                   int startIndex, int endIndex, int value, String comment,
                                   String source, String sentence, boolean subscribe) {

        if (fragments.size() > 1 && fragments.get(1) instanceof ArticleFragment) {
            ((ArticleFragment) fragments.get(1)).onAnnotationSubmit(
                    cb,
                    type,
                    startIndex,
                    endIndex,
                    value,
                    comment,
                    source,
                    sentence,
                    subscribe
            );
        }
    }


    @Override
    public void onAnnotationSourceChecker(Annotation.AnnotationSourceCheckerCallback cb, String source) {


        if (fragments.size() > 1 && fragments.get(1) instanceof ArticleFragment) {

            ((ArticleFragment) fragments.get(1)).onAnnotationSourceChecker(cb, source);

        }


    }

    @Override
    public void onMoreAnnotations(String type, int startIndex, int endIndex) {
        if (fragments.size() > 1 && fragments.get(1) instanceof ArticleFragment) {
            ArticleFragment articleFragment = (ArticleFragment) fragments.get(1);
            String articleId = articleFragment.getArticle().getId();

            AnnotationsFragment annotationsFragment = AnnotationsFragment.newInstance(
                    articleId,
                    type,
                    startIndex,
                    endIndex
            );

            onRouteToArticleExtension(annotationsFragment);
        }
    }

    @Override
    public void onAnnotationClose() {
        if (fragments.size() > 2 && fragments.get(2) instanceof AnnotationInputFragment) {
            viewPager.setCurrentItem(1);
            fragments.subList(2, fragments.size()).clear();
            pagerAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onAnnotationVote(Annotation annotation) {
    }

    @Override
    public void onRouteToProfile(String userId) {
        if (interactionListener != null) {
            interactionListener.onRouteToProfile(userId);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (context instanceof NewsFragmentInteractionListener) {
            interactionListener = (NewsFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement NewsFragmentInteractionListener");
        }
    }

    private class ScreenSlidePagerAdapter extends FragmentStatePagerAdapter {
        ScreenSlidePagerAdapter(FragmentManager fragmentManager) {
            super(fragmentManager);
        }

        @Override
        public Fragment getItem(int position) {
            return fragments.get(position);
        }

        @Override
        public int getItemPosition(@Nullable Object object) {
            if (object instanceof ArticleFragment && articleUpdated) {
                articleUpdated = false;
                return POSITION_NONE;
            } else if (object instanceof AnnotationInputFragment && articleExtensionUpdated) {
                articleExtensionUpdated = false;
                return POSITION_NONE;
            } else if (object instanceof AnnotationsFragment && articleExtensionUpdated) {
                articleExtensionUpdated = false;
                return POSITION_NONE;
            }

            return POSITION_UNCHANGED;
        }

        @Override
        public int getCount() {
            return fragments.size();
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            super.destroyItem(container, position, object);
        }
    }

    private class DepthPageTransformer implements ViewPager.PageTransformer {
        private static final float MIN_SCALE = 0.75f;

        public void transformPage(View view, float position) {
            int pageWidth = view.getWidth();

            if (position < -1) {
                view.setAlpha(0f);
            } else if (position <= 0) {
                view.setAlpha(1f);
                view.setTranslationX(0f);
                view.setScaleX(1f);
                view.setScaleY(1f);
            } else if (position <= 1) {
                view.setAlpha(1 - position);
                view.setTranslationX(pageWidth * -position);

                float scaleFactor = MIN_SCALE + (1 - MIN_SCALE) * (1 - Math.abs(position));
                view.setScaleX(scaleFactor);
                view.setScaleY(scaleFactor);
            } else {
                view.setAlpha(0f);
            }
        }
    }

    public interface NewsFragmentInteractionListener {
        void onRouteToProfile(String userId);
    }
}
