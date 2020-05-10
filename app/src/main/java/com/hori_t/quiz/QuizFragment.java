package com.hori_t.quiz;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.hori_t.quiz_lib.Question.AnswerChoice;
import com.hori_t.quiz_lib.QuizPlayer;
import com.sky.hori_t.util.Log;

import java.util.ArrayList;
import java.util.Collections;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link QuizEventListener} interface
 * to handle interaction events.
 * Use the {@link QuizFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class QuizFragment extends Fragment {
    @Nullable
    private QuizEventListener mListener;
    @Nullable
    private SoundPool mSoundPool;
    private int mQuestionSoundId;
    private int mBgmStream;

    @NonNull
    private ArrayList<TextView> mPlayerView = new ArrayList<>();
    @Nullable
    private SoundButton mButtonA;
    @Nullable
    private SoundButton mButtonB;
    @Nullable
    private SoundButton mButtonC;
    @Nullable
    private SoundButton mButtonD;
    @Nullable
    private TextView mQuestionView;
    @Nullable
    private TextView mTimeLimitView;
    @Nullable
    private TextView mQuizNumView;
    private int quizCnt = 0;

    private boolean isAnswerSelected = false;
    @Nullable
    private CountDownTimer mTimer;

    public QuizFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment QuizFragment.
     */
    public static QuizFragment newInstance() {
        return new QuizFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.vMethodIn();
        super.onCreate(savedInstanceState);

        mSoundPool = new SoundPool.Builder()
                .setMaxStreams(2)
                .build();
        mSoundPool.setOnLoadCompleteListener((soundPool, sampleId, status) -> {
            if (mQuestionSoundId == sampleId) {
                soundPool.play(sampleId, 1.0f, 1.0f,
                        0,0,1.0f);
            } else {
                mBgmStream = soundPool.play(sampleId, 1.0f, 1.0f,
                        0, -1, 1.0f);
            }
        });
        mSoundPool.load(getActivity().getApplicationContext(), R.raw.loop02, 1);
        mQuestionSoundId = mSoundPool.load(
                getActivity().getApplicationContext(), R.raw.question1, 1);
        Log.vMethodOut();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.vMethodIn();
        Log.vMethodOut();
        return inflater.inflate(R.layout.fragment_quiz, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        Log.vMethodIn();
        mQuestionView = view.findViewById(R.id.quiz_text);
        mTimeLimitView = view.findViewById(R.id.time);
        mQuizNumView = view.findViewById(R.id.quiz_number);

        mButtonA = view.findViewById(R.id.answerA);
        mButtonA.setOnClickListener(v -> answer(AnswerChoice.A));
        mButtonB = view.findViewById(R.id.answerB);
        mButtonB.setOnClickListener(v -> answer(AnswerChoice.B));
        mButtonC = view.findViewById(R.id.answerC);
        mButtonC.setOnClickListener(v -> answer(AnswerChoice.C));
        mButtonD = view.findViewById(R.id.answerD);
        mButtonD.setOnClickListener(v -> answer(AnswerChoice.D));

        Bundle bundle = getArguments();
        ArrayList<QuizPlayer> playerList =
                (ArrayList<QuizPlayer>) bundle.getSerializable("PlayerList");
        switch (playerList.size()) {
            case 4:
                TextView player4 = view.findViewById(R.id.player4);
                mPlayerView.add(player4);
            case 3:
                TextView player3 = view.findViewById(R.id.player3);
                mPlayerView.add(player3);
            case 2:
                TextView player2 = view.findViewById(R.id.player2);
                mPlayerView.add(player2);
        }
        TextView player1 = view.findViewById(R.id.player1);
        mPlayerView.add(player1);
        Collections.reverse(mPlayerView);

        if (null != bundle.getString("QuizText")) {
            onQuestion(playerList, bundle.getString("QuizText"));
        }
        Log.vMethodOut();
    }

    void onQuestion(@NonNull ArrayList<QuizPlayer> playerList, @NonNull String questionText) {
        Log.vMethodIn();
        quizCnt++;
        String quizNumText =
                quizCnt + "/" + Constants.QUESTION_NUM + getString(R.string.question_num);
        mQuizNumView.setText(quizNumText);
        setAllPlayerView(playerList);
        isAnswerSelected = false;
        mButtonA.setPressed(false);
        mButtonB.setPressed(false);
        mButtonC.setPressed(false);
        mButtonD.setPressed(false);
        mButtonA.setEnabled(true);
        mButtonB.setEnabled(true);
        mButtonC.setEnabled(true);
        mButtonD.setEnabled(true);

        mQuestionView.setText(questionText);

        mTimeLimitView.setText(String.valueOf(Constants.ANSWER_TIME_LIMIT));
        mTimer = new CountDownTimer(
                Constants.ANSWER_TIME_LIMIT * 1000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                mTimeLimitView.setText(String.valueOf(millisUntilFinished / 1000));
            }

            @Override
            public void onFinish() {
                if (isAnswerSelected == false) {
                    mListener.answer(AnswerChoice.Timeout);
                }
            }
        };
        mTimer.start();
        mSoundPool.play(
                mQuestionSoundId,
                1.0f, 1.0f, 0, 0, 1.0f);
        Log.vMethodOut();
    }

    void onPlayerAnswered(int playerNum) {
        Log.vMethodIn();
        TextView textView = mPlayerView.get(playerNum);
        String text = textView.getText().toString() + "\n解答済み";
        textView.setText(text);
        Log.vMethodOut(text);
    }

    void onPlayerDisconnected(int playerNum) {
        Log.vMethodIn();
        TextView textView = mPlayerView.get(playerNum);
        textView.setBackgroundColor(Color.RED);
        Log.vMethodOut();
    }

    private void answer(@NonNull AnswerChoice choice) {
        Log.vMethodIn();
        isAnswerSelected = true;
        mTimer.cancel();
        mListener.answer(choice);
        mButtonA.setEnabled(false);
        mButtonB.setEnabled(false);
        mButtonC.setEnabled(false);
        mButtonD.setEnabled(false);

        switch (choice) {
            case A:
                mButtonA.setPressed(true);
                break;
            case B:
                mButtonB.setPressed(true);
                break;
            case C:
                mButtonC.setPressed(true);
                break;
            case D:
                mButtonD.setPressed(true);
                break;
        }
        Log.vMethodOut();
    }

    private void setAllPlayerView(@NonNull ArrayList<QuizPlayer> playerList) {
        Log.vMethodIn();
        switch (playerList.size()) {
            case 4: {
                setPlayerView(mPlayerView.get(3), playerList.get(3));
            }
            case 3: {
                setPlayerView(mPlayerView.get(2), playerList.get(2));
            }
            case 2: {
                setPlayerView(mPlayerView.get(1), playerList.get(1));
            }
        }
        setPlayerView(mPlayerView.get(0), playerList.get(0));

        int myPlayerNum = 0;
        for (QuizPlayer player : playerList) {
            if (player.isMe()) {
                break;
            }
            myPlayerNum++;
        }
        mPlayerView.get(myPlayerNum).setTextColor(Color.BLUE);
        mPlayerView.get(myPlayerNum).setTypeface(Typeface.DEFAULT_BOLD);
        Log.vMethodOut();
    }

    private void setPlayerView(@NonNull TextView playerView, @NonNull QuizPlayer player) {
        Log.vMethodIn();
        String idAndPoint = player.getId() + "\n" + player.getCorrectAnswerPoint();
        playerView.setText(idAndPoint);
        Log.vMethodOut(idAndPoint);
    }

    @Override
    public void onAttach(Context context) {
        Log.vMethodIn();
        super.onAttach(context);
        if (context instanceof QuizEventListener) {
            mListener = (QuizEventListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
        Log.vMethodOut();
    }

    @Override
    public void onDetach() {
        Log.vMethodIn();
        super.onDetach();
        mListener = null;
        mTimer.cancel();
        mSoundPool.stop(mBgmStream);
        mSoundPool.release();
        Log.vMethodOut();
    }

}
