package ru.appkode.school.gui;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import ru.appkode.school.R;
import ru.appkode.school.data.ClientInfo;


/**
 * Created by lexer on 02.08.14.
 */
public class ClientListAdapter extends ArrayAdapter<ClientInfo> {

    private List<ClientInfo> mClientsInfo;
    private int mResId;

    public ClientListAdapter(Context context, int resource, List<ClientInfo> clientsInfo) {
        super(context, resource, clientsInfo);
        mClientsInfo = clientsInfo;
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
            holder.lockImage = (ImageView) v.findViewById(R.id.lock_image);
            holder.group = (TextView) v.findViewById(R.id.group);
            holder.name = (TextView) v.findViewById(R.id.name);
            holder.otherBlock = (TextView) v.findViewById(R.id.other_teacher_lock);
            holder.isSelected = (CheckBox) v.findViewById(R.id.choose);
            v.setTag(holder);
        } else {
            holder = (ViewHolder) v.getTag();
        }

        final ClientInfo clientInfo = mClientsInfo.get(position);
        holder.group.setText(clientInfo.group);
        holder.name.setText(clientInfo.name + " " + clientInfo.lastName);

        if (clientInfo.isBlockedByOther) {
            holder.otherBlock.setVisibility(View.GONE);
        } else {
            holder.otherBlock.setVisibility(View.VISIBLE);
        }

        holder.isSelected.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                clientInfo.isChosen = isChecked;
            }
        });
        holder.isSelected.setChecked(clientInfo.isChosen);

        return v;
    }

    public void setClientsList(List<ClientInfo> clientsInfo) {
        if (mClientsInfo != clientsInfo) {
            mClientsInfo.clear();
            mClientsInfo.addAll(clientsInfo);
        }
        notifyDataSetChanged();
    }

    private class ViewHolder {
        ImageView lockImage;
        TextView group;
        TextView name;
        TextView otherBlock;
        CheckBox isSelected;
    }
}
