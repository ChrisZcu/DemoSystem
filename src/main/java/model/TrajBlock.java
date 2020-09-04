package model;

/**
 * The traj structured block which used to s
 */
public final class TrajBlock {
    private BlockType blockType; // full, random, VFGS
    private Trajectory[] trajList;    // all traj given to this block
    private int threadNum;
    private int dIdx, rIdx;           // the param for select color

    public TrajBlock() {
        blockType = BlockType.NONE;
        trajList = null;
    }

    public void setNewBlock(BlockType blockType, Trajectory[] trajList,
                            int threadNum, int dIdx, int rIdx) {
        this.blockType = blockType;
        this.trajList = trajList;
        this.threadNum = threadNum;
        this.dIdx = dIdx;
        this.rIdx = rIdx;
    }

    public String getBlockInfoStr(int[] deltaList, double[] rateList) {
        String info = "";
        info += "type=" + blockType;
        // notice that there is no break
        switch (blockType) {
            case VFGS:
                info += " delta=" + deltaList[dIdx];
            case RAND:
                info += " rate=" + rateList[rIdx];
            case FULL:
                // do nothing
        }
        return info;
    }

    public BlockType getBlockType() {
        return blockType;
    }

    public Trajectory[] getTrajList() {
        return trajList;
    }

    public int getThreadNum() {
        return threadNum;
    }

    public int getDIdx() {
        return dIdx;
    }

    public int getRIdx() {
        return rIdx;
    }
}
