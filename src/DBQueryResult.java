public class DBQueryResult {
    private Object result;
    private int affectedRows;

    public DBQueryResult(Object result, int affectedRows) {
        this.result = result;
        this.affectedRows = affectedRows;
    }

    public Object getResult() {
        return result;
    }

    public int getAffectedRows() {
        return affectedRows;
    }
}

