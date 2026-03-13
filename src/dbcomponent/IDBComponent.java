package dbcomponent;

import java.util.List;

public interface IDBComponent<T> {
    // Ejecuta un SELECT (u otra consulta que devuelva datos) y retorna un resultado tipado.
    DBQueryResult<T> executeQuery(String query) throws DBException;

    // Ejecuta INSERT/UPDATE/DELETE (u otra operación de escritura).
    DBQueryResult<T> executeUpdate(String query) throws DBException;

    /**
     * Ejecuta un SELECT y mapea cada fila a un tipo R.
     */
    <R> DBQueryResult<List<R>> executeQuery(String query, RowMapper<R> mapper) throws DBException;

    DBConnection getConnection() throws DBException;

    /**
     * Inicia/obtiene un controlador de transacciones.
     */
    DBTransaction transaction() throws DBException;

    /**
     * Inicia/obtiene un batch de queries.
     */
    DBQueryBatch batch() throws DBException;

    /**
     * Funcionalidad para cargar/ejecutar queries desde archivos .sql.
     */
    DBQueryFile queryFiles() throws DBException;

    void close() throws DBException;
}
