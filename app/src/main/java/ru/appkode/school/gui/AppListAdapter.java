package ru.appkode.school.gui;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import ru.appkode.school.R;
import ru.appkode.school.activity.AppActivity;

/**
 * Created by lexer on 19.08.14.
 */
public class AppListAdapter extends ArrayAdapter<AppActivity.AppInfo> implements View.OnClickListener {

    private List<AppActivity.AppInfo> mInfos;

    private int mResId;
    public AppListAdapter(Context context, int resource, List<AppActivity.AppInfo> infos) {
        super(context, resource, infos);
        mInfos = infos;
        mResId = resource;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        View v = convertView;
        if (v == null) {
            LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = inflater.inflate(mResId, null);
            holder = new ViewHolder();
            holder.image = (ImageView) v.findViewById(R.id.image);
            holder.text = (TextView) v.findViewById(R.id.text);
            v.setTag(holder);
        } else {
            holder = (ViewHolder) v.getTag();
        }

        AppActivity.AppInfo info = mInfos.get(position);
        holder.image.setImageDrawable(info.icon);
        holder.text.setText(info.appname);

        return v;
    }

    private class ViewHolder {
        ImageView image;
        TextView text;

    }
    @Override
    public void onClick(View v) {
        Integer position = (Integer) v.getTag();
        AppActivity.AppInfo info = getItem(position);
        PackageManager manager = getContext().getPackageManager();
        Intent intent = manager.getLaunchIntentForPackage(info.packageName);
        if (intent != null){
            getContext().startActivity(intent);
        }
    }
}
