package com.example.hori_t;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.example.quiz_lib.Question.AnswerChoice;

import androidx.navigation.NavController;

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
