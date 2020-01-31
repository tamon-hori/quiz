package com.example.hori_t;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.example.hori_t.listView.JoinedPlayerAdapter;
import com.example.quiz_lib.QuizPlayer;

import java.util.ArrayList;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link QuizEventListener} interface
 * to handle interaction events.
 * Use the {@link WaitingMemberFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class WaitingMemberFragment extends Fragment {
    @Nullable
    private QuizEventListener mListener;
    @NonNull
    private ArrayList<QuizPlayer> mPlayerList = new ArrayList<>();
    @Nullable
    private JoinedPlayerAdapter mPlayerAdapter;
    @Nullable
    private SoundButton button;

    public WaitingMemberFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment WaitingMemberFragment.
     */
    public static WaitingMemberFragment newInstance() {
        return new WaitingMemberFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_waiting_member, container, false);
        ListView listView = rootView.findViewById(R.id._memberList);
        mPlayerAdapter = new JoinedPlayerAdapter(inflater.getContext(), mPlayerList);
        listView.setAdapter(mPlayerAdapter);
        return rootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        button = view.findViewById(R.id.button);
        button.setOnClickListener(v ->
                mListener.changeEvent(QuizEventListener.Event.GATHER_MEMBER, null));
        button.setEnabled(false);
        if (PlayerType.Guest == PlayerType.getType(getArguments().getInt("PlayerType"))) {
            button.setVisibility(View.INVISIBLE);
        }
    }

    public void changePlayerList(@NonNull ArrayList<QuizPlayer> playerList) {
        mPlayerAdapter.changePlayers(playerList);
        if ((PlayerType.Host == PlayerType.getType(getArguments().getInt("PlayerType")))
                && (2 <= mPlayerAdapter.getCount())) {
            button.setEnabled(true);
        }
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
