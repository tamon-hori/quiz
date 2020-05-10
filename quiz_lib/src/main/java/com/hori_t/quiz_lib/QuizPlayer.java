package com.hori_t.quiz_lib;

import android.support.annotation.NonNull;

import java.io.Serializable;

public class QuizPlayer implements Serializable {
    @NonNull
    private final String mId;
    private int mCorrectAnswerPoint;
    private boolean isMe;   // TODO 暫定処理 App層で設定
    private boolean isConnect = true;

    public QuizPlayer(@NonNull String playerId) {
        mId = playerId;
    }

    public QuizPlayer(@NonNull String playerId, int correctAnswerPoint) {
        mId = playerId;
        mCorrectAnswerPoint = correctAnswerPoint;
    }

    @NonNull
    public String getId() {
        return mId;
    }

    public void addPoint() {
        mCorrectAnswerPoint++;
    }

    public int getCorrectAnswerPoint() {
        return mCorrectAnswerPoint;
    }

    public boolean isMe() {
        return isMe;
    }

    public void setMe() {
        isMe = true;
    }

    public void disconnect() {
        isConnect = false;
    }

    public boolean isConnect() {
        return isConnect;
    }
}
