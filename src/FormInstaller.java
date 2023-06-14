import javax.swing.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.io.File;

public class FormInstaller extends JFrame {
    private static final long serialVersionUID = 1L;
    private File selectedDirectory;
    private String version;
    public static String logs;
    private JLabel appTitle;
    private JPanel mainWindow;
    private JTextField directoryDisplay;
    private JButton directorySelect;
    private JComboBox<ReleaseData> versionSelector;
    private JButton installButton;
    private JCheckBox deleteExistingFilesOption;
    private JPanel mainContent;
    private JComboBox<ModLoaderType> modloaderSelector;

    public void setDefaultDirectory(){
        // Set default selected directory
        String osName = System.getProperty("os.name").toLowerCase();
        if (osName.contains("windows")) {
            selectedDirectory = new File("C:\\Users\\" + System.getProperty("user.name")
                    + "\\AppData\\Roaming\\.minecraft\\mods");
        } else {
            selectedDirectory = new File(System.getProperty("user.home") + "/Library/Application Support/minecraft/mods");
        }
    }

    public void refreshVersionSelection(ModLoaderType type, VersionControlFetcher Fetcher) throws IOException {
        versionSelector.removeAllItems();
        ArrayList<ReleaseData> rl = Fetcher.fetch(), temp = new ArrayList<ReleaseData>();
        for (ReleaseData r : rl) {
            if (r.modLoaderType == type || (type == ModLoaderType.All && r.modLoaderType == ModLoaderType.None || r.modLoaderType == ModLoaderType.All)) {
                versionSelector.addItem(r);
                temp.add(r);
            }
        }
        if (temp.size() > 0)
            versionSelector.setSelectedItem(temp.toArray()[0]);
    }

    public FormInstaller() {
        // Content Pane Init
        setContentPane(mainWindow);
        setTitle("Mods Installer");
        setSize(600,250);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);
        setLocation(400, 200);

        deleteExistingFilesOption.setSelected(true);
        modloaderSelector.addItem(ModLoaderType.All);
        modloaderSelector.addItem(ModLoaderType.Fabric);
        modloaderSelector.addItem(ModLoaderType.Forge);
        modloaderSelector.addItem(ModLoaderType.Quilt);
        modloaderSelector.addItem(ModLoaderType.LiteLoader);

        // Customizable GitLab Instance
        ConfigInstance Instance = new ConfigInstance("glpat-KNKWUrMw6zEitLhRspsJ", "46729939", "infsmp-mods-");
        VersionControlFetcher Fetcher = new VersionControlFetcher(Instance, this);
        String osName = System.getProperty("os.name").toLowerCase();
        setDefaultDirectory();
        try{
            refreshVersionSelection(ModLoaderType.All, Fetcher);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Failed to fetch latest version from GitLab. Please check your internet connection and try again.\nError: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }

        // Directory selector
        directorySelect.addActionListener(e -> {
            selectInstallationDirectory(osName);
        });

