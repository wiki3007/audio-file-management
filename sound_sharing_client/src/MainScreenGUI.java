import javafx.collections.ObservableList;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class MainScreenGUI {
    Stage window;
    RemoteHostMasterThread host;
    SoundFile file;
    ArrayList<SoundFile> fileList;
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

        TableColumn<String, SoundFile> sizeColumn = new TableColumn<>("Size");
        sizeColumn.setCellValueFactory(new PropertyValueFactory<>("size"));
        tableView.getColumns().add(sizeColumn);

        TableColumn<String, SoundFile> formatColumn = new TableColumn<>("Format");
        formatColumn.setCellValueFactory(new PropertyValueFactory<>("format"));
        tableView.getColumns().add(formatColumn);

        TableColumn<String, SoundFile> typeColumn = new TableColumn<>("Type");
        typeColumn.setCellValueFactory(new PropertyValueFactory<>("type"));
        tableView.getColumns().add(typeColumn);

        TableColumn<String, SoundFile> dateAddedColumn = new TableColumn<>("Date added");
        dateAddedColumn.setCellValueFactory(new PropertyValueFactory<>("dateAdded"));
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
        this.fileList = host.getPublicFileArray();
        this.listLists = host.getPublicListArray();

        GridPane layout = new GridPane();

        BorderPane filesLayout = new BorderPane();
        BorderPane listsLayout = new BorderPane();

        HBox filesButtons = new HBox();
        Button addFileButton = new Button("Load all files");

        filesButtons.getChildren().addAll(addFileButton);

        TableView fileTable = createFileTable();

        for(SoundFile file : fileList){
            fileTable.getItems().add(file);
        }

        TableView.TableViewSelectionModel<SoundFile> fileSelectionModel = fileTable.getSelectionModel();
        fileSelectionModel.setSelectionMode(SelectionMode.SINGLE);

        filesLayout.setTop(filesButtons);
        filesLayout.setCenter(fileTable);

        BorderPane playerLayout = new BorderPane();
        HBox playerButtonsLayout = new HBox();
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
            }
        });

        playerButtonsLayout.getChildren().addAll(playButton, pauseButton, stopButton);
        playerLayout.setCenter(playerButtonsLayout);
        filesLayout.setBottom(playerLayout);

        HBox listsButtons = new HBox();
        Button createListButton = new Button("Create temporary list");

        listsButtons.getChildren().addAll(createListButton);
        listsLayout.setTop(listsButtons);

        layout.add(filesLayout, 0, 0);
        layout.add(listsLayout, 0, 1);
        layout.add(playerLayout, 1, 0);

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
