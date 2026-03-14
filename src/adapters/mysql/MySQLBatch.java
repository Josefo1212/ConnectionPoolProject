package adapters.mysql;

import dbcomponent.DBException;
import dbcomponent.DBQueryBatch;
import dbcomponent.DBQueryResult;
import pool.PoolManager;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class MySQLBatch implements DBQueryBatch {
    private final Connection connection;
    private final PoolManager poolManager;
    private final Statement statement;
    private boolean closed = false;

    public MySQLBatch(Connection connection, PoolManager poolManager) throws DBException {
        this.connection = connection;
        this.poolManager = poolManager;
        try {
            this.statement = connection.createStatement();
        } catch (SQLException e) {
            poolManager.releaseConnection(connection);
            throw DBException.fromSQLException(e, "batch.createStatement");
        }
    }

    @Override
    public void addQuery(String query) throws DBException {
        if (closed) throw new DBException("Batch ya está cerrado");
        try {
            statement.addBatch(query);
        } catch (SQLException e) {
            throw DBException.fromSQLException(e, "batch.addQuery");
        }
    }

    @Override
    public DBQueryResult<int[]> executeBatch() throws DBException {
        if (closed) throw new DBException("Batch ya está cerrado");
        try {
            int[] result = statement.executeBatch();
            return new DBQueryResult<>(result, 0);
        } catch (SQLException e) {
            throw DBException.fromSQLException(e, "batch.executeBatch");
        }
    }

    @Override
    public void clearBatch() throws DBException {
        if (closed) return;
        try {
            statement.clearBatch();
        } catch (SQLException e) {
            throw DBException.fromSQLException(e, "batch.clearBatch");
        } finally {
            closeInternal();
        }
    }

    private void closeInternal() {
        if (closed) return;
        closed = true;
        try {
            statement.close();
        } catch (Exception ignored) {
        } finally {
            poolManager.releaseConnection(connection);
        }
    }
}

