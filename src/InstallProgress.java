import javax.swing.*;

public class InstallProgress {
    public JTextArea logsArea;
    public JPanel progressWindow;
    public JProgressBar installProgress;
    public JLabel windowTitle;
    public void setInstallProgress(float progress){
        installProgress.setValue(Float.floatToIntBits(progress));
    }
}
