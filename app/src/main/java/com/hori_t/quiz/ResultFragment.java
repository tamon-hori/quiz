package com.hori_t.quiz;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;

import com.hori_t.quiz.listView.ResultAdapter;
import com.hori_t.quiz_lib.QuizPlayer;

import java.util.ArrayList;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link QuizEventListener} interface
 * to handle interaction events.
 * Use the {@link ResultFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ResultFragment extends Fragment {

    @Nullable
    private QuizEventListener mListener;

    public ResultFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment WaitingMemberFragment.
     */
    public static ResultFragment newInstance() {
        return new ResultFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_result, container, false);

        Button button = rootView.findViewById(R.id.endButton);
        button.setOnClickListener(v -> mListener.changeEvent(
                QuizEventListener.Event.CONFIRMED_RESULT, null));

        ListView listView = rootView.findViewById(R.id.resultList);
        // 結果のリスト取得
        ArrayList<QuizPlayer> resultList = (ArrayList<QuizPlayer>)
                getArguments().getSerializable("ResultList");

        ResultAdapter mResultAdapter = new ResultAdapter(inflater.getContext(), resultList);
        listView.setAdapter(mResultAdapter);
        return rootView;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof QuizEventListener) {
            mListener = (QuizEventListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement QuizEventListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

}
