package com.example.quiz_lib;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.example.ble_communication_lib.BleDataCommunicationException;
import com.example.ble_communication_lib.BleDataCommunicationServer;
import com.example.quiz_lib.Question.AnswerChoice;
import com.example.quiz_lib.Question.Question;
import com.example.quiz_lib.packet.PayloadKey;
import com.example.quiz_lib.packet.QuizPacket;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Objects;

public class QuizHostManager {
    private enum State {
        WaitingMember,
        Quiz
    }

    public interface Callback {
        void onWaitingStarted(@NonNull QuizPlayer player);
        void onPlayerChanged(@NonNull ArrayList<QuizPlayer> playerList);
        void onQuestion(@NonNull Question question);
        void onPlayerAnswered(int playerNum);
        void onCorrectAnswer(boolean isCorrect, @NonNull AnswerChoice answer);
        void onPlayerDisconnected(int playerNum);
        void onResult(@NonNull ArrayList<QuizPlayer> result);
        void onStopped(@Nullable QuizException exception);
    }

    private static int mPlayerIdCnt = 0;

    @NonNull
    private final Handler mHandler;
    @NonNull
    private final BleDataCommunicationServer mServer;
    @NonNull
    private final BleDataCommunicationServer.Callback mCommunicationCallback =
            new BleDataCommunicationServer.Callback() {
                @Override
                public void onWaitingStarted() {
                     _onWaitingStarted();
                }

                @Override
                public void onWaitingStopped() {
                    mCallback.onStopped(null);
                }

                @Override
                public void onConnected(@NonNull BluetoothDevice device) {
                    Log.d(getClass().getSimpleName(), "Connected.");
                    addPlayer(device);
                }

                @Override
                public void onDisconnected(@NonNull BluetoothDevice device) {
                    _onDisconnected(device);
                }

                @Override
                public void onDataReceived(@NonNull BluetoothDevice device, @NonNull byte[] data) {
                    handlePacket(device, data);
                }

                @Override
                public void onError(@NonNull BleDataCommunicationException exception) {
                }
            };
    @NonNull
    private final CallbackExecutor mCallback;
    @Nullable
    private State mState;

    @NonNull
    private final QuizPlayer mMyPlayerInfo;
    @NonNull
    private final LinkedHashMap<BluetoothDevice, QuizPlayer> mPlayerMap = new LinkedHashMap<>();
    @Nullable
    private QuestionFactory mQuestionFactory;
    @Nullable
    private HostQuestion mNowQuestion;
    private int mQuestionNum;
    @NonNull
    private HashMap<QuizPlayer, Boolean> mAnswerState = new HashMap<>();
    @Nullable
    private AnswerChoice mAnswerChoice;

    public QuizHostManager(
            @NonNull Context context,
            @Nullable Looper dispatchQueueLooper,
            @NonNull Callback callback) throws UnsupportedOperationException {
        // 自スレッド生成
        HandlerThread handlerThread = new HandlerThread(getClass().getSimpleName());
        handlerThread.start();
        mHandler = new android.os.Handler(handlerThread.getLooper());

        // コールバックインスタンス生成
        Handler callbackHandler;
        if (dispatchQueueLooper == null) {
            HandlerThread callbackThread =
                    new HandlerThread(getClass().getSimpleName() + "Callback");
            callbackThread.start();
            callbackHandler = new Handler(callbackThread.getLooper());
        } else {
            callbackHandler = new Handler(dispatchQueueLooper);
        }
        mCallback = new CallbackExecutor(callback, callbackHandler);

        // BLE通信クラス生成
        mServer = new BleDataCommunicationServer(
                context, mHandler.getLooper(), mCommunicationCallback);

        mMyPlayerInfo = new QuizPlayer(makePlayerId());
        mPlayerMap.put(null, mMyPlayerInfo);
    }

    public void startWaiting() {
        mServer.startWaiting();
    }

    @NonNull
    public QuizPlayer getMyPlayerInfo() {
        return mMyPlayerInfo;
    }

    @NonNull
    public ArrayList<QuizPlayer> getAllPlayerInfo() {
        ArrayList<QuizPlayer> playerList = new ArrayList<>();
        for (BluetoothDevice device : mPlayerMap.keySet()) {
            playerList.add(mPlayerMap.get(device));
        }
        return playerList;
    }

