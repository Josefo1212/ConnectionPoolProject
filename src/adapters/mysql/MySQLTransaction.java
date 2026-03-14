package adapters.mysql;

import dbcomponent.DBException;
import dbcomponent.DBTransaction;
import pool.PoolManager;

import java.sql.Connection;
import java.sql.SQLException;

public class MySQLTransaction implements DBTransaction {
    private final Connection connection;
    private final PoolManager poolManager;
    private boolean active = false;
    private boolean finished = false;

    public MySQLTransaction(Connection connection, PoolManager poolManager) {
        this.connection = connection;
        this.poolManager = poolManager;
    }

    @Override
    public void begin() throws DBException {
        try {
            if (finished) throw new DBException("La transacción ya se cerró");
            connection.setAutoCommit(false);
            active = true;
        } catch (SQLException e) {
            throw DBException.fromSQLException(e, "transaction.begin");
        }
    }

    @Override
    public void commit() throws DBException {
        try {
            if (finished) throw new DBException("La transacción ya se cerró");
            if (!active) return;
            connection.commit();
            connection.setAutoCommit(true);
            active = false;
            finished = true;
        } catch (SQLException e) {
            throw DBException.fromSQLException(e, "transaction.commit");
        } finally {
            if (finished) poolManager.releaseConnection(connection);
        }
    }

    @Override
    public void rollback() throws DBException {
        try {
            if (finished) return;
            if (active) {
                connection.rollback();
                connection.setAutoCommit(true);
            }
            active = false;
            finished = true;
        } catch (SQLException e) {
            throw DBException.fromSQLException(e, "transaction.rollback");
        } finally {
            if (finished) poolManager.releaseConnection(connection);
        }
    }

    @Override
    public void close() throws DBException {
        if (!finished) rollback();
    }
}

