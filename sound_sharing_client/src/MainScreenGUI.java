import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicReference;

public class MainScreenGUI {
    Stage window;
    RemoteHostMasterThread host;
    SoundFile file;
    ArrayList<SoundFile> fileList;
    ArrayList<SoundFile> privateFileList;
    SoundList list;
    ArrayList<SoundList> listLists;
    MediaPlayer musicPlayer;

    MainScreenGUI(Stage window, RemoteHostMasterThread host){
        this.window = window;
        this.host = host;
    }

    TableView createFileTable(){
        TableView tableView = new TableView();
        tableView.setPlaceholder(new Label("No files to display"));

        TableColumn<String, SoundFile> idColumn = new TableColumn<>("File id");
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        tableView.getColumns().add(idColumn);

        TableColumn<String, SoundFile> nameColumn = new TableColumn<>("Name");
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        tableView.getColumns().add(nameColumn);

        TableColumn<String, SoundFile> descriptionColumn = new TableColumn<>("Description");
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
        tableView.getColumns().add(descriptionColumn);

        TableColumn<String, SoundFile> durationColumn = new TableColumn<>("Duration");
        durationColumn.setCellValueFactory(new PropertyValueFactory<>("duration"));
        tableView.getColumns().add(durationColumn);

        TableColumn<String, SoundFile> sizeColumn = new TableColumn<>("Size (B)");
        sizeColumn.setCellValueFactory(new PropertyValueFactory<>("size"));
        tableView.getColumns().add(sizeColumn);

        TableColumn<String, SoundFile> formatColumn = new TableColumn<>("Format");
        formatColumn.setCellValueFactory(new PropertyValueFactory<>("format"));
        tableView.getColumns().add(formatColumn);

        TableColumn<String, SoundFile> typeColumn = new TableColumn<>("Type");
        typeColumn.setCellValueFactory(new PropertyValueFactory<>("type"));
        tableView.getColumns().add(typeColumn);

        TableColumn<String, SoundFile> dateAddedColumn = new TableColumn<>("Date added");
        dateAddedColumn.setCellValueFactory(new PropertyValueFactory<>("date_added"));
        tableView.getColumns().add(dateAddedColumn);

        return tableView;
    }

