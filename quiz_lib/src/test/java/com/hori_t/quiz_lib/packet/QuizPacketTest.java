package com.hori_t.quiz_lib.packet;

import com.hori_t.quiz_lib.QuizPlayer;

import org.junit.Assert;
import org.junit.Test;

import java.util.LinkedList;

public class QuizPacketTest {
    @Test
    public void playerId() {
        QuizPacket packet1 = QuizPacket.Creator.playerId("Player0");
        byte[] byte1 = packet1.getBytes();
        QuizPacket packet2 = QuizPacket.Creator.parsePacket(byte1);

        String str1 = (String) packet1.getPayload(PayloadKey.PlayerId);
        String str2 = (String) packet2.getPayload(PayloadKey.PlayerId);
        Assert.assertTrue(str1.equals(str2));
        Assert.assertTrue(packet1.getType() == packet2.getType());
    }

    @Test
    public void playerChanged() {
        LinkedList<QuizPlayer> playerList = new LinkedList<>();
        playerList.add(new QuizPlayer("Player0"));
        playerList.add(new QuizPlayer("Player1"));
        QuizPacket packet1 = QuizPacket.Creator.playerChanged(playerList);
        byte[] byte1 = packet1.getBytes();
        QuizPacket packet2 = QuizPacket.Creator.parsePacket(byte1);

        LinkedList<QuizPlayer> listA = (LinkedList<QuizPlayer>) packet1.getPayload(PayloadKey.PlayerList);
        LinkedList<QuizPlayer> listB = (LinkedList<QuizPlayer>) packet2.getPayload(PayloadKey.PlayerList);

        Assert.assertTrue(packet1.getType() == packet2.getType());

        QuizPlayer playerA0 = listA.get(0);
        QuizPlayer playerB0 = listB.get(0);
        Assert.assertTrue(playerA0.getId().equals(playerB0.getId()));

        QuizPlayer playerA1 = listA.get(1);
        QuizPlayer playerB1 = listB.get(1);
        Assert.assertTrue(playerA1.getId().equals(playerB1.getId()));
    }

}