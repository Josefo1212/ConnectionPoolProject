public interface DBConnection {

    boolean isConnected();

    void disconnect() throws DBException;

    void connect() throws DBException;
}