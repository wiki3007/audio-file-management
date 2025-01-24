import java.io.IOException;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// add actually sending the files to register file functions
// add actually deleting the files to delete file function

/**
 * Function Checkboxes, server-side
 * Code implemented / UI implemented (does the server even need UI?) / Tested
 *
 * Guest:
 *  Get public files            Y / N / N
 *  Get public lists            Y / N / N
 *
 * Standard:
 *  Registering account         Y / Y / Y
 *  Log in to server            Y / Y / Y
 *  Get public files            Y / Y / Y
 *  Get private files           Y / N / N
 *  Upload public files         Y / Y / Y
 *  Upload private files        Y / N / N
 *  Delete private files        Y / N / N
 *  Share private files         Y / N / N
 *  Add file to list            Y / N / N
 *  Remove file from list       Y / N / N
 *  Unshare file from user      Y / N / N
 *  Unshare file from all       Y / N / N
 *  Get public lists            Y / N / N
 *  Get private lists           Y / N / N
 *  Create public lists         Y / N / N
 *  Create private lists        Y / N / N
 *  Delete private lists        Y / N / N
 *  Share private lists         Y / N / N
 *  Listen to files on server   Y / Y / Y (but file isn't deleted properly)
 *  Download files from server  Y / Y / Y
 *
 * Admin:
 *  All things from standard    Y / N / N
 *  Add public files as server  Y / N / N
 *  Browse other users files    Y / N / N
 *  Delete other users files    Y / N / N
 *  Add public lists as server  Y / N / N
 *  Browse other users lists    Y / N / N
 *  Delete other users lists    Y / N / N
 *  Change other's account type Y / N / N
 */
public class Main {
    public static void main(String[] args) throws SQLException, IOException, ClassNotFoundException {
        ServerThread serverThread = new ServerThread();
        ExecutorService exec = Executors.newCachedThreadPool();
        exec.submit(serverThread);
    }
}