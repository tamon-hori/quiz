package com.example.quiz_lib;

import android.support.annotation.NonNull;

import com.example.quiz_lib.Question.AnswerChoice;
import com.example.quiz_lib.Question.Question;

class HostQuestion extends Question {
    @NonNull
    private final AnswerChoice mCorrectAnswer;

    HostQuestion(@NonNull String questionText, @NonNull AnswerChoice correctAnswer) {
        super(questionText);
        mCorrectAnswer = correctAnswer;
    }

    @NonNull
    AnswerChoice getCorrectAnswer() {
        return mCorrectAnswer;
    }

    boolean isCorrect(@NonNull AnswerChoice answer) {
        return mCorrectAnswer == answer;
    }
}
