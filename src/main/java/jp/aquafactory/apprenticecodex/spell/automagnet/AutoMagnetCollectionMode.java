package jp.aquafactory.apprenticecodex.spell.automagnet;

public enum AutoMagnetCollectionMode {
    NORMAL(0),
    REVERSE(1);

    private final int id;

    AutoMagnetCollectionMode(int id) {
        this.id = id;
    }

    public int id() {
        return id;
    }

    public boolean canCollect(boolean crouching) {
        return (this == REVERSE) == crouching;
    }

    public static AutoMagnetCollectionMode fromCrouching(boolean crouching) {
        return crouching ? REVERSE : NORMAL;
    }

    public static AutoMagnetCollectionMode byId(int id) {
        for (var mode : values()) {
            if (mode.id == id) {
                return mode;
            }
        }
        return NORMAL;
    }

    public static AutoMagnetCollectionMode byName(String name) {
        try {
            return valueOf(name);
        } catch (IllegalArgumentException exception) {
            return NORMAL;
        }
    }
}
