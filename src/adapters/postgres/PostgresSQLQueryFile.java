package adapters.postgres;

import dbcomponent.DBException;
import dbcomponent.DBQueryFile;
import dbcomponent.DBQueryResult;
import pool.PoolManager;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class PostgresSQLQueryFile implements DBQueryFile {
    private final Connection connection;
    private final PoolManager poolManager;

    public PostgresSQLQueryFile(Connection connection, PoolManager poolManager) {
        this.connection = connection;
        this.poolManager = poolManager;
    }

    @Override
    public DBQueryResult<?> queryFromFile(Path sqlFile) throws DBException {
        List<String> queries = loadQueriesFromFile(sqlFile);

        int totalAffected = 0;
        int executed = 0;

        try (Statement st = connection.createStatement()) {
            for (String q : queries) {
                if (q == null || q.isBlank()) continue;
                boolean hasResultSet = st.execute(q);
                if (!hasResultSet) {
                    totalAffected += st.getUpdateCount();
                }
                executed++;
            }
            return new DBQueryResult<>("Ejecutadas " + executed + " sentencias", totalAffected);
        } catch (SQLException e) {
            throw DBException.fromSQLException(e, "queryFromFile: " + sqlFile);
        } finally {
            poolManager.releaseConnection(connection);
        }
    }

    @Override
    public List<String> loadQueriesFromFile(Path sqlFile) throws DBException {
        try {
            String raw = Files.readString(sqlFile, StandardCharsets.UTF_8);
            // Quitar comentarios de línea simples -- ...
            StringBuilder sb = new StringBuilder();
            for (String line : raw.split("\\R")) {
                String trimmed = line.trim();
                if (trimmed.startsWith("--")) continue;
                sb.append(line).append('\n');
            }
            String noLineComments = sb.toString();

            String[] parts = noLineComments.split(";");
            List<String> out = new ArrayList<>();
            for (String p : parts) {
                String q = p.trim();
                if (!q.isEmpty()) out.add(q);
            }
            return out;
        } catch (IOException e) {
            throw new DBException(DBException.Category.IO, null,
                    "Error leyendo archivo SQL: " + sqlFile, e);
        }
    }
}
