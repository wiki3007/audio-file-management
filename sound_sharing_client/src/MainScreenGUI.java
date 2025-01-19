import javafx.scene.Parent;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

public class MainScreenGUI {
    Stage window;
    RemoteHostMasterThread host;

    MainScreenGUI(Stage window, RemoteHostMasterThread host){
        this.window = window;
        this.host = host;
    }

    Parent createMainScreen(String type){
        return switch (type) {
            case "guest" -> this.createGuestScreen();
            case "standard" -> this.createStandardScreen();
            case "admin" -> this.createAdminScreen();
            default -> null;
        };

    }

    private Parent createGuestScreen(){
        GridPane layout = new GridPane();
        return layout;
    }

    private Parent createStandardScreen(){
        GridPane layout = new GridPane();
        return layout;
    }

    private Parent createAdminScreen(){
        GridPane layout = new GridPane();
        return layout;
    }
}
