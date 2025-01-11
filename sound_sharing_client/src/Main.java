import java.io.BufferedWriter;
import java.io.IOException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Function Checkboxes, client-side
 * Code implemented / UI implemented / Tested
 *
 * Guest:
 *  Get public files            Y / N / N
 *  Get public lists            Y / N / N
 *  Make temporary lists        Y / N / N
 *  Listen to files on server   N / N / N
 *
 * Standard:
 *  Registering account         Y / N / N
 *  Log in to server            Y / N / N
 *  Get public files            Y / N / N
 *  Get private files           Y / N / N
 *  Upload public files         Y / N / N
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
 *  Listen to files on server   N / N / N
 *  Download files from server  N / N / N
 *
 * Admin:
 *  Add public files as server  Y / N / N
 *  Browse other users files    Y / N / N
 *  Delete other users files    Y / N / N
 *  Add public lists as server  Y / N / N
 *  Browse other users lists    Y / N / N
 *  Delete other users lists    Y / N / N
 *  Change other's account type Y / N / N
 */
public class Main {
    public static void main(String[] args) throws IOException {
        Scanner in = new Scanner(System.in);
        String address;

        // Keep asking the user for server address
        RemoteHostMasterThread hostThread = null;
        do {
            try
            {
                System.out.print("Connect to: ");
                address = in.next();

                InetAddress serverAddress = InetAddress.getByName(address);
                //System.out.println(serverAddress);
                hostThread = new RemoteHostMasterThread(serverAddress, 53529);
                break;
            }
            catch (ConnectException connectionRefused)
            {
                System.out.println("Connection refused, bad address or server is closed");
            }
        } while(true);
        ExecutorService exec = Executors.newCachedThreadPool();
        exec.submit(hostThread);
    }
}

