package com.utoronto.ece1778.probo.News;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.utoronto.ece1778.probo.R;

import java.util.ArrayList;

import javax.annotation.Nullable;

public class NewsFragment extends Fragment {
    private ArrayList<Fragment> fragments;
    private ViewPager viewPager;
    private PagerAdapter pagerAdapter;

    private String currentArticleId;

    private boolean articleUpdated = false;
    private boolean articleExtensionUpdated = false;

    public NewsFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_news, container, false);

        fragments = new ArrayList<>();
        fragments.add(new ArticlesFragment());

        viewPager = v.findViewById(R.id.view_pager);
        pagerAdapter = new ScreenSlidePagerAdapter(getActivity().getSupportFragmentManager());
        viewPager.setAdapter(pagerAdapter);

        return v;
    }

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

    public void onRouteToArticleExtension(Fragment fragment) {
        if (fragments.size() > 2) {
            fragments.subList(2, fragments.size()).clear();
        }

        articleExtensionUpdated = true;

        fragments.add(fragment);
        pagerAdapter.notifyDataSetChanged();

        viewPager.setCurrentItem(2);
    }

    public void onAnnotationInput(String quote, String type, int startIndex, int endIndex, int value) {
        if (fragments.size() > 1 && fragments.get(1) instanceof ArticleFragment) {
            AnnotationInputFragment annotationInputFragment = AnnotationInputFragment.newInstance(
                    quote,
                    type,
                    startIndex,
                    endIndex,
                    value
            );

            onRouteToArticleExtension(annotationInputFragment);
        }
    }

    public void onAnnotationSubmit(String type, int startIndex, int endIndex, int value, String comment) {
        if (fragments.size() > 1 && fragments.get(1) instanceof ArticleFragment) {
            ((ArticleFragment) fragments.get(1)).onAnnotationSubmit(type, startIndex, endIndex, value, comment);
        }
    }

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

    public void onAnnotationClose() {
        if (fragments.size() > 1 && fragments.get(1) instanceof ArticleFragment) {
            ((ArticleFragment) fragments.get(1)).onAnnotationClose();
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
            } else if (object instanceof AnnotationsFragment) {
                articleExtensionUpdated = false;
                return POSITION_NONE;
            }

            return POSITION_UNCHANGED;
        }

        @Override
        public int getCount() {
            return fragments.size();
        }
    }
}
