package com.hori_t.quiz_lib.packet;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.hori_t.quiz_lib.Constants;
import com.hori_t.quiz_lib.Question.AnswerChoice;
import com.hori_t.quiz_lib.QuizPlayer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class QuizPacket {
    @NonNull
    private final PacketType mType;
    @NonNull
    private final HashMap<PayloadKey, Object> mPayloads = new HashMap<>();

    private static final int TYPE_OFFSET = 0;
    private static final int SIZE_OF_TYPE = 1;
    private static final int SIZE_OF_ANSWER_POINT = 1;

    private QuizPacket(@NonNull PacketType type) {
        mType = type;
    }

    public static class Creator {
        @NonNull
        public static QuizPacket parsePacket(@NonNull byte[] data)
                throws IllegalArgumentException {
            PacketType type = PacketType.getType(data[TYPE_OFFSET]);
            QuizPacket packet = new QuizPacket(type);
            switch (type) {
                case PlayerId: {
                    if ((SIZE_OF_TYPE + Constants.PLAYER_ID_LEN) != data.length) {
                        throw new IllegalArgumentException("Invalid num of characters");
                    }
                    String id = new String(
                            Arrays.copyOfRange(data, TYPE_OFFSET + 1 , data.length));
                    packet.mPayloads.put(PayloadKey.PlayerId, id);
                    break;
                }
                case PlayerChanged: {
                    // プレイヤー数算出
                    int playerNum = (data.length - SIZE_OF_TYPE) / Constants.PLAYER_ID_LEN;
                    ArrayList<QuizPlayer> playerList = new ArrayList<>();
                    for (int cnt = 0; playerNum > cnt; cnt++) {
                        int from = (TYPE_OFFSET + 1) + (Constants.PLAYER_ID_LEN * cnt);
                        byte[] idByte = Arrays.copyOfRange(
                                data, from, from + Constants.PLAYER_ID_LEN);
                        playerList.add(new QuizPlayer(new String(idByte)));
                    }
                    packet.mPayloads.put(PayloadKey.PlayerList, playerList);
                    break;
                }
                case Question: {
                    String questionText = new String(
                            Arrays.copyOfRange(data, TYPE_OFFSET + 1 , data.length));
                    packet.mPayloads.put(PayloadKey.QuestionText, questionText);
                    break;
                }
                case Answer:
                case CorrectAnswer: {
                    AnswerChoice choice = AnswerChoice.getChoice(data[TYPE_OFFSET + 1]);
                    packet.mPayloads.put(PayloadKey.Choice, choice);
                    break;
                }
                case PlayerAnswered:
                case PlayerDisconnected: {
                    packet.mPayloads.put(PayloadKey.PlayerNum, (int) data[TYPE_OFFSET + 1]);
                    break;
                }
                case PlayersState:
                case Result: {
                    // プレイヤー数算出
                    int sizeOfPlayer = (Constants.PLAYER_ID_LEN + SIZE_OF_ANSWER_POINT);
                    int playerNum = (data.length - SIZE_OF_TYPE) / sizeOfPlayer;
                    ArrayList<QuizPlayer> playerList = new ArrayList<>();
                    for (int cnt = 0; playerNum > cnt; cnt++) {
                        int from = (SIZE_OF_TYPE) + (sizeOfPlayer * cnt);
                        // ID抜出
                        byte[] idByte = Arrays.copyOfRange(
                                data, from, from + Constants.PLAYER_ID_LEN);
                        byte correctPoint =
                                data[from + Constants.PLAYER_ID_LEN];
                        // リストにプレイヤー追加
                        playerList.add(new QuizPlayer(new String(idByte), correctPoint));
                    }
                    packet.mPayloads.put(PayloadKey.PlayerList, playerList);
                    break;
                }
            }
            return packet;
        }

        @NonNull
        public static QuizPacket playerId(@NonNull String playerId)
                throws IllegalArgumentException {
            if (playerId.length() != Constants.PLAYER_ID_LEN) {
                throw new IllegalArgumentException("Invalid num of characters");
            }
            QuizPacket packet = new QuizPacket(PacketType.PlayerId);
            packet.mPayloads.put(PayloadKey.PlayerId, playerId);
            return packet;
        }

        @NonNull
        public static QuizPacket playerChanged(@NonNull ArrayList<QuizPlayer> playerList) {
            QuizPacket packet = new QuizPacket(PacketType.PlayerChanged);
            packet.mPayloads.put(PayloadKey.PlayerList, playerList);
            return packet;
        }

        @NonNull
        public static QuizPacket question(@NonNull String questionText) {
            QuizPacket packet = new QuizPacket(PacketType.Question);
            packet.mPayloads.put(PayloadKey.QuestionText, questionText);
            return packet;
        }

        @NonNull
        public static QuizPacket answer(@NonNull AnswerChoice choice) {
            QuizPacket packet = new QuizPacket(PacketType.Answer);
            packet.mPayloads.put(PayloadKey.Choice, choice);
            return packet;
        }

        @NonNull
        public static QuizPacket playerAnswered(int playerNum) {
            QuizPacket packet = new QuizPacket(PacketType.PlayerAnswered);
            packet.mPayloads.put(PayloadKey.PlayerNum, playerNum);
            return packet;
        }

        @NonNull
        public static QuizPacket correctAnswer(@NonNull AnswerChoice choice) {
            QuizPacket packet = new QuizPacket(PacketType.CorrectAnswer);
            packet.mPayloads.put(PayloadKey.Choice, choice);
            return packet;
        }

        @NonNull
        public static QuizPacket playersState(@NonNull ArrayList<QuizPlayer> playerList) {
            QuizPacket packet = new QuizPacket(PacketType.PlayersState);
            packet.mPayloads.put(PayloadKey.PlayerList, playerList);
            return packet;
        }

        @NonNull
        public static QuizPacket playerDisconnected(int playerNum) {
            QuizPacket packet = new QuizPacket(PacketType.PlayerDisconnected);
            packet.mPayloads.put(PayloadKey.PlayerNum, playerNum);
            return packet;
        }

        @NonNull
        public static QuizPacket result(@NonNull ArrayList<QuizPlayer> playerList) {
            QuizPacket packet = new QuizPacket(PacketType.Result);
            packet.mPayloads.put(PayloadKey.PlayerList, playerList);
            return packet;
        }
    }

    @NonNull
    public byte[] getBytes() {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        stream.write(mType.getByte());
        try {
            switch (mType) {
                case PlayerId: {
                    String id = (String) mPayloads.get(PayloadKey.PlayerId);
                    stream.write(id.getBytes());
                    break;
                }
                case PlayerChanged: {
                    ArrayList playerList = (ArrayList) mPayloads.get(PayloadKey.PlayerList);
                    for (Object player : playerList) {
                        QuizPlayer quizPlayer = (QuizPlayer) player;
                        stream.write(quizPlayer.getId().getBytes());
                    }
                    break;
                }
                case Question: {
                    String questionText = (String) mPayloads.get(PayloadKey.QuestionText);
                    stream.write(questionText.getBytes());
                    break;
                }
                case Answer:
                case CorrectAnswer: {
                    AnswerChoice choice = (AnswerChoice) mPayloads.get(PayloadKey.Choice);
                    stream.write(choice.getByte());
                    break;
                }
                case PlayerAnswered:
                case PlayerDisconnected: {
                    stream.write((int) mPayloads.get(PayloadKey.PlayerNum));
                    break;
                }
                case PlayersState:
                case Result: {
                    ArrayList playerList = (ArrayList) mPayloads.get(PayloadKey.PlayerList);
                    for (Object player : playerList) {
                        QuizPlayer quizPlayer = (QuizPlayer) player;
                        stream.write(quizPlayer.getId().getBytes());
                        stream.write((byte) quizPlayer.getCorrectAnswerPoint());
                    }
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return stream.toByteArray();
    }

    @NonNull
    public PacketType getType() {
        return mType;
    }

    @NonNull
    public HashMap getPayloads() {
        return mPayloads;
    }

    @Nullable
    public Object getPayload(PayloadKey key) {
        return mPayloads.get(key);
    }
}
