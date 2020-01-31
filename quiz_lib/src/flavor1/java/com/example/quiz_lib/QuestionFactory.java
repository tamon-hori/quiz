package com.example.quiz_lib;

import android.support.annotation.NonNull;

import com.example.quiz_lib.Question.AnswerChoice;

import java.util.Collections;
import java.util.LinkedList;

public class QuestionFactory {
    @NonNull
    private final LinkedList<HostQuestion> mQuestionList = new LinkedList<>();

    QuestionFactory() {
        mQuestionList.add(new HostQuestion(
                "今年厄年の堀多聞さん。正月に起きた大事件とは…!?\n\nA:交通事故\nB:インフルエンザ\nC:スマホ紛失\nD:骨折", AnswerChoice.A));
        mQuestionList.add(new HostQuestion(
                "神奈川県の県庁所在地は？\n\nA:川崎市\nB:鎌倉市\nC:町田市\nD:横浜市", AnswerChoice.D));
        mQuestionList.add(new HostQuestion(
                "阿久津さんの誕生日は?\n\nA:2月3日\nB:2月4日\nC:2月5日\nD:2月6日", AnswerChoice.B));
        mQuestionList.add(new HostQuestion(
                "日本の県庁所在地の中で唯一ひらがなが使われている県は？\n\nA:埼玉県\nB:栃木県\nC:茨城県\nD:福島県", AnswerChoice.A));
        mQuestionList.add(new HostQuestion(
                "佐々木さんが2018年度に落とした体重は何kg？\n\nA:4kg\nB:8kg\nC:12kg\nD:16kg", AnswerChoice.D));
        mQuestionList.add(new HostQuestion(
                "徹さんの髪の色は？\n\nA:ピンク\nB:緑\nC:青\nD:茶色", AnswerChoice.D));
        mQuestionList.add(new HostQuestion(
                "Sky株式会社の設立当時の会社名は？\n\nA:イン・ザ・スカイ\nB:スカイ・シンク・システム\nC:スイカ\nD:スカイ・システム・エンジニアリング", AnswerChoice.B));
        Collections.shuffle(mQuestionList);
    }
    @NonNull
    public HostQuestion getQuestion() {
        return mQuestionList.poll();
    }
}
