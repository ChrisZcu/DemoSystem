package model;

public final class TrajToSubpart {
    private final int trajId;     // the id of the ORIGIN traj
    private final int beginPosIdx, endPosIdx;     // the sub part we selected

    public TrajToSubpart(int trajId, int beginPosIdx, int endPosIdx) {
        this.trajId = trajId;
        this.beginPosIdx = beginPosIdx;
        this.endPosIdx = endPosIdx;
    }

    public int getTrajId() {
        return trajId;
    }

    public int getBeginPosIdx() {
        return beginPosIdx;
    }

    public int getEndPosIdx() {
        return endPosIdx;
    }

    @Override
    public String toString() {
        return "TrajToSubpart{" +
                "trajId=" + trajId +
                ", beginPosIdx=" + beginPosIdx +
                ", endPosIdx=" + endPosIdx +
                '}';
    }
}
