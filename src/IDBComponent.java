public interface IDBComponent {
    DBQueryResult executeQuery(String query) throws DBException;
    DBQueryResult executeUpdate(String query) throws DBException;
    DBConnection getConnection() throws DBException;
    void close() throws DBException;
}

