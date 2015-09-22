package jsprit.core.problem.misc;

public class DestinationBaseContext {

    private int runNum;
    
    private int insertionIndex;

    public DestinationBaseContext() {
        runNum = 0;
        insertionIndex = 0;
    }
    
    public int getRunNum() {
        return runNum;
    }
    
    public int getInsertionIndex() {
        return insertionIndex;
    }

    public void incrementRunNum() {
        runNum++;
    }
    
    public void incrementInsertionIndex() {
        insertionIndex++;
    }
    
}
