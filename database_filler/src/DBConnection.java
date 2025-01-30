import java.sql.*;

/**
 * Class that facilitates database connectivity
 */
public class DBConnection {
    /**
     * URL to the database, just assume it runs local to the server on XAMPP, IDC
     */
    private String url = "jdbc:Mysql://127.0.0.1:3306";
    /**
     * Database login
     */
    private String login = "root";
    /**
     * Database password
     */
    private String password = ""; // people cannot steal your plaintext if you just so no. What are criminals going to do? Break the law? That's illegal.
    /**
     * Database name
     */
    private String dbname = "sound_sharing";
    /**
     * Database connector
     */
    private Connection connection = DriverManager.getConnection(url + "/" + dbname, login, password);
    /**
     * Main database thingy to use commands from
     */
    public Statement statement = connection.createStatement();
    /**
     * Second main database command thingy
     */
    PreparedStatement preparedStatement;

    public DBConnection() throws SQLException {
    }

    public String getUrl() {
        return url;
    }

    public String getLogin() {
        return login;
    }

    public String getPassword() {
        return password;
    }

    public String getDbname() {
        return dbname;
    }

    /**
     * Executes command on database
     * @param command Command to execute
     * @return Integer that gets returned as result of statement.executeUpdate, or -1 if error occurs
     * @throws SQLException If database connection breaks
     */
    public int execUpdate(String command) throws SQLException {
        Statement statement = connection.createStatement();
        try
        {
            int returnCode = statement.executeUpdate(command);
            //System.out.println(returnCode);
            return returnCode;
        }
        catch (SQLException sqlException)
        {
            System.out.println("Command \"" + command + "\" failed\t" + sqlException.getMessage() + ": " + sqlException.getErrorCode());
        }
        finally {
            statement.close();
        }
        return -1;
    }

    /**
     * Executes INSERT, UPDATE or DELETE query on the database
     * @param command Command to execute
     * @return ResultSet that gets returned as a result of statement.executeQuery
     * @throws SQLException If database connection breaks
     */
    public ResultSet execQuery(String command) throws SQLException {
        Statement statement = connection.createStatement();
        try
        {
            ResultSet returnSet = statement.executeQuery(command);
            //System.out.println(returnCode);
            return returnSet;
        }
        catch (SQLException sqlException)
        {
            System.out.println("Command \"" + command + "\" failed\t" + sqlException.getMessage() + ": " + sqlException.getErrorCode());
        }
        finally {
            statement.close();
        }
        return statement.executeQuery("");
    }

    public ResultSet searchDatabase(String command) throws SQLException {
        preparedStatement = connection.prepareStatement(command);
        try
        {
            ResultSet returnSet = preparedStatement.executeQuery();
            return returnSet;
        }
        catch (SQLException sqlException)
        {
            System.out.println("Command \"" + command + "\" failed\t" + sqlException.getMessage() + ": " + sqlException.getErrorCode());
        }
        finally {
            //preparedStatement.close();
        }
        return (preparedStatement = connection.prepareStatement("")).executeQuery();
    }
}
