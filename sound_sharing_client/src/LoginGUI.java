import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class LoginGUI {
    Stage window;
    RemoteHostMasterThread host;

    LoginGUI(Stage window, RemoteHostMasterThread host){
        this.window = window;
        this.host = host;
    }

    public Parent createLoginScreen(){
        BorderPane loginScreenLayout = new BorderPane();

        VBox loginLayout = new VBox();
        loginLayout.setPadding(new Insets(20, 20, 20, 20));
        loginLayout.setSpacing(10);

        Label loginLabel = new Label("Login:");
        TextField loginField = new TextField();

        Label passwordLabel = new Label("Password:");
        PasswordField passwordField = new PasswordField();

        Button loginButton = new Button("Sign in");
        loginButton.setOnAction((event) -> {
            if(this.host.loginProcedureArg(loginField.getText(), passwordField.getText())){
                if(this.host.account.getType().equals("guest")){
                    MainScreenGUI mainScreen = new MainScreenGUI(this.window, this.host);
                    this.window.setScene(new Scene(mainScreen.createMainScreen("guest")));
                }else if(this.host.account.getType().equals("standard")){

                }else if(this.host.account.getType().equals("admin")){

                }
            }else{
                Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                errorAlert.setTitle("Test");
                errorAlert.setContentText("Username or password is wrong, try again.");
            };
        });

        Button changeToSignUpButton = new Button("Register");
        changeToSignUpButton.setOnAction((event) -> {
            SignUpGUI registerScreen = new SignUpGUI(this.window, this.host);
            Scene registerScene = new Scene(registerScreen.createSignUpScreen());

            this.window.setScene(registerScene);
        });

        loginLayout.getChildren().addAll(loginLabel, loginField, passwordLabel, passwordField, loginButton);
        loginScreenLayout.setCenter(loginLayout);
        loginScreenLayout.setTop(changeToSignUpButton);

        return loginScreenLayout;
    }
}
