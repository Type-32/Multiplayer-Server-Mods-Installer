public class ConfigInstance {
    public String GITLAB_PERSONAL_ACCESS_TOKEN = "";
    public String GITLAB_PROJECT_ID = "";
    public String EXTRACTED_FOLDER_PREFIX = "";
    public ConfigInstance(String pat, String pid, String exfp){
        GITLAB_PERSONAL_ACCESS_TOKEN = pat;
        GITLAB_PROJECT_ID = pid;
        EXTRACTED_FOLDER_PREFIX = exfp;
    }
}
