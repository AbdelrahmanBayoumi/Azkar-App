package com.bayoumi.util.web;

import com.bayoumi.util.Logger;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;

import java.io.File;

public class FileDownloader {
    public static boolean downloadFile(String fileURL, File destinationFile) {
        if (destinationFile == null || fileURL == null || fileURL.isEmpty()) {
            Logger.debug("Invalid file URL or destination file");
            return false;
        }
        try {
            HttpResponse<File> response = Unirest.get(fileURL).asFile(destinationFile.getPath());
            if (response.getStatus() == 200) {
                return true;
            } else {
                Logger.debug("Server returned non-OK status: " + response.getStatus());
                return false;
            }
        } catch (Exception e) {
            Logger.error(null, e, FileDownloader.class.getName() + ".downloadFile('" + fileURL + "', '" + destinationFile.getPath() + "')");
            return false;
        }
    }

}
