public enum ModLoaderType {
    Fabric("fabric"),
    Forge("forge"),
    Quilt("quilt"),
    LiteLoader("liteloader"),
    All("all"),
    None("none");
    public final String label;

    private ModLoaderType(String label) {
        this.label = label;
    }
    public static ModLoaderType fromLabel(String label) {
        for (ModLoaderType type : ModLoaderType.values()) {
            if (type.label.equals(label)) {
                return type;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        String n = super.toString();
        n.toCharArray()[0] = Character.toUpperCase(n.toCharArray()[0]);
        return n;
    }
}
