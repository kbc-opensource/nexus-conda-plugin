package be.kbc.eap.nexus.datastore.internal;

import com.google.gson.*;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class CondaModel {

    private String architecture;
    private String buildNumber;
    private String license;
    private String licenseFamily;
    private String platform;
    private String subdir;
    private List<String> depends;

    static String getJsonAttribute(JsonObject json, String attribute, String defaultValue) {
        if(json.has(attribute) && !json.get(attribute).isJsonNull()) {
            return json.get(attribute).getAsString();
        }
        return defaultValue;
    }
    static Optional<CondaModel> fromIndexJson(String indexJson) {

        if(indexJson == null) {
            return Optional.empty();
        }

        Gson gson = new Gson();

        CondaModel model = new CondaModel();

        JsonObject indexRoot = gson.fromJson(indexJson, JsonObject.class);
        model.setArchitecture(getJsonAttribute(indexRoot, "arch", "noarch"));
        model.setBuildNumber(getJsonAttribute(indexRoot, "build_number", ""));
        model.setLicense(getJsonAttribute(indexRoot, "license", ""));
        model.setLicenseFamily(getJsonAttribute(indexRoot, "license_family", ""));
        model.setPlatform(getJsonAttribute(indexRoot, "platform", "UNKNOWN"));
        model.setSubdir(getJsonAttribute(indexRoot, "subdir", ""));

        JsonArray depends = indexRoot.get("depends").getAsJsonArray();
        List<String> sDepends = new ArrayList<>();

        for(JsonElement depend : depends) {
            sDepends.add(depend.getAsString());
        }

        model.setDepends(sDepends);

        return Optional.of(model);
    }


    public List<String> getDepends() {
        return depends;
    }

    public void setDepends(List<String> depends) {
        this.depends = depends;
    }

    public String getArchitecture() {
        return architecture;
    }

    public void setArchitecture(String architecture) {
        this.architecture = architecture;
    }

    public String getBuildNumber() {
        return buildNumber;
    }

    public void setBuildNumber(String buildNumber) {
        this.buildNumber = buildNumber;
    }

    public String getLicense() {
        return license;
    }

    public void setLicense(String license) {
        this.license = license;
    }

    public String getLicenseFamily() {
        return licenseFamily;
    }

    public void setLicenseFamily(String licenseFamily) {
        this.licenseFamily = licenseFamily;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public String getSubdir() {
        return subdir;
    }

    public void setSubdir(String subdir) {
        this.subdir = subdir;
    }
}
