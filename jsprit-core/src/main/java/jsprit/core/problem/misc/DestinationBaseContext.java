package jsprit.core.problem.misc;

public class DestinationBaseContext {

    private int runNum;

    public DestinationBaseContext() {
        runNum = 0;
    }
    
    public int getRunNum() {
        return runNum;
    }

    public void incrementRunNum() {
        runNum++;
    }
    
}
