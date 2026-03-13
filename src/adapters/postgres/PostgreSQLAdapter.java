package adapters.postgres;

import dbcomponent.*;
import pool.PoolManager;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class PostgreSQLAdapter implements IDBComponent<Object> {
    private final PoolManager poolManager;

    public PostgreSQLAdapter() {
        this.poolManager = PoolManager.getInstance();
    }

    private Connection acquire() throws DBException {
        Connection c = poolManager.getConnection();
        if (c == null) {
            throw new DBException(DBException.Category.TIMEOUT, null,
                    "No se pudo obtener una conexión del pool (interrumpido/timeout)");
        }
        return c;
    }

    @Override
    public DBQueryResult<Object> executeQuery(String query) throws DBException {
        Connection c = acquire();
        try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery(query)) {
            // Resultado genérico: devolvemos el ResultSet materializado como List<Object[]> (mínimo viable)
            // Para tipado real, usa la sobrecarga con RowMapper.
            var rows = new ArrayList<Object[]>();
            int cols = rs.getMetaData().getColumnCount();
            while (rs.next()) {
                Object[] row = new Object[cols];
                for (int i = 1; i <= cols; i++) row[i - 1] = rs.getObject(i);
                rows.add(row);
            }
            return new DBQueryResult<>(rows, 0);
        } catch (SQLException e) {
            throw DBException.fromSQLException(e, "executeQuery");
        } finally {
            poolManager.releaseConnection(c);
        }
    }

    @Override
    public <R> DBQueryResult<List<R>> executeQuery(String query, RowMapper<R> mapper) throws DBException {
        if (mapper == null) throw new DBException("RowMapper no puede ser null");

        Connection c = acquire();
        try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery(query)) {
            List<R> out = new ArrayList<>();
            while (rs.next()) {
                out.add(mapper.mapRow(rs));
            }
            return new DBQueryResult<>(out, 0);
        } catch (SQLException e) {
            throw DBException.fromSQLException(e, "executeQuery(mapper)");
        } finally {
            poolManager.releaseConnection(c);
        }
    }

    @Override
    public DBQueryResult<Object> executeUpdate(String query) throws DBException {
        Connection c = acquire();
        try (Statement st = c.createStatement()) {
            int affected = st.executeUpdate(query);
            return new DBQueryResult<>(null, affected);
        } catch (SQLException e) {
            throw DBException.fromSQLException(e, "executeUpdate");
        } finally {
            poolManager.releaseConnection(c);
        }
    }

    @Override
    public DBConnection getConnection() throws DBException {
        return new PostgreSQLConnection(acquire(), poolManager);
    }

    @Override
    public DBTransaction transaction() throws DBException {
        return new PostgresSQLTransaction(acquire(), poolManager);
    }

    @Override
    public DBQueryBatch batch() throws DBException {
        return new PostgreSQLBatch(acquire(), poolManager);
    }

    @Override
    public DBQueryFile queryFiles() throws DBException {
        return new PostgresSQLQueryFile(acquire(), poolManager);
    }

    @Override
    public void close() throws DBException {
        // No-op: el pool vive toda la app. Las conexiones se adquieren/liberan por operación.
    }
}
