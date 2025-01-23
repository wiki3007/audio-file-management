import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.Objects;

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

        Button changeToSignInButton = new Button("Login");
        changeToSignInButton.setOnAction((event) -> {
            LoginGUI loginScreen = new LoginGUI(this.window, this.host);
            Scene loginScene = new Scene(loginScreen.createLoginScreen());

            this.window.setScene(loginScene);
        });

        loginButton.setOnAction((event) -> {
            if(loginField.getText().isEmpty()){
                Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                errorAlert.setTitle("Test");
                errorAlert.setContentText("You must enter login!");
                errorAlert.showAndWait();
                return;
            }
            if(passwordField.getText().isEmpty()){
                Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                errorAlert.setTitle("Test");
                errorAlert.setContentText("You must enter password!");
                errorAlert.showAndWait();
                return;
            }
            if(!Objects.equals(passwordField.getText(), confirmPasswordField.getText())){
                Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                errorAlert.setTitle("Test");
                errorAlert.setContentText("Passwords do not match!");
                errorAlert.showAndWait();
                return;
            }
            System.out.println("ok");
            if(host.registerProcedureArgs(loginField.getText(), passwordField.getText())){
                Alert errorAlert = new Alert(Alert.AlertType.INFORMATION);
                errorAlert.setTitle("Test");
                errorAlert.setContentText("Welcome!");
                errorAlert.show();
                MainScreenGUI mainScreen = new MainScreenGUI(this.window, this.host);
                this.window.setScene(new Scene(mainScreen.createMainScreen("standard")));
            }else{
                Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                errorAlert.setTitle("Test");
                errorAlert.setContentText("Something went wrong with registering.");
                errorAlert.showAndWait();
            }
        });

        signUpLayout.getChildren().addAll(loginLabel, loginField, passwordLabel, passwordField,
                confirmPasswordLabel, confirmPasswordField, loginButton);
        signUpScreenLayout.setCenter(signUpLayout);
        signUpScreenLayout.setTop(changeToSignInButton);
        signUpScreenLayout.setPadding(new Insets(10, 10, 10, 10));

        return signUpScreenLayout;
    }
}
