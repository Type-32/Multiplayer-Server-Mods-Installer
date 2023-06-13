import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import jdk.nashorn.internal.runtime.Version;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class VersionControlFetcher {
    protected ConfigInstance config;
    public ArrayList<ReleaseData> releaseDataArrayList = new ArrayList<ReleaseData>();
    public VersionControlFetcher(ConfigInstance config) {
        this.config = config;
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
            for (JsonElement release : releases) {
                JsonObject releaseObj = release.getAsJsonObject();
                releaseDataArrayList.add(new ReleaseData(releaseObj.get("tag_name").getAsString(),releaseObj.get("assets").getAsJsonObject().get("sources").getAsJsonArray().get(0).getAsJsonObject().get("url").getAsString(),url));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return releaseDataArrayList;
    }
}
