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

public class Installer extends JFrame {
    private static final long serialVersionUID = 1L;
    private File selectedDirectory;
    private JTextField directoryPath;
    private JProgressBar progressBar;

    public Installer() {
        // Set up UI
        setTitle("Installer");
        setSize(700, 300);
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
            selectedDirectory = chooser.getSelectedFile();
            directoryPath.setText(selectedDirectory.getAbsolutePath());
        });
        directoryPanel.add(directorySelector);

        File subSD = new File(selectedDirectory.getAbsolutePath());

        // Directory path text box
        directoryPath = new JTextField(30);
        directoryPath.setEditable(false);
        directoryPanel.add(directoryPath);

        add(directoryPanel);

        directoryPath.setText(selectedDirectory.getAbsolutePath());
        System.out.println(selectedDirectory.getAbsolutePath());

        // Install/Update button
        JButton installButton = new JButton("Install / Update");
        installButton.addActionListener(e -> {
            if (selectedDirectory == null && (!selectedDirectory.getAbsolutePath().contains("mods") && !selectedDirectory.getAbsolutePath().contains("minecraft"))) {
                JOptionPane.showMessageDialog(Installer.this, "Please select a directory first.");
                return;
            }

            try {
                // Download latest source code zip file
                URL url = new URL(
                        "https://api.github.com/repos/Type-32/InfSMP-Mods/releases/latest");
                InputStream in = url.openStream();
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                int nRead;
                byte[] data = new byte[5012];
                while ((nRead = in.read(data, 0, data.length)) != -1) {
                    buffer.write(data, 0, nRead);
                }
                buffer.flush();
                String response = new String(buffer.toByteArray());
                JSONObject json = new JSONObject(response);
                String downloadUrl = json.getString("zipball_url");
                in.close();

                File zipFile = new File(selectedDirectory, "source.zip");

                try {
                    Files.copy(new URL(downloadUrl).openStream(), zipFile.toPath(),
                            StandardCopyOption.REPLACE_EXISTING);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(Installer.this,
                            "An error occurred while downloading the source code zip file: " + ex.getMessage());
                    return;
                }

                // Extract zip file
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
                String fName = "";
                try {
                    for (File file : selectedDirectory.listFiles()) {
                        if (file.getName().contains("Type-32-InfSMP-Mods-")) {
                            fName = (osName.contains("windows") ? "\\" : "/") + file.getName();
                            break;
                        }
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(Installer.this,
                            "An error occurred while scanning for the source folder: " + ex.getMessage());
                }
                File extractedFolder = new File(selectedDirectory.getAbsolutePath() + fName);
//                try {
//
//                } catch (Exception ex) {
//                    JOptionPane.showMessageDialog(Installer.this,
//                            "An error occurred while moving mods to directory: " + ex.getMessage());
//                    System.console().printf(ex.toString());
//                }
                System.out.println("Extracted folder: " + extractedFolder.getAbsolutePath());
                for (File file : extractedFolder.listFiles()) {
                    System.out.println("Folder File: " + file.getAbsolutePath());
                    Files.move(file.toPath(), new File(selectedDirectory, file.getName()).toPath(),StandardCopyOption.REPLACE_EXISTING);
                }

                // Delete extracted folder and zip file
                extractedFolder.delete();

                JOptionPane.showMessageDialog(Installer.this, "Finished.");
            } catch (Exception ex) {
                System.out.println(ex.getCause());
                JOptionPane.showMessageDialog(Installer.this, "An error occurred while executing process: " + ex.getMessage());
                ex.printStackTrace();
            }

            System.exit(0);
        });
        add(installButton);

        // Progress Bar
        progressBar = new JProgressBar(0, 100);
        add(progressBar);
        progressBar.setValue(0);
        setVisible(true);

        setVisible(true);
    }

    public static void main(String[] args) {
        new Installer();
    }
}
