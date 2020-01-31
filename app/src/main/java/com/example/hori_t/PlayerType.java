package com.example.hori_t;

enum PlayerType {
    Host(0),
    Guest(1);

    private final int mType;

    PlayerType(final int type) {
        mType = type;
    }

    int getInt() {
        return mType;
    }

    public static PlayerType getType(final int value) throws IllegalArgumentException {
        for (PlayerType type : PlayerType.values()) {
            if (type.getInt() == value) {
                return type;
            }
        }
        throw new IllegalArgumentException("Bad value.");
    }
}
