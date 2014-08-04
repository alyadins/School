package ru.appkode.school.gui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import ru.appkode.school.R;
import ru.appkode.school.data.Client;

/**
 * Created by lexer on 02.08.14.
 */
public class ClientListAdapter extends ArrayAdapter<Client> {

    private List<Client> mClients;
    private int mResId;

    public ClientListAdapter(Context context, int resource, List<Client> clients) {
        super(context, resource, clients);
        mClients = clients;
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

        Client client = mClients.get(position);
        holder.group.setText(client.group);
        holder.name.setText(client.name);

        if (client.isLocketByOther) {
            holder.otherBlock.setVisibility(View.GONE);
        } else {
            holder.otherBlock.setVisibility(View.VISIBLE);
        }

        holder.isSelected.setChecked(client.isChosen);

        return v;
    }

    private class ViewHolder {
        ImageView lockImage;
        TextView group;
        TextView name;
        TextView otherBlock;
        CheckBox isSelected;
    }
}
