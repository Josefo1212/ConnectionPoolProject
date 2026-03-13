package adapters.postgres;

import dbcomponent.DBTransaction;
import dbcomponent.DBException;
import java.sql.Connection;

public record PostgresSQLTransaction(Connection connection) implements DBTransaction {

    @Override
    public void begin() throws DBException {
        // Implementar begin transaction
    }

    @Override
    public void commit() throws DBException {
        // Implementar commit
    }

    @Override
    public void rollback() throws DBException {
        // Implementar rollback
    }
}

