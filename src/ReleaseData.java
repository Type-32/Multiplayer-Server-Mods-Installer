import java.util.Objects;

class ReleaseData {
    public String versionTitleName = "";
    public String versionTagName = "";
    public String zipballURL = "";
    public String originalURL = "";

    public ReleaseData(String versionTitleName, String versionTagName, String zipballURL, String originalURL) {
        this.versionTitleName = versionTitleName;
        this.versionTagName = versionTagName;
        this.zipballURL = zipballURL;
        this.originalURL = originalURL;
    }

    @Override
    public String toString() {
        return new String(versionTitleName + " (" + versionTagName + ")");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ReleaseData)) return false;
        ReleaseData that = (ReleaseData) o;
        return Objects.equals(versionTagName, that.versionTagName) && Objects.equals(zipballURL, that.zipballURL) && Objects.equals(originalURL, that.originalURL);
    }
}