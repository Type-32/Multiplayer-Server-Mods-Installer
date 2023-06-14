import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import jdk.nashorn.internal.runtime.Version;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.lang.reflect.Array;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;

public class VersionControlFetcher {
    protected ConfigInstance config;
    protected Component windowComp;
    public ArrayList<ReleaseData> releaseDataArrayList = new ArrayList<ReleaseData>();
    public VersionControlFetcher(ConfigInstance config, Component windowComp) {
        this.config = config;
        this.windowComp = windowComp;
    }
    public ArrayList<ReleaseData> fetch() throws IOException {
        releaseDataArrayList.clear();
        String url = "https://gitlab.com/api/v4/projects/" + config.GITLAB_PROJECT_ID + "/releases";

        try {
            URL urlObj = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) urlObj.openConnection();

            connection.setRequestMethod("GET");
            connection.setRequestProperty("PRIVATE-TOKEN", config.GITLAB_PERSONAL_ACCESS_TOKEN);

            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            System.out.println(response.toString());
            JsonArray releases = new Gson().fromJson(response.toString(), JsonArray.class);
            if(releases.isEmpty() || releases.isJsonNull()) {
                FormInstaller.log("Failed to fetch releases from GitLab");
                JOptionPane.showConfirmDialog(windowComp, "Failed to fetch releases from GitLab", "Error", JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
            for (JsonElement release : releases) {
                JsonObject releaseObj = release.getAsJsonObject();
                String name = releaseObj.get("name").getAsString(), tagName = releaseObj.get("tag_name").getAsString(), assets = releaseObj.get("assets").getAsJsonObject().get("sources").getAsJsonArray().get(0).getAsJsonObject().get("url").getAsString();
                System.out.println("name: " + name + ", tagName: " + tagName + ", assets: " + assets);
                releaseDataArrayList.add(new ReleaseData(name, tagName, assets, url, tagName.toLowerCase().contains("fabric") ? ModLoaderType.Fabric : tagName.toLowerCase().contains("forge") ? ModLoaderType.Forge : tagName.toLowerCase().contains("liteloader") ? ModLoaderType.LiteLoader : tagName.toLowerCase().contains("quilt") ? ModLoaderType.Quilt : ModLoaderType.None));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return releaseDataArrayList;
    }
    public File downloadRelease(ReleaseData data, File parentFilePath) throws IOException {
        FormInstaller.log("Try fetching latest source code zip file release from GitLab");
        String downloadUrl = data.zipballURL;
        String version = data.versionTagName;
        FormInstaller.log("Retrieved downloadUrl from master " + data.originalURL);
        FormInstaller.log("Retrieved version tag " + version);

        FormInstaller.log("Try downloading source code zip file from retrieved " + downloadUrl);
        File zipFile = new File(parentFilePath, "source.zip");

        try {
            downloadZipFile(downloadUrl, zipFile.getAbsolutePath());
        } catch (Exception ex) {
            FormInstaller.log("Failed to download source code zip file with error " + ex.getMessage());
            JOptionPane.showMessageDialog(windowComp,"An error occurred while downloading the source code zip file: " + ex.getMessage());
            return null;
        }
        FormInstaller.log("Successfully downloaded source code zip file to " + zipFile.getAbsolutePath());
        return zipFile;
    }
    private void downloadZipFile(String zipballURL, String filePath) {

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
}
