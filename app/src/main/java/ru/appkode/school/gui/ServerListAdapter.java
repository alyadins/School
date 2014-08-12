package ru.appkode.school.gui;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import java.util.List;

import ru.appkode.school.R;
import ru.appkode.school.data.ParcelableServerInfo;

/**
 * Created by lexer on 01.08.14.
 */
public class ServerListAdapter extends ArrayAdapter<ParcelableServerInfo> {


    private List<ParcelableServerInfo> mServers;
    private int mResId;

    private OnClientStateChanged mOnClientStateChanged;

    public ServerListAdapter(Context context, int resource, List<ParcelableServerInfo> servers) {
        super(context, resource, servers);
        mServers = servers;
        this.mResId = resource;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        ViewHolder holder;

        View v = convertView;
        if (v == null) {
            LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = inflater.inflate(mResId, null);
            holder = new ViewHolder();
            holder.statusImage = (ImageView) v.findViewById(R.id.status_image);
            holder.name = (TextView) v.findViewById(R.id.name);
            holder.subject = (TextView) v.findViewById(R.id.subject);
            holder.isConnected = (Switch) v.findViewById(R.id.is_connected);
            v.setTag(holder);
        } else {
            holder = (ViewHolder) v.getTag();
        }

        final ParcelableServerInfo info = mServers.get(position);

        String fullTeacherName = info.name + " " + info.secondName + " " + " " + info.lastName + " ";

        holder.name.setText(fullTeacherName);
        holder.subject.setText(info.subject);
        holder.isConnected.setOnCheckedChangeListener(null);
        holder.isConnected.setChecked(info.isConnected);
        holder.isConnected.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (mOnClientStateChanged != null)
                    if (isChecked) {
                        mOnClientStateChanged.onClientConnect(info);
                    } else {
                        mOnClientStateChanged.onClientDisconnect(info);
                    }
            }
        });
        return v;
    }


    public void setData(List<ParcelableServerInfo> serverList) {
        mServers.clear();
        mServers.addAll(serverList);
        notifyDataSetChanged();
    }


    private class ViewHolder {
        ImageView statusImage;
        TextView name;
        TextView subject;
        Switch isConnected;
    }

    public void setOnClientStateChangeListener(OnClientStateChanged l) {
        mOnClientStateChanged = l;
    }
    public interface OnClientStateChanged {
        public void onClientDisconnect(ParcelableServerInfo info);
        public void onClientConnect(ParcelableServerInfo info);
    }

}
