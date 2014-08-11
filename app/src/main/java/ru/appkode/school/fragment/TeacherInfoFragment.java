package ru.appkode.school.fragment;

import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import ru.appkode.school.R;

/**
 * Created by lexer on 02.08.14.
 */
public class TeacherInfoFragment extends Fragment implements View.OnClickListener {
    public static final String TAG = "teacherInfoFragment";

    public static final int BLOCK = 0;
    public static final int UNBLOCK = 1;
    public static final int DELETE = 2;

    public void setOnUserActionPerformListener(OnUserActionPerform l) {
        mOnUserActionPerform = l;
    }

    public interface OnUserActionPerform {
        public void onUserActionPerform(int action);
    }

    private String mName;
    private String mSubject;

    private TextView mNameTextView;
    private TextView mSubjectTextView;

    private ImageButton mBlockButton;
    private ImageButton mUnblockButton;
    private ImageButton mDeleteButton;

    private OnUserActionPerform mOnUserActionPerform;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_teacher_info, null);

        mNameTextView = (TextView) v.findViewById(R.id.name);
        mSubjectTextView = (TextView) v.findViewById(R.id.subject);
        mBlockButton = (ImageButton) v.findViewById(R.id.block_button);
        mUnblockButton = (ImageButton) v.findViewById(R.id.unblock_button);
        mDeleteButton = (ImageButton) v.findViewById(R.id.delete_button);

        mBlockButton.setOnClickListener(this);
        mUnblockButton.setOnClickListener(this);
        mDeleteButton.setOnClickListener(this);

        setName(mName);
        setSubject(mSubject);

        return v;
    }


    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (mOnUserActionPerform != null) {
            switch (id) {
                case R.id.block_button:
                    mOnUserActionPerform.onUserActionPerform(BLOCK);
                    break;
                case R.id.unblock_button:
                    mOnUserActionPerform.onUserActionPerform(UNBLOCK);
                    break;
                case R.id.delete_button:
                    mOnUserActionPerform.onUserActionPerform(DELETE);
                    break;
            }
        }
    }

    public void setName(String name, String secondName, String lastName) {
        mName = name + "\n" + secondName + "\n" + lastName;
        setName(mName);
    }

    private void setName(String fullname) {
        if (mNameTextView != null) {
            mNameTextView.setText(mName);
        }
    }

    public void setSubject(String subject) {
        mSubject = subject;

        if (mSubjectTextView != null) {
            mSubjectTextView.setText(mSubject);
        }
    }
}