    public void startQuiz(int questionNum) {
        mState = State.Quiz;
        mQuestionNum = questionNum;
        mHandler.post(() -> {
            for (BluetoothDevice device : mPlayerMap.keySet()) {
                mAnswerState.put(mPlayerMap.get(device), false);
            }
            mQuestionFactory = new QuestionFactory();
            sendQuestion();
        });
    }

    private void sendQuestion() {
        mNowQuestion = mQuestionFactory.getQuestion();
        mCallback.onQuestion(mNowQuestion);
        QuizPacket packet = QuizPacket.Creator.question(mNowQuestion.getQuestionText());
        sendAllGuestPlayers(packet);
    }

    public void answer(@NonNull AnswerChoice choice) {
        mHandler.post(() -> {
            mAnswerChoice = choice;
            handleAnswer(mMyPlayerInfo, choice);
        });
    }

    public void stop() {
        mHandler.post(() -> {
            mServer.stopWaiting();
        });
    }

    private void _onWaitingStarted() {
        mState = State.WaitingMember;
        mCallback.onWaitingStarted(mMyPlayerInfo);
        mCallback.onPlayerChanged(getAllPlayerInfo());
    }

    private void _onDisconnected(@NonNull BluetoothDevice device) {
        if (null == mState) {
            return;
        }
        switch (mState) {
            case WaitingMember: {
                mPlayerMap.remove(device);
                mCallback.onPlayerChanged(getAllPlayerInfo());
                sendAllGuestPlayers(QuizPacket.Creator.playerChanged(getAllPlayerInfo()));
                break;
            }
            case Quiz: {
                if (mServer.getAllDeviceInfo().isEmpty()) {
                    mCallback.onStopped(new QuizException());
                    return;
                }
                QuizPlayer player = mPlayerMap.get(device);
                player.disconnect();
                int playerNum = getPlayerNum(player);
                mCallback.onPlayerDisconnected(playerNum);
                sendAllGuestPlayers(QuizPacket.Creator.playerDisconnected(playerNum));
                break;
            }
        }
    }

    private void sendAllGuestPlayers(@NonNull QuizPacket packet) {
        for (BluetoothDevice device : mPlayerMap.keySet()) {
            if ((null == device) || (!mPlayerMap.get(device).isConnect())) {
                continue;
            }
            mServer.sendData(device, packet.getBytes());
        }
    }

    private void handlePacket(@NonNull BluetoothDevice device, @NonNull byte[] data) {
        QuizPacket packet = QuizPacket.Creator.parsePacket(data);
        Log.d("クイズLib", "[receivedPacket] type: " + packet.getType());
        switch (packet.getType()) {
            case Answer: {
                handleAnswer(
                        mPlayerMap.get(device),
                        (AnswerChoice) packet.getPayload(PayloadKey.Choice));
                break;
            }
        }
    }

    private void addPlayer(@NonNull BluetoothDevice device) {
        // 4人以上なら切断
        if (Constants.MAX_NUM_OF_PLAYER <= mPlayerMap.size()) {
            mServer.disconnect(device);
            return;
        }
        String newPlayerId = makePlayerId();
        mPlayerMap.put(device, new QuizPlayer(newPlayerId));
        // プレイヤーID送信
        mServer.sendData(device, QuizPacket.Creator.playerId(newPlayerId).getBytes());
        ArrayList<QuizPlayer> playerList = new ArrayList<>();
        for (BluetoothDevice key : mPlayerMap.keySet()) {
            playerList.add(mPlayerMap.get(key));
        }
        sendAllGuestPlayers(QuizPacket.Creator.playerChanged(playerList));
        mCallback.onPlayerChanged(playerList);
        Log.d(getClass().getSimpleName(), "Player changed.");
    }

