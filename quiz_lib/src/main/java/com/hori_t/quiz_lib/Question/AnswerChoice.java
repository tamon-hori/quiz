package com.hori_t.quiz_lib.Question;

public enum AnswerChoice {
    Timeout((byte) 0x00),
    A((byte) 0x01),
    B((byte) 0x02),
    C((byte) 0x03),
    D((byte) 0x04),
    ;

    private final byte mChoice;

    AnswerChoice(final byte choice) {
        this.mChoice = choice;
    }

    public byte getByte() {
        return mChoice;
    }

    public static AnswerChoice getChoice(final byte value) throws IllegalArgumentException {
        for (AnswerChoice choice : AnswerChoice.values()) {
            if (choice.getByte() == value) {
                return choice;
            }
        }
        throw new IllegalArgumentException("Bad value.");
    }

    @Override
    public String toString() {
        return super.toString();
    }
}
