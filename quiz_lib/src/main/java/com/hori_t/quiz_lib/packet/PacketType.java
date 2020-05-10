package com.hori_t.quiz_lib.packet;

public enum PacketType {
    PlayerId((byte) 0x01),
    PlayerChanged((byte) 0x02),
    Question((byte) 0x03),
    PlayerAnswered((byte) 0x04),
    Answer((byte) 0x05),
    CorrectAnswer((byte) 0x06),
    PlayersState((byte) 0x07),
    PlayerDisconnected((byte) 0x08),
    Result((byte) 0x09)
    ;

    private final byte mType;

    PacketType(final byte type) {
        this.mType = type;
    }

    public byte getByte() {
        return mType;
    }

    public static PacketType getType(final byte value) throws IllegalArgumentException {
        for (PacketType type : PacketType.values()) {
            if (type.getByte() == value) {
                return type;
            }
        }
        throw new IllegalArgumentException("Bad value.");
    }

}
