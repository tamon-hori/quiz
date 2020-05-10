package com.hori_t.quiz_lib.Question;

import android.support.annotation.NonNull;

public class Question {
    @NonNull
    private final String mQuestionText;

    public Question(@NonNull String questionText) {
        mQuestionText = questionText;
    }

    public String getQuestionText() {
        return mQuestionText;
    }
}
