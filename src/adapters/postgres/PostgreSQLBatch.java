package adapters.postgres;

import dbcomponent.DBQueryBatch;
import dbcomponent.DBQueryResult;
import dbcomponent.DBException;
import java.sql.Connection;

public class PostgreSQLBatch implements DBQueryBatch {
    private final Connection connection;

    public PostgreSQLBatch(Connection connection) {
        this.connection = connection;
    }

    @Override
    public void addQuery(String query) throws DBException {
        // Implementar addQuery
    }

    @Override
    public DBQueryResult<int[]> executeBatch() throws DBException {
        // Implementar executeBatch
        return null;
    }

    @Override
    public void clearBatch() throws DBException {
        // Implementar clearBatch
    }
}

