package com.hori_t.quiz;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.hori_t.quiz_lib.Question.AnswerChoice;

public interface QuizEventListener {
    enum Event {
        FLASH_SHOWED,
        START,
        SELECT_HOST,
        SELECT_GUEST,
        GATHER_MEMBER,
        CONFIRMED_RESULT
    }
    void changeEvent(@NonNull Event event, @Nullable Bundle bundle);
    void answer(@NonNull AnswerChoice choice);
}
