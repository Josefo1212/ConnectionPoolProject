package adapters.postgres;

import dbcomponent.DBQueryFile;
import dbcomponent.DBQueryResult;
import dbcomponent.DBException;
import java.sql.Connection;
import java.nio.file.Path;
import java.util.List;

public class PostgresSQLQueryFile implements DBQueryFile {
    private final Connection connection;

    public PostgresSQLQueryFile(Connection connection) {
        this.connection = connection;
    }

    @Override
    public DBQueryResult<?> queryFromFile(Path sqlFile) throws DBException {
        // Implementar ejecución de archivo SQL
        return null;
    }

    @Override
    public List<String> loadQueriesFromFile(Path sqlFile) throws DBException {
        // Implementar carga de queries desde archivo
        return null;
    }
}

