package dbcomponent;
 /*
 * Contrato genérico para ejecutar lotes de queries.
*/
public interface DBQueryBatch {

    void clearBatch() throws DBException;

    DBQueryResult<int[]> executeBatch() throws DBException;

    void addQuery(String query) throws DBException;
}