        // Modloader selector
        modloaderSelector.addActionListener(e -> {
            try {
                refreshVersionSelection((ModLoaderType) modloaderSelector.getSelectedItem(), Fetcher);
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        });

        // Version selector
        versionSelector.addActionListener(e -> {
            if(versionSelector.getSelectedItem() == null) return;
            version = versionSelector.getSelectedItem().toString();
        });

        directoryDisplay.setText(this.selectedDirectory.getAbsolutePath());
        System.out.println(this.selectedDirectory.getAbsolutePath());

        // Install/Update button
        installButton.addActionListener(e -> {
            boolean flag = false;
            if (deleteExistingFilesOption.isSelected()) {
                log("User prompted to delete existing files");
                int res = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete all existing mods in the selected directory?", "Confirm Option", JOptionPane.YES_NO_OPTION);
                if (res == JOptionPane.NO_OPTION) {
                    log("User cancelled installation");
                    return;
                }
                log("User confirmed to delete existing files");
            }
            if (this.directorySelect == null && (!this.selectedDirectory.getAbsolutePath().contains("mods") && !this.selectedDirectory.getAbsolutePath().contains("minecraft"))) {
                JOptionPane.showMessageDialog(this, "Please select a Valid Mods directory first.");
                log("Invalid directory " + this.selectedDirectory.getAbsolutePath());
                return;
            }

            try {
                // Delete old files
                if (deleteExistingFilesOption.isSelected()) {
                    log("Try delete mod files under dir " + this.selectedDirectory.getAbsolutePath());
                    for (File file : this.selectedDirectory.listFiles()) {
                        if (file.getName().endsWith(".jar")) {
                            log("Deleted " + file.getName());
                            file.delete();
                        }
                    }
                }

                // Download latest source code zip file
                File zipFile = Fetcher.downloadRelease((ReleaseData) versionSelector.getSelectedItem(), this.selectedDirectory);

                // Extract zip file
                log("Try extracting source code zip file to " + this.selectedDirectory.getAbsolutePath());
                ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipFile.toPath()));
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    File file = new File(this.selectedDirectory, entry.getName());
                    if (entry.isDirectory()) {
                        file.mkdirs();
                    } else {
                        Files.copy(zis, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    }
                }
                zis.close();
                zipFile.delete();

                // Move files from extracted folder to current directory
                log("Try moving files from extracted folder to " + this.selectedDirectory.getAbsolutePath());
                String fName = "";
                try {
                    for (File file : this.selectedDirectory.listFiles()) {
                        if (file.getName().contains(Instance.EXTRACTED_FOLDER_PREFIX)) {
                            fName = (osName.contains("windows") ? "\\" : "/") + file.getName();
                            break;
                        }
                    }
                } catch (Exception ex) {
                    log("Failed to scan for extracted folder with error " + ex.getMessage());
                    JOptionPane.showMessageDialog(this,
                            "An error occurred while scanning for the source folder: " + ex.getMessage());
                }
                File extractedFolder = new File(this.selectedDirectory.getAbsolutePath() + fName);

                System.out.println("Extracted folder: " + extractedFolder.getAbsolutePath());
                log("Extracted folder " + extractedFolder.getAbsolutePath());
                for (File file : extractedFolder.listFiles()) {
                    System.out.println("Folder File: " + file.getAbsolutePath());
                    log("Move file " + file.getAbsolutePath());
                    Files.move(file.toPath(), new File(this.selectedDirectory, file.getName()).toPath(),StandardCopyOption.REPLACE_EXISTING);
                }

                // Delete extracted folder and zip file
                log("Deleting extracted folder " + extractedFolder.getAbsolutePath());
                extractedFolder.delete();

                for (File file : this.selectedDirectory.listFiles()) {
                    log("Checking " + file.getAbsolutePath() + " for .zip");
                    if(file.getName().endsWith(".zip") || file.getName().equals("source.zip")){
                        log("Found zip " + file.getAbsolutePath());
                        file.delete();
                        break;
                    }
                }

                log("Installation finished with version tag " + version + " from repo " + ((ReleaseData) versionSelector.getSelectedItem()).originalURL);
                flag = true;
            } catch (Exception ex) {
                log("Failed to install with error " + ex.getMessage());
                System.out.println(ex.getCause());
                JOptionPane.showMessageDialog(this, "An error occurred while executing process: " + ex.getMessage());
                ex.printStackTrace();
            }
            try (FileWriter writer = new FileWriter("install_logs.txt")) {
                writer.write(logs);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            if(flag){
                JOptionPane.showMessageDialog(this, "Download Complete.\nInstallation logs have been saved to install_logs.txt. (Path: " + new File("install_logs.txt").getAbsolutePath() + ")");
            }else{
                JOptionPane.showMessageDialog(this, "Installation failed. Please check install_logs.txt for more information. (Path: " + new File("install_logs.txt").getAbsolutePath() + ")");
            }
            System.exit(0);
        });
    }
    private void selectInstallationDirectory(String osName) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setCurrentDirectory(selectedDirectory);
        chooser.showOpenDialog(this);
        selectedDirectory = chooser.getSelectedFile();
        setDefaultDirectory();
        directoryDisplay.setText(selectedDirectory.getAbsolutePath());
        log("Set directory " + selectedDirectory.getAbsolutePath());
    }

    public static void log(String message) {
        logs += message + "\n";
    }
    public static void main(String[] args) {
        FormInstaller form = new FormInstaller();

    }
}
