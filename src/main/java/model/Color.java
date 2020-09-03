package model;

public enum Color {
    RED(0), BROWN(1), GREEN(2), BLUE(3), PINK(4), YELLOW(5);

    private final int value;

    Color(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
