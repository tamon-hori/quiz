package com.hori_t.quiz_lib;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.hori_t.ble_communication_lib.BleDataCommunicationClient;
import com.hori_t.ble_communication_lib.BleDataCommunicationException;
import com.hori_t.quiz_lib.Question.AnswerChoice;
import com.hori_t.quiz_lib.Question.Question;
import com.hori_t.quiz_lib.packet.PayloadKey;
import com.hori_t.quiz_lib.packet.QuizPacket;

import java.util.ArrayList;

public class QuizGuestManager {
    public interface Callback {
        void onJoined(@NonNull QuizPlayer player);
        void onPlayerChanged(@NonNull ArrayList<QuizPlayer> playerList);
        void onQuestion(@NonNull Question question);
        void onPlayerAnswered(int playerNum);
        void onCorrectAnswer(boolean isCorrect, @NonNull AnswerChoice answer);
        void onPlayerDisconnected(int playerNum);
        void onResult(@NonNull ArrayList<QuizPlayer> result);
        void onStopped(@Nullable QuizException exception);
    }

    @NonNull
    private final Handler mHandler;
    @NonNull
    private final CallbackExecutor mCallback;
    @NonNull
    private final BleDataCommunicationClient mClient;
    private boolean isConnecting = false;
    @NonNull
    private final BleDataCommunicationClient.Callback mCommunicationCallback =
            new BleDataCommunicationClient.Callback() {
                @Override
                public void onDiscovered(@NonNull BluetoothDevice device) {
                    Log.d(getClass().getSimpleName(), "Discoverd. " + device.getAddress());
                    if (!isConnecting) {
                        isConnecting = true;
                        mClient.connect(device);
                    }
                }

                @Override
                public void onDiscoveryStopped() {

                }

                @Override
                public void onConnected() {
                    Log.d("クイズLib", "-------------onConnected---------------");
                }

                @Override
                public void onDisconnected() {
                    isConnecting = false;
                    mCallback.onStopped(null);
                }

                @Override
                public void onDataReceived(@NonNull byte[] data) {
                    handlePacket(data);
                }

                @Override
                public void onError(@NonNull BleDataCommunicationException exception) {
                    QuizException quizException = null;
                    if ((BleDataCommunicationException.ErrorCode.Disconnect ==
                            exception.getmErrorCode()) && (!isFinished)) {
                        quizException = new QuizException();
                    }
                    mCallback.onStopped(quizException);
                }
            };
    @Nullable
    private QuizPlayer mMyPlayerInfo;
    @NonNull
    private ArrayList<QuizPlayer> mAllPlayerInfo = new ArrayList<>();
    @Nullable
    private AnswerChoice mAnswerChoice;
    private boolean isFinished = false;

    public QuizGuestManager(
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
        mClient = new BleDataCommunicationClient(
                context, mHandler.getLooper(), mCommunicationCallback);
    }

    public void discoverHost() {
        mHandler.post(mClient::startDiscovery);
    }

    @NonNull
    public QuizPlayer getMyPlayerInfo() {
        return mMyPlayerInfo;
    }

    @NonNull
    public ArrayList<QuizPlayer> getAllPlayerInfo() {
        return mAllPlayerInfo;
    }

    public void answer(@NonNull AnswerChoice answer) {
        mHandler.post(() -> _answer(answer));
    }

    private void _answer(@NonNull AnswerChoice answer) {
        mAnswerChoice = answer;
        mClient.sendData(QuizPacket.Creator.answer(answer).getBytes());
    }

    public void stop() {
        mHandler.post(mClient::disconnect);
    }

    private void handlePacket(@NonNull byte[] data) {
        QuizPacket packet = QuizPacket.Creator.parsePacket(data);
        Log.d("クイズLib", "[receivedPacket] type: " + packet.getType());
        switch (packet.getType()) {
            case PlayerId:
                String playerId = (String) packet.getPayload(PayloadKey.PlayerId);
                mMyPlayerInfo = new QuizPlayer(playerId);
                mCallback.onJoined(mMyPlayerInfo);
                break;
            case PlayerChanged: {
                mAllPlayerInfo =
                        (ArrayList<QuizPlayer>) packet.getPayload(PayloadKey.PlayerList);
                mCallback.onPlayerChanged(mAllPlayerInfo);
                break;
            }
            case Question: {
                String questionText = (String) packet.getPayload(PayloadKey.QuestionText);
                mCallback.onQuestion(new Question(questionText));
                break;
            }
            case PlayerAnswered: {
                int playerNum = (int) packet.getPayload(PayloadKey.PlayerNum);
                mCallback.onPlayerAnswered(playerNum);
                break;
            }
            case CorrectAnswer: {
                AnswerChoice choice = (AnswerChoice) packet.getPayload(PayloadKey.Choice);
                mCallback.onCorrectAnswer(choice == mAnswerChoice, choice);
                break;
            }
            case PlayersState: {
                ArrayList<QuizPlayer> playerList =
                        (ArrayList<QuizPlayer>) packet.getPayload(PayloadKey.PlayerList);
                mAllPlayerInfo = playerList;
                break;
            }
            case PlayerDisconnected: {
                int playerNum = (int) packet.getPayload(PayloadKey.PlayerNum);
                mCallback.onPlayerDisconnected(playerNum);
                break;
            }
            case Result: {
                isFinished = true;
                ArrayList<QuizPlayer> playerList =
                        (ArrayList<QuizPlayer>) packet.getPayload(PayloadKey.PlayerList);
                mCallback.onResult(playerList);
                break;
            }
        }
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
        public void onJoined(@NonNull QuizPlayer player) {
            mHandler.post(() -> mCallback.onJoined(player));
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
