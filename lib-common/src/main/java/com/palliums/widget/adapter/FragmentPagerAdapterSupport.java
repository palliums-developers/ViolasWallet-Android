package com.palliums.widget.adapter;


import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * @author QuincySx
 * @date 2019/9/20 下午5:24
 */
public class FragmentPagerAdapterSupport extends FragmentPagerAdapter {

    private List<Fragment> mFragmentList;
    private List<String> mTitleList;

    public FragmentPagerAdapterSupport(FragmentManager manager) {
        super(manager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
    }

    @Override
    public Fragment getItem(int position) {
        return mFragmentList == null || position >= mFragmentList.size() ? null : mFragmentList.get(position);
    }

    @Nullable
    @Override
    public CharSequence getPageTitle(int position) {
        return mTitleList == null || position >= mTitleList.size() ? null : mTitleList.get(position);
    }

    @Override
    public int getCount() {
        return mFragmentList == null ? 0 : mFragmentList.size();
    }

    public void addFragment(Fragment fragment) {
        if (mFragmentList == null) {
            mFragmentList = new ArrayList<>();
        }
        mFragmentList.add(fragment);
    }

    public void addTitle(String title) {
        if (mTitleList == null) {
            mTitleList = new ArrayList<>();
        }
        mTitleList.add(title);
    }

    public void setFragments(List<Fragment> fragments) {
        mFragmentList = fragments;
    }

    public void setTitles(List<String> titles) {
        mTitleList = titles;
    }

    public void replaceFragment(int index, Fragment fragment) {
        if (mFragmentList != null && index < mFragmentList.size()) {
            mFragmentList.set(index, fragment);
        }
    }
}
