package model;

public enum Color {
    DEEP_BLUE(0), SKY_BLUE(1), LIGHT_BLUE(2), YELLOW(3), ORANGE(4), RED(5);

    public int value;

    Color(int value) {
        this.value = value;
    }

    public static Color[] getColor() {
        return new Color[]{DEEP_BLUE, SKY_BLUE, LIGHT_BLUE, YELLOW, ORANGE, RED};
    }
}
