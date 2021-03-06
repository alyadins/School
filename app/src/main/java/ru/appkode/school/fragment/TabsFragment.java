package ru.appkode.school.fragment;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TabHost;
import android.widget.TextView;

import ru.appkode.school.R;

/**
 * Created by lexer on 01.08.14.
 */
public class TabsFragment extends Fragment {

    public static final String TAG = "tabsFragment";

    private static final String LEFT = "left";
    private static final String RIGHT = "right";

    private Fragment mLeftFragment;
    private Fragment mRightFragment;
    private String mLeftTitle;
    private String mRightTitle;
    private String mLeftTag;
    private String mRightTag;

    private TabHost mTabHost;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_user_server_list, null);

        mTabHost = (TabHost) v.findViewById(R.id.tabhost);

        mTabHost.setup();

        TabHost.TabSpec spec = mTabHost.newTabSpec(LEFT);
        spec.setIndicator("");
        spec.setContent(R.id.left_tab);
        mTabHost.addTab(spec);

        spec = mTabHost.newTabSpec(RIGHT);
        spec.setIndicator("");
        spec.setContent(R.id.right_tab);
        mTabHost.addTab(spec);

        mTabHost.setCurrentTab(0);

        setLeftTitle(mLeftTitle);
        setRightTitle(mRightTitle);
        setLeftFragment(mLeftFragment, mLeftTag);
        setRightFragment(mRightFragment, mRightTag);

        return v;
    }


    public void setLeftTitle(String title) {
        mLeftTitle = title;
        setTitle(mLeftTitle, 0);
    }

    public void setRightTitle(String title) {
        mRightTitle = title;
        setTitle(mRightTitle, 1);
    }

    public void setLeftFragment(Fragment fragment, String tag) {
        mLeftTag = tag;
        if (mLeftFragment != null && fragment.getId() == mLeftFragment.getId())
            return;
        mLeftFragment = fragment;
        if (mLeftFragment != null) {
            FragmentTransaction transaction = getFragmentManager().beginTransaction();
            transaction.add(R.id.left_tab, mLeftFragment, tag);
            transaction.commit();
        }
    }

    public void setRightFragment(Fragment fragment, String tag) {
        mRightTag = tag;
        if (mRightFragment != null && fragment.getId() == mRightFragment.getId())
            return;
        mRightFragment = fragment;
        if (mRightFragment != null) {
            FragmentTransaction transaction = getFragmentManager().beginTransaction();
            transaction.add(R.id.right_tab, mRightFragment, tag);
            transaction.commit();
        }
    }

    private void setTitle(String title, int position) {
        if (mTabHost != null) {
            TextView textView = (TextView) mTabHost.getTabWidget().getChildTabViewAt(position).findViewById(android.R.id.title);
            textView.setText(title);
        }
    }
}