    private void handleAnswer(@NonNull QuizPlayer player, @NonNull AnswerChoice choice) {
        mAnswerState.put(player, true);
        // 回答状況通知
        int playerNum = getPlayerNum(player);
        mCallback.onPlayerAnswered(playerNum);
        sendAllGuestPlayers(QuizPacket.Creator.playerAnswered(playerNum));
        // 正解判定
        Objects.requireNonNull(mNowQuestion);
        if (mNowQuestion.isCorrect(choice)) {
            player.addPoint();
        }
        // 全員回答完了なら回答状況リセット、正解送信
        if (!isAnswerFinished()) {
            return;
        }
        for (QuizPlayer playerKey : mAnswerState.keySet()) {
            mAnswerState.put(playerKey, false);
        }
        mCallback.onCorrectAnswer(
                mNowQuestion.isCorrect(mAnswerChoice), mNowQuestion.getCorrectAnswer());
        sendAllGuestPlayers(QuizPacket.Creator.correctAnswer(mNowQuestion.getCorrectAnswer()));
        // 得点状況送信
        sendAllGuestPlayers(QuizPacket.Creator.playersState(getAllPlayerInfo()));
        // mQuestionNumが0になるまで出題繰り返し
        mQuestionNum--;
        Runnable runnable;
        if (0 >= mQuestionNum) {
            runnable = this::returnResult;
        } else {
            runnable = this::sendQuestion;
        }
        mHandler.postDelayed(runnable, 5000);
    }

    // TODO もっと簡単に取得する方法ありそう
    private int getPlayerNum(@NonNull QuizPlayer player) {
        int playerNum = 0;
        for (BluetoothDevice device : mPlayerMap.keySet()) {
            if (mPlayerMap.get(device).getId().equals(player.getId())) {
                break;
            }
            playerNum++;
        }
        return playerNum;
    }

    private void returnResult() {
        mState = null;
        ArrayList<QuizPlayer> result = new ArrayList<>();
        // プレイヤー情報抜出
        for (BluetoothDevice device : mPlayerMap.keySet()) {
            if (mPlayerMap.get(device).isConnect()) {
                result.add(mPlayerMap.get(device));
            }
        }
        // 得点順にソート
        Collections.sort(
                result,
                (o1, o2) -> o2.getCorrectAnswerPoint() - o1.getCorrectAnswerPoint());
        mCallback.onResult(result);
        sendAllGuestPlayers(QuizPacket.Creator.result(result));
    }

    private boolean isAnswerFinished() {
        for (BluetoothDevice device : mPlayerMap.keySet()) {
            boolean answerState = mAnswerState.get(mPlayerMap.get(device));
            if ((!answerState) && (mPlayerMap.get(device).isConnect())) {
                return false;
            }
        }
        return true;
    }

    private static String makePlayerId() {
        mPlayerIdCnt++;
        return "Player" + (mPlayerIdCnt);
    }

    private static class CallbackExecutor implements Callback {
        @NonNull
        private final Callback mCallback;
        @NonNull
        private final Handler mHandler;

        CallbackExecutor(@NonNull Callback callback, @NonNull Handler handler) {
            mCallback = callback;
            mHandler = handler;
        }

        @Override
        public void onWaitingStarted(@NonNull QuizPlayer player) {
            mHandler.post(() -> mCallback.onWaitingStarted(player));
        }

        @Override
        public void onPlayerChanged(@NonNull ArrayList<QuizPlayer> playerList) {
            mHandler.post(() -> mCallback.onPlayerChanged(playerList));
        }

        @Override
        public void onQuestion(@NonNull Question question) {
            mHandler.post(() -> mCallback.onQuestion(question));
        }

        @Override
        public void onPlayerAnswered(int playerNum) {
            mHandler.post(() -> mCallback.onPlayerAnswered(playerNum));
        }

        @Override
        public void onCorrectAnswer(boolean isCorrect, @NonNull AnswerChoice answer) {
            mHandler.post(() -> mCallback.onCorrectAnswer(isCorrect, answer));
        }

        @Override
        public void onPlayerDisconnected(int playerNum) {
            mHandler.post(() -> mCallback.onPlayerDisconnected(playerNum));
        }

        @Override
        public void onResult(@NonNull ArrayList<QuizPlayer> result) {
            mHandler.post(() -> mCallback.onResult(result));
        }

        @Override
        public void onStopped(@Nullable QuizException exception) {
            mHandler.post(() -> mCallback.onStopped(exception));
        }
    }
}
