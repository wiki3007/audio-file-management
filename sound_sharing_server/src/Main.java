import java.io.IOException;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Function Checkboxes, client-side
 * Code implemented / UI implemented (does the server even need UI?) / Tested
 *
 * Guest:
 *  Get public files            N / N / N
 *  Get public lists            N / N / N
 *  Make temporary lists        N / N / N
 *  Listen to files on server   N / N / N
 *
 * Standard:
 *  Registering account         N / N / N
 *  Log in to server            N / N / N
 *  Get public files            N / N / N
 *  Get private files           N / N / N
 *  Upload public files         N / N / N
 *  Upload private files        N / N / N
 *  Delete private files        N / N / N
 *  Share private files         N / N / N
 *  Get public lists            N / N / N
 *  Create public lists         N / N / N
 *  Create private lists        N / N / N
 *  Delete private lists        N / N / N
 *  Share private lists         N / N / N
 *  Listen to files on server   N / N / N
 *  Download files from server  N / N / N
 *
 * Admin:
 *  All things from standard    N / N / N
 *  Add public files as server  N / N / N
 *  Browse other users files    N / N / N
 *  Delete other users files    N / N / N
 *  Add public lists as server  N / N / N
 *  Browse other users lists    N / N / N
 *  Delete other users lists    N / N / N
 *  Change other's account type N / N / N
 */
public class Main {
    public static void main(String[] args) throws SQLException, IOException, ClassNotFoundException {
        ServerThread serverThread = new ServerThread();
        ExecutorService exec = Executors.newCachedThreadPool();
        exec.submit(serverThread);
    }
}