    TableView createListTable(){
        TableView tableView = new TableView();

        return tableView;
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
        this.fileList = host.getAllPublicFilesUpdate();
        this.listLists = host.getAllPublicListsUpdate();

        GridPane layout = new GridPane();

        BorderPane filesLayout = new BorderPane();
        BorderPane listsLayout = new BorderPane();

        HBox filesButtons = new HBox();
        Button loadAllFilesButton = new Button("Load all files");

        filesButtons.getChildren().addAll(loadAllFilesButton);

        TableView fileTable = createFileTable();

        for(SoundFile file : fileList){
            fileTable.getItems().add(file);
        }

        TableView.TableViewSelectionModel<SoundFile> fileSelectionModel = fileTable.getSelectionModel();
        fileSelectionModel.setSelectionMode(SelectionMode.SINGLE);

        filesLayout.setTop(filesButtons);
        filesLayout.setCenter(fileTable);

        GridPane playerLayout = new GridPane();
        HBox playerButtonsLayout = new HBox();
        playerButtonsLayout.setSpacing(10);
        playerButtonsLayout.setPadding(new Insets(10, 10, 10, 10));
        HBox controlLayout = new HBox();
        controlLayout.setSpacing(10);
        controlLayout.setPadding(new Insets(10, 10, 10, 10));

        Slider volumeSlider = new Slider();
        volumeSlider.setMin(0);
        volumeSlider.setMax(100);
        volumeSlider.setValue(50);
        volumeSlider.setShowTickLabels(true);
        volumeSlider.setShowTickMarks(true);
        volumeSlider.valueProperty().addListener((listener) -> {
            if(musicPlayer != null){
                musicPlayer.setVolume(volumeSlider.getValue() / 100);
            }

        });

        Button playButton = new Button("Play");
        playButton.setOnAction((event) -> {
            if(musicPlayer == null){
                ObservableList<SoundFile> selectedFile = fileSelectionModel.getSelectedItems();
                file = selectedFile.getFirst();
                try {
                    host.getSoundFile(file, false);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                String path = file.getPath();
                System.out.println(path);
                Media media = new Media(new File(path).toURI().toString());
                musicPlayer = new MediaPlayer(media);
            }
            musicPlayer.play();
        });

        Button pauseButton = new Button("Pause");
        pauseButton.setOnAction((event) -> {
            if(this.musicPlayer != null){
                musicPlayer.pause();
            }
        });

        Button stopButton = new Button("Stop");
        stopButton.setOnAction((event) -> {
            if(this.musicPlayer != null){
                musicPlayer.stop();
                musicPlayer = null;
                new File(file.getPath()).delete();
                file = null;
            }
        });

        Label volumeLabel = new Label("Volume");

        playerButtonsLayout.getChildren().addAll(playButton, pauseButton, stopButton);
        controlLayout.getChildren().addAll(volumeLabel, volumeSlider);
        playerLayout.add(playerButtonsLayout, 0, 0);
        playerLayout.add(controlLayout, 0, 1);
        filesLayout.setBottom(playerLayout);

        HBox listsButtons = new HBox();
        Button createListButton = new Button("Create temporary list");

        listsButtons.getChildren().addAll(createListButton);
        listsLayout.setTop(listsButtons);

        filesLayout.setPadding(new Insets(10, 10, 10, 10));
        listsLayout.setPadding(new Insets(10, 10, 10, 10));
        playerLayout.setPadding(new Insets(10, 10, 10, 10));

        layout.add(filesLayout, 0, 0);
        layout.add(listsLayout, 1, 0);
        layout.add(playerLayout, 0, 1);
        layout.setPadding(new Insets(10, 10, 10, 10));

        return layout;
    }

    private Parent createStandardScreen(){
        this.fileList = host.getAllPublicFilesUpdate();
        this.listLists = host.getAllPublicListsUpdate();

        TabPane tabPane = new TabPane();
        GridPane layout = new GridPane();
        GridPane privateLayout = new GridPane();

        BorderPane filesLayout = new BorderPane();
        BorderPane listsLayout = new BorderPane();

        HBox filesButtons = new HBox();
        Button loadAllFilesButton = new Button("Load all files");
        Button addFileButton = new Button("Add file");
        Button deleteFileButton = new Button("Delete file");

        TableView fileTable = createFileTable();

        for(SoundFile file : fileList){
            fileTable.getItems().add(file);
        }

        TableView.TableViewSelectionModel<SoundFile> fileSelectionModel = fileTable.getSelectionModel();
        fileSelectionModel.setSelectionMode(SelectionMode.SINGLE);

        loadAllFilesButton.setOnAction((event) -> {
            this.fileList = host.getAllPublicFilesUpdate();
            fileTable.getItems().clear();
            for(SoundFile file : fileList){
                fileTable.getItems().add(file);
            }
        });

        addFileButton.setOnAction((event) -> {
            FileChooser fileChooser = new FileChooser();
            FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("Audio files (*.flac, *.mp3," +
                    " *.ogg, *.wav, *.wma, *.webm", "*.flac", "*.mp3", "*.ogg", "*.wav", "*.wma", "*.webm");
            fileChooser.getExtensionFilters().add(extFilter);

            File selectedFile = fileChooser.showOpenDialog(window);
            if(selectedFile != null){
                try {
                    String fileName = selectedFile.getName();
                    String path = selectedFile.toURI().toURL().toString();
                    String extension = fileName.substring(fileName.lastIndexOf(".") + 1,
                            selectedFile.getName().length());
                    int size = (int) selectedFile.length();
                    //String dateAdded = LocalDate.now().toString();

                    Media musicFile = new Media(selectedFile.toURI().toURL().toString());
                    String duration = musicFile.getDuration().toString();

                    GridPane descriptionWindow = new GridPane();
                    Label descriptionLabel = new Label("Enter description");
                    TextArea descriptionArea = new TextArea();

                    HBox buttons = new HBox();

                    Button addNewFileButton = new Button("Add file");
                    Button cancelButton = new Button("Cancel");

                    buttons.getChildren().addAll(addNewFileButton, cancelButton);

                    addNewFileButton.setOnAction((windowEvent) -> {
                        String description = "";
                        description = descriptionArea.getText();

                        try {
                            host.addFile(fileName, description, duration, size, extension, "public", path);
                            Stage stage = (Stage) addNewFileButton.getScene().getWindow();
                            stage.close();

                            this.fileList = host.getAllPublicFilesUpdate();
                            fileTable.getItems().clear();
                            for(SoundFile file : fileList){
                                fileTable.getItems().add(file);
                            }
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });

                    cancelButton.setOnAction((cancelEvent) -> {
                        Stage stage = (Stage) cancelButton.getScene().getWindow();
                        stage.close();
                    });

                    descriptionWindow.add(descriptionLabel, 0, 0);
                    descriptionWindow.add(descriptionArea, 0, 1);
                    descriptionWindow.add(buttons, 0, 2);

                    Scene descriptionScene = new Scene(descriptionWindow, 230, 100);
                    Stage newWindow = new Stage();
                    newWindow.initModality(Modality.APPLICATION_MODAL);
                    newWindow.setTitle("Test");
                    newWindow.setScene(descriptionScene);
                    newWindow.setX(this.window.getX() + 200);
                    newWindow.setY(this.window.getY() + 100);
                    newWindow.show();
                } catch (MalformedURLException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        deleteFileButton.setOnAction((deleteEvent) -> {
            ObservableList<SoundFile> selectedFile = fileSelectionModel.getSelectedItems();
            file = selectedFile.getFirst();

            if(file != null){
                host.deleteFile(file);

                this.fileList = host.getAllPublicFilesUpdate();
                fileTable.getItems().clear();
                for(SoundFile file : fileList){
                    fileTable.getItems().add(file);
                }

                file = null;
            }
        });

        filesButtons.getChildren().addAll(loadAllFilesButton, addFileButton, deleteFileButton);

        filesLayout.setTop(filesButtons);
        filesLayout.setCenter(fileTable);

        GridPane playerLayout = new GridPane();
        HBox playerButtonsLayout = new HBox();
        playerButtonsLayout.setSpacing(10);
        playerButtonsLayout.setPadding(new Insets(10, 10, 10, 10));
        HBox controlLayout = new HBox();
        controlLayout.setSpacing(10);
        controlLayout.setPadding(new Insets(10, 10, 10, 10));

        Slider volumeSlider = new Slider();
        volumeSlider.setMin(0);
        volumeSlider.setMax(100);
        volumeSlider.setValue(50);
        volumeSlider.setShowTickLabels(true);
        volumeSlider.setShowTickMarks(true);
        volumeSlider.valueProperty().addListener((listener) -> {
            if(musicPlayer != null){
                musicPlayer.setVolume(volumeSlider.getValue() / 100);
            }

        });

        Button playButton = new Button("Play");
        playButton.setOnAction((event) -> {
            if(musicPlayer == null){
                ObservableList<SoundFile> selectedFile = fileSelectionModel.getSelectedItems();
                file = selectedFile.getFirst();
                try {
                    host.getSoundFile(file, false);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                String path = file.getPath();
                System.out.println(path);
                Media media = new Media(new File(path).toURI().toString());
                musicPlayer = new MediaPlayer(media);
            }
            musicPlayer.play();
        });

        Button pauseButton = new Button("Pause");
        pauseButton.setOnAction((event) -> {
            if(this.musicPlayer != null){
                musicPlayer.pause();
            }
        });

        Button stopButton = new Button("Stop");
        stopButton.setOnAction((event) -> {
            if(this.musicPlayer != null){
                musicPlayer.stop();
                musicPlayer = null;
                new File(file.getPath()).delete();
                file = null;
            }
        });

        Button downloadButton = new Button("Download file");
        downloadButton.setOnAction((event) -> {
            ObservableList<SoundFile> selectedFile = fileSelectionModel.getSelectedItems();
            file = selectedFile.getFirst();
            try {
                host.getSoundFile(file, true);
                Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                successAlert.setTitle("Test");
                successAlert.setContentText("Successfully downloaded file to folder './downloads'.");
                successAlert.show();
            } catch (IOException e) {
                Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                errorAlert.setTitle("Test");
                errorAlert.setContentText("Something went wrong with downloading file.");
                errorAlert.show();
                throw new RuntimeException(e);
            }
        });

        Label volumeLabel = new Label("Volume ");

        playerButtonsLayout.getChildren().addAll(playButton, pauseButton, stopButton, downloadButton);
        controlLayout.getChildren().addAll(volumeLabel, volumeSlider);
        playerLayout.add(playerButtonsLayout, 0, 0);
        playerLayout.add(controlLayout, 0, 1);

        filesLayout.setBottom(playerLayout);

        HBox listsButtons = new HBox();
        Button createListButton = new Button("Create temporary list");

        listsButtons.getChildren().addAll(createListButton);
        listsLayout.setTop(listsButtons);

        layout.add(filesLayout, 0, 0);
        layout.add(listsLayout, 1, 0);
        layout.add(playerLayout, 0, 1);

        Tab publicTab = new Tab("Public", layout);

        TableView privateFileTable = createFileTable();

        for(SoundFile file : fileList){
            privateFileTable.getItems().add(file);
        }

        Tab privateTab = new Tab("Private", new Label("Under construction"));
        Tab shareTab = new Tab("Share", new Label("Under construction"));

        tabPane.getTabs().addAll(publicTab, privateTab, shareTab);
        tabPane.setTabDragPolicy(TabPane.TabDragPolicy.FIXED);
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        return tabPane;
    }

    private Parent createAdminScreen(){
        this.fileList = host.getAllPublicFilesUpdate();
        this.listLists = host.getAllPublicListsUpdate();

        GridPane layout = new GridPane();
        GridPane usersLayout = new GridPane();
        TabPane tabPane = new TabPane();

        BorderPane filesLayout = new BorderPane();
        BorderPane listsLayout = new BorderPane();

        HBox filesButtons = new HBox();
        Button loadAllFilesButton = new Button("Load all files");
        Button addFileButton = new Button("Add file");
        Button deleteFileButton = new Button("Delete file");

        TableView fileTable = createFileTable();

        for(SoundFile file : fileList){
            fileTable.getItems().add(file);
        }

        loadAllFilesButton.setOnAction((event) -> {
            this.fileList = host.getAllPublicFilesUpdate();
            fileTable.getItems().clear();
            for(SoundFile file : fileList){
                fileTable.getItems().add(file);
            }
        });

        addFileButton.setOnAction((event) -> {
            FileChooser fileChooser = new FileChooser();
            FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("Audio files (*.flac, *.mp3," +
                    " *.ogg, *.wav, *.wma, *.webm", "*.flac", "*.mp3", "*.ogg", "*.wav", "*.wma", "*.webm");
            fileChooser.getExtensionFilters().add(extFilter);

            File selectedFile = fileChooser.showOpenDialog(window);
            System.out.println("ok");
            if(selectedFile != null){
                try {
                    String fileName = selectedFile.getName();
                    String path = selectedFile.toURI().toURL().toString();
                    String extension = fileName.substring(fileName.lastIndexOf(".") + 1,
                            selectedFile.getName().length());
                    int size = (int) selectedFile.length();
                    //String dateAdded = LocalDate.now().toString();

                    Media musicFile = new Media(selectedFile.toURI().toURL().toString());
                    String duration = musicFile.getDuration().toString();

                    GridPane descriptionWindow = new GridPane();
                    Label descriptionLabel = new Label("Enter description");
                    TextArea descriptionArea = new TextArea();

                    HBox buttons = new HBox();

                    Button addNewFileButton = new Button("Add file");
                    Button cancelButton = new Button("Cancel");

                    buttons.getChildren().addAll(addNewFileButton, cancelButton);

                    addNewFileButton.setOnAction((windowEvent) -> {
                        String description = "";
                        description = descriptionArea.getText();

                        try {
                            host.addFile(fileName, description, duration, size, extension, "public", path);
                            Stage stage = (Stage) addNewFileButton.getScene().getWindow();
                            stage.close();

                            this.fileList = host.getAllPublicFilesUpdate();
                            fileTable.getItems().clear();
                            for(SoundFile file : fileList){
                                fileTable.getItems().add(file);
                            }
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });

                    cancelButton.setOnAction((cancelEvent) -> {
                        Stage stage = (Stage) cancelButton.getScene().getWindow();
                        stage.close();
                    });

                    descriptionWindow.add(descriptionLabel, 0, 0);
                    descriptionWindow.add(descriptionArea, 0, 1);
                    descriptionWindow.add(buttons, 0, 2);

                    Scene descriptionScene = new Scene(descriptionWindow, 230, 100);
                    Stage newWindow = new Stage();
                    newWindow.initModality(Modality.APPLICATION_MODAL);
                    newWindow.setTitle("Test");
                    newWindow.setScene(descriptionScene);
                    newWindow.setX(this.window.getX() + 200);
                    newWindow.setY(this.window.getY() + 100);
                    newWindow.show();
                } catch (MalformedURLException e) {
                    throw new RuntimeException(e);
                }
            } else{
                System.out.println("error");
            }
        });

        TableView.TableViewSelectionModel<SoundFile> fileSelectionModel = fileTable.getSelectionModel();
        fileSelectionModel.setSelectionMode(SelectionMode.SINGLE);

        deleteFileButton.setOnAction((deleteEvent) -> {
            ObservableList<SoundFile> selectedFile = fileSelectionModel.getSelectedItems();
            file = selectedFile.getFirst();

            if(file != null){
                host.deleteFile(file);

                this.fileList = host.getAllPublicFilesUpdate();
                fileTable.getItems().clear();
                for(SoundFile file : fileList){
                    fileTable.getItems().add(file);
                }

                file = null;
            }
        });

        filesButtons.getChildren().addAll(loadAllFilesButton, addFileButton, deleteFileButton);

        filesLayout.setTop(filesButtons);
        filesLayout.setCenter(fileTable);

        GridPane playerLayout = new GridPane();
        HBox playerButtonsLayout = new HBox();
        playerButtonsLayout.setSpacing(10);
        playerButtonsLayout.setPadding(new Insets(10, 10, 10, 10));
        HBox controlLayout = new HBox();
        controlLayout.setSpacing(10);
        controlLayout.setPadding(new Insets(10, 10, 10, 10));

        Slider volumeSlider = new Slider();
        volumeSlider.setMin(0);
        volumeSlider.setMax(100);
        volumeSlider.setValue(50);
        volumeSlider.setShowTickLabels(true);
        volumeSlider.setShowTickMarks(true);
        volumeSlider.valueProperty().addListener((listener) -> {
            if(musicPlayer != null){
                musicPlayer.setVolume(volumeSlider.getValue() / 100);
            }

        });

        Button playButton = new Button("Play");
        playButton.setOnAction((event) -> {
            if(musicPlayer == null){
                ObservableList<SoundFile> selectedFile = fileSelectionModel.getSelectedItems();
                file = selectedFile.getFirst();
                try {
                    host.getSoundFile(file, false);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                String path = file.getPath();
                System.out.println(path);
                Media media = new Media(new File(path).toURI().toString());
                musicPlayer = new MediaPlayer(media);
            }
            musicPlayer.play();
        });

        Button pauseButton = new Button("Pause");
        pauseButton.setOnAction((event) -> {
            if(this.musicPlayer != null){
                musicPlayer.pause();
            }
        });

        Button stopButton = new Button("Stop");
        stopButton.setOnAction((event) -> {
            if(this.musicPlayer != null){
                musicPlayer.stop();
                musicPlayer = null;
                new File(file.getPath()).delete();
                file = null;
            }
        });

        Button downloadButton = new Button("Download file");
        downloadButton.setOnAction((event) -> {
            ObservableList<SoundFile> selectedFile = fileSelectionModel.getSelectedItems();
            file = selectedFile.getFirst();
            try {
                host.getSoundFile(file, true);
                Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                successAlert.setTitle("Test");
                successAlert.setContentText("Successfully downloaded file to folder './downloads'.");
                successAlert.show();
            } catch (IOException e) {
                Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                errorAlert.setTitle("Test");
                errorAlert.setContentText("Something went wrong with downloading file.");
                errorAlert.show();
                throw new RuntimeException(e);
            }
        });

        Label volumeLabel = new Label("Volume");

        playerButtonsLayout.getChildren().addAll(playButton, pauseButton, stopButton, downloadButton);
        controlLayout.getChildren().addAll(volumeLabel, volumeSlider);
        playerLayout.add(playerButtonsLayout, 0, 0);
        playerLayout.add(controlLayout, 0, 1);
        filesLayout.setBottom(playerLayout);

        HBox listsButtons = new HBox();
        Button createListButton = new Button("Create temporary list");

        listsButtons.getChildren().addAll(createListButton);
        listsLayout.setTop(listsButtons);

        layout.add(filesLayout, 0, 0);
        layout.add(listsLayout, 1, 0);
        layout.add(playerLayout, 0, 1);



        Tab publicTab = new Tab("Public", layout);

        Tab usersTab = new Tab("Users", new Label("Under construction"));
        tabPane.getTabs().addAll(publicTab, usersTab);
        tabPane.setTabDragPolicy(TabPane.TabDragPolicy.FIXED);
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        return tabPane;
    }
}
