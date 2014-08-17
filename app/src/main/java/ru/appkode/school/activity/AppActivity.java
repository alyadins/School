package ru.appkode.school.activity;

import android.app.Activity;
import android.app.ListActivity;
import android.os.Bundle;
import android.widget.ArrayAdapter;

import java.util.ArrayList;

import ru.appkode.school.service.ClientService;

/**
 * Created by lexer on 17.08.14.
 */
public class AppActivity extends ListActivity {

    private ArrayList<String> mWhiteList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mWhiteList = getIntent().getStringArrayListExtra(ClientService.WHITE_LIST_PARAM);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, mWhiteList);
        setListAdapter(adapter);
    }
}
