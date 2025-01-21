import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.SubScene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.InetAddress;
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
 *  Download files from server  Y / N / N
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
public class Main extends Application{

    Stage window;
    RemoteHostMasterThread hostThread;

    private boolean createAddressScene(){
        RemoteHostMasterThread hostThread;
        try
        {
            InetAddress serverAddress = InetAddress.getLocalHost();
            System.out.println(InetAddress.getLocalHost());
            hostThread = new RemoteHostMasterThread(serverAddress, 53529);
        }
        catch (IOException connectionRefused)
        {
            System.out.println("Connection refused, bad address or server is closed");
            Alert errorAlert = new Alert(Alert.AlertType.ERROR);
            errorAlert.setTitle("Test");
            errorAlert.setContentText("Connection refused, bad address or server is closed.");
            errorAlert.showAndWait();
            errorAlert.setOnHidden((event) -> Platform.exit());

            return false;
        }

        ExecutorService exec = Executors.newCachedThreadPool();
        exec.submit(hostThread);
        this.hostThread = hostThread;

        Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
        successAlert.setTitle("Test");
        successAlert.setContentText("Successfully connected.");
        successAlert.showAndWait();

        return true;
    }

    @Override
    public void start(Stage window){
        this.window = window;
        if(createAddressScene()){
            LoginGUI loginScreen = new LoginGUI(this.window, this.hostThread);
            Scene loginScene = new Scene(loginScreen.createLoginScreen());

            this.window.setScene(loginScene);
            this.window.show();
        }


    }

    public static void main(String[] args){
        launch(Main.class);

        /*
        Scanner in = new Scanner(System.in);
        String address;

        // Keep asking the user for server address
        RemoteHostMasterThread hostThread = null;
        do {
            try
            {
                //System.out.print("Connect to: ");
                //address = in.next();

                //InetAddress serverAddress = InetAddress.getByName(address);
                InetAddress serverAddress = InetAddress.getLocalHost();
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

        System.out.println("ok");
         */
    }
}

