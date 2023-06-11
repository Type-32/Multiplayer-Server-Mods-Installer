import java.io.*;
import java.net.URL;
import java.nio.file.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import org.json.JSONObject;

public class Installer extends Application {
    private static final long serialVersionUID = 1L;
    private File selectedDirectory;
    private Label directoryPath;
    private ProgressBar progressBar;
    private Button cancelButton;
    private TextArea logsWindow;
    private Task<Void> worker;
    private String versionCheckFileContent;

    public void start(Stage primaryStage) {
        // Set up UI
        primaryStage.setTitle("InfSMP Mods Installer");

        // Set default selected directory
        String osName = System.getProperty("os.name").toLowerCase();
        if (osName.contains("windows")) {
            selectedDirectory = new File("C:\\Users\\" + System.getProperty("user.name")
                    + "\\AppData\\Roaming\\.minecraft\\mods");
        } else {
            selectedDirectory = new File(System.getProperty("user.home") + "/Library/Application Support/minecraft/mods");
        }

        // Directory selector
        HBox directoryPanel = new HBox();
        directoryPanel.setSpacing(10);
        Button directorySelector = new Button("Select Directory");
        directorySelector.setOnAction(e -> {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setInitialDirectory(selectedDirectory);
            selectedDirectory = chooser.showDialog(primaryStage);
            directoryPath.setText(selectedDirectory != null ? selectedDirectory.getAbsolutePath() : "NullReferenceException thrown");
        });
        directoryPanel.getChildren().add(directorySelector);

        // Directory path label
        directoryPath = new Label();
        if (selectedDirectory != null) {
            directoryPath.setText(selectedDirectory.getAbsolutePath());
        }
        directoryPanel.getChildren().add(directoryPath);

        // Install/Update button
        Button installButton = new Button("Install / Update");
        installButton.setOnAction(e -> {

            if (selectedDirectory == null || (!selectedDirectory.getName().contains("mods") && !selectedDirectory.getAbsolutePath().contains("minecraft"))) {
                showAlert(Alert.AlertType.ERROR, "Please select a VALID mods directory first.");
                return;
            }
            primaryStage.getScene().setRoot(createProgressScene());

            // Check if directory has files
            if (selectedDirectory.list().length > 0) {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                        "There are files in your selected directory. Are you sure you want to continue?",
                        ButtonType.YES, ButtonType.NO);
                alert.showAndWait().ifPresent(result -> {
                    if (result == ButtonType.YES) {
                        // Delete all files in directory
                        for (File file : selectedDirectory.listFiles()) {
                            file.delete();
                        }
                    } else {
                        primaryStage.getScene().setRoot(createMainScene());
                        return;
                    }
                });
            }

            // Perform download and installation in background
            Task<Void> task = new Task<Void>() {
                protected Void call() throws Exception {
                    try {
                        // Download latest source code zip file
                        updateMessage("Downloading latest source code zip file...");
                        URL url = new URL(
                                "https://api.github.com/repos/Type-32/InfSMP-Mods/releases/latest");
                        InputStream in = url.openStream();
                        String response = getByteStream(in);
                        JSONObject json = new JSONObject(response);
                        String downloadUrl = json.getString("zipball_url");
                        in.close();

                        File zipFile = new File(selectedDirectory, "source.zip");
                        Files.copy(new URL(downloadUrl).openStream(), zipFile.toPath(),
                                StandardCopyOption.REPLACE_EXISTING);

                        updateMessage("Extracting zip file...");
                        // Extract zip file
                        ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile));
                        ZipEntry entry;
                        while ((entry = zis.getNextEntry()) != null) {
                            File file = new File(selectedDirectory, entry.getName());
                            if (entry.isDirectory()) {
                                file.mkdirs();
                            } else {
                                Files.copy(zis, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
                            }
                        }
                        zis.close();

                        updateMessage("Moving files from extracted folder to current directory...");
                        // Move files from extracted folder to current directory
                        File extractedFolder = new File(selectedDirectory, selectedDirectory.list()[0]);
                        for (File file : extractedFolder.listFiles()) {
                            Files.move(file.toPath(),
                                    new File(selectedDirectory, file.getName()).toPath(),
                                    StandardCopyOption.REPLACE_EXISTING);
                        }

                        updateMessage("Deleting extracted folder and zip file...");
                        // Delete extracted folder and zip file
                        extractedFolder.delete();
                        zipFile.delete();

                        updateMessage("Finished.");
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }

                    return null;
                }
            };
            task.messageProperty().addListener((obs, oldMessage, newMessage) -> {
                progressBar.setProgress(-1);
                logsWindow.appendText(newMessage + "\n");
            });
            task.setOnSucceeded(event -> {
                primaryStage.getScene().setRoot(createMainScene());
                showAlert(Alert.AlertType.INFORMATION, "Finished.");
            });
            new Thread(task).start();
        });

        // Set up main scene
        primaryStage.setScene(new Scene(createMainScene()));
        primaryStage.show();
    }
    private void showAlert(Alert.AlertType type, String message) {
        Alert alert = new Alert(type, message);
        alert.showAndWait();
    }
    private Pane createMainScene() {
        VBox root = new VBox();
        root.setSpacing(10);
        root.setPadding(new Insets(10));

        root.getChildren().add(directoryPath.getParent());

        Button installButton = new Button("Install / Update");
        installButton.setOnAction(((Button) directoryPath.getParent().getChildrenUnmodifiable().get(0)).getOnAction());
        root.getChildren().add(installButton);

        return root;
    }

    private Pane createProgressScene() {
        VBox root = new VBox();
        root.setSpacing(10);
        root.setPadding(new Insets(10));

        root.getChildren().add(new Label("Download Progress"));

        progressBar = new ProgressBar();
        progressBar.setPrefWidth(400);
        root.getChildren().add(progressBar);

        cancelButton = new Button("Cancel");
        cancelButton.setOnAction(e -> {
            Platform.exit();
            System.exit(0);
        });
        root.getChildren().add(cancelButton);

        logsWindow = new TextArea();
        logsWindow.setEditable(false);
        logsWindow.setPrefRowCount(10);
        logsWindow.setPrefColumnCount(40);
        root.getChildren().add(logsWindow);

        return root;
    }

    public String getByteStream(InputStream in) throws IOException{
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[1024];
        while ((nRead = in.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        buffer.flush();
        return buffer.toString();
    }

    public static void main(String[] args) {
        new Installer();
    }
}
