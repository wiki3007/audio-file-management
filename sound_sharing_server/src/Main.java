import java.io.IOException;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Function Checkboxes, server-side
 * Code implemented / UI implemented (does the server even need UI?) / Tested
 *
 * Guest:
 *  Get public files            Y / N / N
 *  Get public lists            Y / N / N
 *  Make temporary lists        N / N / N   (client only)
 *  Listen to files on server   N / N / N   (client only)
 *
 * Standard:
 *  Registering account         Y / N / N
 *  Log in to server            Y / N / N
 *  Get public files            Y / N / N
 *  Get private files           Y / N / N
 *  Upload public files         N / N / N
 *  Upload private files        N / N / N
 *  Delete private files        N / N / N
 *  Share private files         N / N / N
 *  Get public lists            Y / N / N
 *  Get private lists           Y / N / N
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