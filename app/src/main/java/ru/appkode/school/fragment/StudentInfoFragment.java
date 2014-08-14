package ru.appkode.school.fragment;

import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import ru.appkode.school.R;
import ru.appkode.school.util.StringUtil;

/**
 * Created by lexer on 01.08.14.
 */
public class StudentInfoFragment extends Fragment {

    public static final String TAG = "studentInfoFragment";

    private boolean mIsBlock = false;
    private String mUserName = "";
    private String mGroup = "";
    private String[] mStatus;

    private TextView mUserNameTextView;
    private TextView mGroupTextView;
    private ImageView mStatusImageView;
    private TextView mStatusTextView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mStatus = getActivity().getResources().getStringArray(R.array.status);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_user_info, null);

        mUserNameTextView = (TextView) v.findViewById(R.id.user_name);
        mGroupTextView = (TextView) v.findViewById(R.id.group);
        mStatusImageView = (ImageView) v.findViewById(R.id.status_image);
        mStatusTextView = (TextView) v.findViewById(R.id.status_message);

        setUserName(mUserName);
        setGroup(mGroup);
        setBlock(mIsBlock);

        return v;
    }

    public void setUserName(String userName) {
        mUserName = userName;
        if (mUserNameTextView != null)
            mUserNameTextView.setText(mUserName);
    }

    public void setGroup(String group) {
        mGroup = group;
        if (mGroupTextView != null)
            mGroupTextView.setText(mGroup);
    }

    public void setBlock(boolean block) {
        mIsBlock = block;

        if (mStatusTextView != null && mStatusTextView != null) {
            if (mIsBlock) {
                mStatusImageView.setImageResource(R.drawable.lock_big);
                mStatusTextView.setText(mStatus[1]);
            } else {
                mStatusImageView.setImageResource(R.drawable.unlock_big);
                mStatusTextView.setText(mStatus[0]);
            }
        }
    }
}
