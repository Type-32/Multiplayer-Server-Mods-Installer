import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.json.JSONObject;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.io.BufferedReader;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

class ReleaseData {
    public String versionTagName = "";
    public String zipballURL = "";
    public String originalURL = "";
}

public class Installer extends JFrame {
    private static final long serialVersionUID = 1L;
    private File selectedDirectory;
    private JTextField directoryPath;
    private String version,logs;

    public Installer() {
        // Set up UI
        setTitle("Installer");
        setSize(600, 200);
        setLayout(new FlowLayout());

        // Set default selected directory
        String osName = System.getProperty("os.name").toLowerCase();
        if (osName.contains("windows")) {
            selectedDirectory = new File("C:\\Users\\" + System.getProperty("user.name")
                    + "\\AppData\\Roaming\\.minecraft\\mods");
        } else {
            selectedDirectory = new File(System.getProperty("user.home") + "/Library/Application Support/minecraft/mods");
        }

        // Directory selector
        JPanel directoryPanel = new JPanel();
        directoryPanel.setLayout(new BoxLayout(directoryPanel, BoxLayout.X_AXIS));
        JButton directorySelector = new JButton("Select Directory");
        directorySelector.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            chooser.setCurrentDirectory(selectedDirectory);
            chooser.showOpenDialog(Installer.this);
            selectedDirectory = chooser.getSelectedFile();
            if (selectedDirectory == null){
                if (osName.contains("windows")) {
                    selectedDirectory = new File("C:\\Users\\" + System.getProperty("user.name")
                            + "\\AppData\\Roaming\\.minecraft\\mods");
                } else {
                    selectedDirectory = new File(System.getProperty("user.home") + "/Library/Application Support/minecraft/mods");
                }
            }
            directoryPath.setText(selectedDirectory.getAbsolutePath());
            log("Set directory " + selectedDirectory.getAbsolutePath());
        });
        directoryPanel.add(directorySelector);

        // Directory path text box
        directoryPath = new JTextField(35);
        directoryPath.setEditable(false);
        directoryPanel.add(directoryPath);

        add(directoryPanel);

        directoryPath.setText(selectedDirectory.getAbsolutePath());
        System.out.println(selectedDirectory.getAbsolutePath());

        // Install/Update button
        JButton installButton = new JButton("Install / Update");
        installButton.addActionListener(e -> {
            if (selectedDirectory == null && (!selectedDirectory.getAbsolutePath().contains("mods") && !selectedDirectory.getAbsolutePath().contains("minecraft"))) {
                JOptionPane.showMessageDialog(Installer.this, "Please select a Valid Mods directory first.");
                log("Invalid directory " + selectedDirectory.getAbsolutePath());
                return;
            }

            try {

                // Delete old files
                log("Try delete mod files under dir " + selectedDirectory.getAbsolutePath());
                for (File file : selectedDirectory.listFiles()) {
                    if (file.getName().endsWith(".jar")) {
                        log("Deleted " + file.getName());
                        file.delete();
                    }
                }

                // Download latest source code zip file
                log("Try fetching latest source code zip file release from GitLab");
//                URL url = new URL(
//                        "https://api.github.com/repos/Type-32/InfSMP-Mods/releases/latest");
//                InputStream in = url.openStream();
//                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
//                int nRead;
//                byte[] data = new byte[5012];
//                while ((nRead = in.read(data, 0, data.length)) != -1) {
//                    buffer.write(data, 0, nRead);
//                }
//                buffer.flush();
//                String response = new String(buffer.toByteArray());
//                JSONObject json = new JSONObject(response);
//                String downloadUrl = json.getString("zipball_url");
                ReleaseData newdat = getLatestRelease("glpat-KNKWUrMw6zEitLhRspsJ", "46729939");
                String downloadUrl = newdat.zipballURL;
                version = newdat.versionTagName;
                log("Retrieved downloadUrl from master " + newdat.originalURL);
                log("Retrieved version tag " + version);

                log("Try downloading source code zip file from retrieved " + downloadUrl);
                File zipFile = new File(selectedDirectory, "source.zip");

                try {
                    downloadZipFile(downloadUrl, "glpat-KNKWUrMw6zEitLhRspsJ", zipFile.getAbsolutePath());
                    //Files.copy(new URL(downloadUrl).openStream(), zipFile.toPath(),StandardCopyOption.REPLACE_EXISTING);
                } catch (Exception ex) {
                    log("Failed to download source code zip file with error " + ex.getMessage());
                    JOptionPane.showMessageDialog(Installer.this,
                            "An error occurred while downloading the source code zip file: " + ex.getMessage());
                    return;
                }

                // Extract zip file
                log("Try extracting source code zip file to " + selectedDirectory.getAbsolutePath());
                ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipFile.toPath()));
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
                zipFile.delete();

                // Move files from extracted folder to current directory
                log("Try moving files from extracted folder to " + selectedDirectory.getAbsolutePath());
                String fName = "";
                try {
                    for (File file : selectedDirectory.listFiles()) {
                        if (file.getName().contains("infsmp-mods-")) {
                            fName = (osName.contains("windows") ? "\\" : "/") + file.getName();
                            break;
                        }
                    }
                } catch (Exception ex) {
                    log("Failed to scan for extracted folder with error " + ex.getMessage());
                    JOptionPane.showMessageDialog(Installer.this,
                            "An error occurred while scanning for the source folder: " + ex.getMessage());
                }
                File extractedFolder = new File(selectedDirectory.getAbsolutePath() + fName);

                System.out.println("Extracted folder: " + extractedFolder.getAbsolutePath());
                log("Extracted folder " + extractedFolder.getAbsolutePath());
                for (File file : extractedFolder.listFiles()) {
                    System.out.println("Folder File: " + file.getAbsolutePath());
                    log("Move file " + file.getAbsolutePath());
                    Files.move(file.toPath(), new File(selectedDirectory, file.getName()).toPath(),StandardCopyOption.REPLACE_EXISTING);
                }

                // Delete extracted folder and zip file
                log("Deleting extracted folder " + extractedFolder.getAbsolutePath());
                extractedFolder.delete();

                for (File file : selectedDirectory.listFiles()) {
                    log("Checking " + file.getAbsolutePath() + " for .zip");
                    if(file.getName().endsWith(".zip") || file.getName().equals("source.zip")){
                        log("Found zip " + file.getAbsolutePath());
                        file.delete();
                        break;
                    }
                }

                log("Installation finished with version tag " + version + " from repo " + newdat.originalURL);
                JOptionPane.showMessageDialog(Installer.this, "Installation Finished.\nVersion Tag (Debug): " + version);
            } catch (Exception ex) {
                log("Failed to install with error " + ex.getMessage());
                System.out.println(ex.getCause());
                JOptionPane.showMessageDialog(Installer.this, "An error occurred while executing process: " + ex.getMessage());
                ex.printStackTrace();
            }

            try (FileWriter writer = new FileWriter("install_logs.txt")) {
                writer.write(logs);
            } catch (Exception ex) {
                ex.printStackTrace();
            }

            System.exit(0);
        });
        add(installButton);
        setVisible(true);
    }
    private void log(String message) {
        logs += message + "\n";
    }

    public static void downloadZipFile(String zipballURL, String personalAccessToken, String filePath) {

        try {
            URL urlObj = new URL(zipballURL);
            HttpURLConnection connection = (HttpURLConnection) urlObj.openConnection();

            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "*/*");

            BufferedInputStream in = new BufferedInputStream(connection.getInputStream());
            FileOutputStream fileOutputStream = new FileOutputStream(filePath);
            byte[] dataBuffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
                fileOutputStream.write(dataBuffer, 0, bytesRead);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static ReleaseData getLatestRelease(String personalAccessToken, String projectId) {
        ReleaseData releaseData = new ReleaseData();
        String url = "https://gitlab.com/api/v4/projects/" + projectId + "/releases";
        releaseData.originalURL = url;

        try {
            URL urlObj = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) urlObj.openConnection();

            connection.setRequestMethod("GET");
            connection.setRequestProperty("PRIVATE-TOKEN", personalAccessToken);

            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            System.out.println(response.toString());
            JsonArray releases = new Gson().fromJson(response.toString(), JsonArray.class);
            if (releases.size() > 0) {
                JsonObject latestRelease = releases.get(0).getAsJsonObject();
                System.out.println(latestRelease.toString());
                releaseData.versionTagName = latestRelease.get("tag_name").getAsString();
                releaseData.zipballURL = latestRelease.get("assets").getAsJsonObject().get("sources").getAsJsonArray().get(0).getAsJsonObject().get("url").getAsString();
                System.out.println(releaseData.zipballURL);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return releaseData;
    }

    public static void main(String[] args) {
        new Installer();
    }
}
