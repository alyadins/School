package ru.appkode.school.fragment;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import ru.appkode.school.R;

/**
 * Created by lexer on 02.08.14.
 */
public class TeacherInfoFragment extends Fragment{
    public static final String TAG = "teacherInfoFragment";

    private String mName;
    private String mSubject;

    private TextView mNameTextView;
    private TextView mSubjectTextView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_teacher_info, null);

        mNameTextView = (TextView) v.findViewById(R.id.name);
        mSubjectTextView = (TextView) v.findViewById(R.id.subject);

        setName(mName);
        setSubject(mSubject);

        return v;
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
