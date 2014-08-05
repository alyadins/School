package ru.appkode.school.gui;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import java.util.List;

import ru.appkode.school.R;
import ru.appkode.school.data.ServerInfo;
import ru.appkode.school.data.TeacherInfo;

/**
 * Created by lexer on 01.08.14.
 */
public class ServerListAdapter extends ArrayAdapter<ServerInfo> {


    private List<ServerInfo> mServers;
    private int mResId;

    public ServerListAdapter(Context context, int resource, List<ServerInfo> servers) {
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

        ServerInfo server = mServers.get(position);
        TeacherInfo info = server.teacherInfo;

        String fullTeacherName = info.name + " " + info.secondName + " " + " " + info.lastName + " ";

        holder.name.setText(fullTeacherName);
        holder.subject.setText(info.subject);
        holder.isConnected.setChecked(server.isConnected);

        return v;
    }


    public void setData(List<ServerInfo> serverList) {
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

}
