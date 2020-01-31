package com.example.hori_t.listView;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.example.hori_t.R;
import com.example.quiz_lib.QuizPlayer;

import java.util.ArrayList;

@SuppressWarnings({"unused"})
public class JoinedPlayerAdapter extends BaseAdapter {
    private final Context mContext;
    private ArrayList<QuizPlayer> mPlayerList;

    public JoinedPlayerAdapter(@NonNull Context context) {
        mContext = context;
        mPlayerList = new ArrayList<>();
    }

    public JoinedPlayerAdapter(@NonNull Context context, @NonNull ArrayList<QuizPlayer> players) {
        mContext = context;
        mPlayerList = players;
    }

    @Override
    public int getCount() {
        return mPlayerList.size();
    }

    @Override
    public QuizPlayer getItem(int position) {
        return mPlayerList.get(position);
    }

    public void changePlayers(@NonNull ArrayList<QuizPlayer> playerList) {
        mPlayerList = playerList;
        notifyDataSetChanged();
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (null == convertView) {
            LayoutInflater inflater = LayoutInflater.from(mContext);
            convertView = inflater.inflate(R.layout.player_list_item, parent, false);
        }

        TextView tagView = convertView.findViewById(R.id.tag);
        String order = (position + 1) + "人目";
        tagView.setText(order);

        TextView nameView = convertView.findViewById(R.id.name);
        nameView.setText(mPlayerList.get(position).getId());

        if (mPlayerList.get(position).isMe()) {
            // 自分は青で太字に
            tagView.setTextColor(Color.BLUE);
            tagView.setTypeface(Typeface.DEFAULT_BOLD);
            nameView.setTextColor(Color.BLUE);
            nameView.setTypeface(Typeface.DEFAULT_BOLD);
        }

        return convertView;
    }
}
