package model;

public enum BlockType {
    FULL(0), RANDOM(1), VFGS(2);
    int value;

    BlockType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
