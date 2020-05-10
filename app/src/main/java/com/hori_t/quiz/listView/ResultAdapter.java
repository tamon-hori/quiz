package com.hori_t.quiz.listView;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.hori_t.quiz.R;
import com.hori_t.quiz_lib.QuizPlayer;

import java.util.ArrayList;

@SuppressWarnings({"unused"})
public class ResultAdapter extends BaseAdapter {
    private final Context mContext;
    private final ArrayList<QuizPlayer> mResultList;

    public ResultAdapter(@NonNull Context context) {
        mContext = context;
        mResultList = new ArrayList<>();
    }

    public ResultAdapter(@NonNull Context context, @NonNull ArrayList<QuizPlayer> players) {
        mContext = context;
        mResultList = players;
    }

    @Override
    public int getCount() {
        return mResultList.size();
    }

    @Override
    public QuizPlayer getItem(int position) {
        return mResultList.get(position);
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
        String rank = (position + 1) + "位";
        tagView.setText(rank);

        TextView nameView = convertView.findViewById(R.id.name);
        nameView.setText(mResultList.get(position).getId());

        TextView pointView = convertView.findViewById(R.id.point);
        String point = mResultList.get(position).getCorrectAnswerPoint()
                + mContext.getString(R.string.correct_point);
        pointView.setText(point);

        if (mResultList.get(position).isMe()) {
            // 自分は青で太字に
            tagView.setTextColor(Color.BLUE);
            tagView.setTypeface(Typeface.DEFAULT_BOLD);
            nameView.setTextColor(Color.BLUE);
            nameView.setTypeface(Typeface.DEFAULT_BOLD);
            pointView.setTextColor(Color.BLUE);
            pointView.setTypeface(Typeface.DEFAULT_BOLD);
        }

        return convertView;
    }
}
