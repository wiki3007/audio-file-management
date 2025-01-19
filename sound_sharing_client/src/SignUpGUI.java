import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class SignUpGUI{
    Stage window;
    RemoteHostMasterThread host;

    SignUpGUI(Stage window, RemoteHostMasterThread host){
        this.window = window;
        this.host = host;
    }

    public Parent createSignUpScreen(){
        BorderPane signUpScreenLayout = new BorderPane();

        VBox signUpLayout = new VBox();
        signUpLayout.setPadding(new Insets(20, 20, 20, 20));
        signUpLayout.setSpacing(10);

        Label loginLabel = new Label("New login:");
        TextField loginField = new TextField();

        Label passwordLabel = new Label("Password:");
        PasswordField passwordField = new PasswordField();

        Label confirmPasswordLabel = new Label("Confirm password:");
        PasswordField confirmPasswordField = new PasswordField();

        Button loginButton = new Button("Sign up");

        Button changeToSignInButton = new Button("Register");
        changeToSignInButton.setOnAction((event) -> {
            LoginGUI loginScreen = new LoginGUI(this.window, this.host);
            Scene loginScene = new Scene(loginScreen.createLoginScreen());

            this.window.setScene(loginScene);
        });

        signUpLayout.getChildren().addAll(loginLabel, loginField, passwordLabel, passwordField,
                confirmPasswordLabel, confirmPasswordField, loginButton);
        signUpScreenLayout.setCenter(signUpLayout);
        signUpScreenLayout.setTop(changeToSignInButton);

        return signUpScreenLayout;
    }
}
