package com.bayoumi.models;

public class UpdateInfo {
    private String version;
    private String baseUrl;
    private String installerUrl;
    private String fileSize;
    private String filename;
    private String comment;

    public UpdateInfo(String version, String baseUrl, String installerUrl, String fileSize, String filename, String comment) {
        this.version = version;
        this.baseUrl = baseUrl;
        this.installerUrl = installerUrl;
        this.fileSize = fileSize;
        this.filename = filename;
        this.comment = comment;
    }

    @Override
    public String toString() {
        return "UpdateInfo{" +
                "version='" + version + '\'' +
                ", baseUrl='" + baseUrl + '\'' +
                ", installerUrl='" + installerUrl + '\'' +
                ", fileSize='" + fileSize + '\'' +
                ", filename='" + filename + '\'' +
                ", comment='" + comment + '\'' +
                '}';
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getInstallerUrl() {
        return installerUrl;
    }

    public void setInstallerUrl(String installerUrl) {
        this.installerUrl = installerUrl;
    }

    public String getFileSize() {
        return fileSize;
    }

    public void setFileSize(String fileSize) {
        this.fileSize = fileSize;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
