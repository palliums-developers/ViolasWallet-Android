package com.violas.wallet.widget.adapter;


import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.fragment.app.FragmentTransaction;
import androidx.viewpager.widget.PagerAdapter;


import com.violas.wallet.ui.main.applyFor.ApplyForSSOFragment;
import com.violas.wallet.ui.main.message.ApplyMessageFragment;

import java.util.ArrayList;
import java.util.List;

/**
 * @author QuincySx
 * @date 2019/9/20 下午5:24
 */
public class FragmentPager2AdapterSupport extends FragmentPagerAdapter {
    private FragmentManager fm;
    private boolean[] flags;//标识,重新设置fragment时全设为true

    private final List<Fragment> mFragmentList = new ArrayList<>();

    public FragmentPager2AdapterSupport(FragmentManager manager) {
        super(manager);
    }

//    @NonNull
//    @Override
//    public Object instantiateItem(@NonNull ViewGroup container, int position) {
//        if (flags != null && flags[position]) {
//            /**得到缓存的fragment, 拿到tag并替换成新的fragment*/
//            Fragment fragment = (Fragment) super.instantiateItem(container, position);
//            String fragmentTag = fragment.getTag();
//            FragmentTransaction ft = fm.beginTransaction();
//            ft.remove(fragment);
//            fragment = mFragmentList.get(position);
//            ft.add(container.getId(), fragment, fragmentTag);
//            ft.attach(fragment);
//            ft.commit();
//            /**替换完成后设为false*/
//            flags[position] = false;
//            if (fragment != null) {
//                return fragment;
//            } else {
//                return super.instantiateItem(container, position);
//            }
//        } else {
//            return super.instantiateItem(container, position);
//        }
//    }

    public void addFragment(Fragment fragment) {
        mFragmentList.add(fragment);
    }

    public void replaceFragment(int index, Fragment fragment) {
        mFragmentList.set(index, fragment);
    }

    @Override
    public long getItemId(int position) {
        return mFragmentList.get(position).hashCode();
    }

    @NonNull
    @Override
    public Fragment getItem(int position) {
        return mFragmentList.get(position);
    }

    @Override
    public int getItemPosition(@NonNull Object object) {
        if (object instanceof ApplyForSSOFragment || object instanceof ApplyMessageFragment) {
            return PagerAdapter.POSITION_NONE;
        } else {
            return super.getItemPosition(object);
        }
    }

    @Override
    public int getCount() {
        return mFragmentList.size();
    }
}
