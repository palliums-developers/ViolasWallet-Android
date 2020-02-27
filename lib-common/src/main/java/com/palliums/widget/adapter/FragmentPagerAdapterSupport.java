package com.palliums.widget.adapter;


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

    private final List<Fragment> mFragmentList = new ArrayList<>();

    public FragmentPagerAdapterSupport(FragmentManager manager) {
        super(manager);
    }

    @Override
    public Fragment getItem(int position) {
        return mFragmentList.get(position);
    }

    @Override
    public int getCount() {
        return mFragmentList.size();
    }

    public void addFragment(Fragment fragment) {
        mFragmentList.add(fragment);
    }

    public void replaceFragment(int index, Fragment fragment) {
        mFragmentList.set(index, fragment);
    }

}
