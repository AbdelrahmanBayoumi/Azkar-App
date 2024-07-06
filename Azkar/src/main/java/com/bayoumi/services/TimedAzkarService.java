package com.bayoumi.services;

import com.bayoumi.controllers.dialog.DownloadResourcesController;
import com.bayoumi.models.azkar.TimedZekrDTO;
import com.bayoumi.models.preferences.Preferences;
import com.bayoumi.models.preferences.PreferencesType;
import com.bayoumi.models.settings.Language;
import com.bayoumi.util.Constants;
import com.bayoumi.util.Logger;
import com.bayoumi.util.Utility;
import com.bayoumi.util.VersionComparator;
import com.bayoumi.util.gui.load.Loader;
import com.bayoumi.util.gui.load.LoaderComponent;
import com.bayoumi.util.gui.load.Locations;
import com.bayoumi.util.web.WebUtilities;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class TimedAzkarService {

    private static final String FOLDER_NAME = "azkar";
    private static final String USER_AND_REPO = "Seen-Arabic/Morning-And-Evening-Adhkar-DB";
    private static final int SCHEDULE_DELAY_MS = 1000 * 60 * 15; // 15 minutes

    /**
     * Initialize the TimedAzkarService by copying the files to the assets path and checking for updates
     */
    public static void init() {
        try {
            copyAzkarFilesToAssetsPath();
            scheduleUpdateCheck();
        } catch (IOException e) {
            Logger.error("IOException during initialization", e, TimedAzkarService.class.getName() + ".init()");
        }
    }

    /**
     * Get the timed azkar from the file and filter them based on the type of Dhikr
     *
     * @param local : Language of the Dhikr (ar for Arabic, en for English)
     * @param type  : Type of Dhikr (0 for both morning and evening, 1 for morning only, 2 for evening only)
     * @return List of TimedZekrDTO objects
     * @throws IOException : If the URI is invalid or the file is not found or can't be read
     */
    public static List<TimedZekrDTO> getTimedAzkar(String local, int type) throws Exception {
        if (!checkIfAllFilesExist()) {
            throw new Exception("Files not found, please download the files first.");
        }
        TimedZekrDTO[] timedZekrDTOS = readTimedAzkarFromFile(local);
        return filterTimedAzkarByType(timedZekrDTOS, type);
    }

    public static void downloadLatestReleaseFiles() throws Exception {
        deleteFilesIfExists();
        final String latestVersion = WebUtilities.getLatestVersion("https://api.github.com/repos/" + USER_AND_REPO + "/releases");
        final LoaderComponent popUp = Loader.getInstance().getPopUp(Locations.DownloadResources);
        for (Language language : Language.values()) {
            ((DownloadResourcesController) popUp.getController())
                    .setData("https://github.com/" + USER_AND_REPO + "/releases/download/" + latestVersion + "/" + language.getLocale() + ".json",
                            Constants.assetsPath + "/" + FOLDER_NAME + "/" + language.getLocale() + ".json",
                            "timedAzkarErrorInDownload",
                            popUp.getStage(), null);
            popUp.showAndWait();
        }
        Preferences.getInstance().set(PreferencesType.TIMED_AZKAR_DATA_VERSION, latestVersion);
    }

    private static TimedZekrDTO[] readTimedAzkarFromFile(String local) throws IOException {
        final ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(Paths.get(Constants.assetsPath + "/" + FOLDER_NAME + "/" + local + ".json").toFile(), TimedZekrDTO[].class);
    }

    private static List<TimedZekrDTO> filterTimedAzkarByType(TimedZekrDTO[] timedZekrDTOS, int type) {
        final ArrayList<TimedZekrDTO> list = new ArrayList<>();
        for (TimedZekrDTO timedZekrDTO : timedZekrDTOS) {
            if (timedZekrDTO.getType() == 0 || timedZekrDTO.getType() == type) {
                list.add(timedZekrDTO);
            }
        }
        return list;
    }

    private static void checkForUpdateSilent() throws Exception {
        final boolean exists = checkIfAllFilesExist();
        final String latestVersion = WebUtilities.getLatestVersion("https://api.github.com/repos/" + USER_AND_REPO + "/releases");
        final String storedVersion = Preferences.getInstance().get(PreferencesType.TIMED_AZKAR_DATA_VERSION);

        if (!exists) {
            Logger.debug("[TimedAzkarService] Files not found, downloading latest release files.");
            downloadLatestReleaseFilesSilent(latestVersion);
        } else if (VersionComparator.isNewerVersion(latestVersion, storedVersion)) {
            Logger.debug("[TimedAzkarService] New version available: " + latestVersion);
            downloadLatestReleaseFilesSilent(latestVersion);
            Preferences.getInstance().set(PreferencesType.TIMED_AZKAR_DATA_VERSION, latestVersion);
        } else {
            Logger.debug("[TimedAzkarService] No new version available.");
        }
    }

    private static void copyAzkarFilesToAssetsPath() throws IOException {
        for (Language language : Language.values()) {
            final Path from = Paths.get("jarFiles" + "/" + FOLDER_NAME + "/" + language.getLocale() + ".json").toAbsolutePath();
            final Path to = Paths.get(Constants.assetsPath + "/" + FOLDER_NAME + "/" + language.getLocale() + ".json").toAbsolutePath();
            if (from.equals(to)) {
                Logger.debug("[TimedAzkarService] Skipping from: " + from + " to: " + to);
                break;
            }
            Logger.debug("[TimedAzkarService] Copying from: " + from + " to: " + to);
            Utility.copyIfNotExist(from, to);
        }
    }

    private static boolean checkIfAllFilesExist() {
        for (Language language : Language.values()) {
            if (!Files.exists(Paths.get(Constants.assetsPath + "/" + FOLDER_NAME + "/" + language.getLocale() + ".json"))) {
                return false;
            }
        }
        return true;
    }

    private static void deleteFilesIfExists() {
        for (Language language : Language.values()) {
            Path path = Paths.get(Constants.assetsPath + "/" + FOLDER_NAME + "/" + language.getLocale() + ".json");
            if (Files.exists(path)) {
                try {
                    Files.delete(path);
                } catch (IOException e) {
                    Logger.error(e.getLocalizedMessage(), e, TimedAzkarService.class.getName() + ".deleteFilesIfExists()");
                }
            }
        }
    }

    private static void downloadLatestReleaseFilesSilent(String version) throws Exception {
        deleteFilesIfExists();
        for (Language language : Language.values()) {
            Logger.debug("[TimedAzkarService] Downloading " + language.getLocale() + ".json");
            WebUtilities.downloadFile("https://github.com/" + USER_AND_REPO + "/releases/download/" + version + "/" + language.getLocale() + ".json"
                    , Constants.assetsPath + "/" + FOLDER_NAME + "/" + language.getLocale() + ".json", null);
            Logger.debug("[TimedAzkarService] Downloaded " + language.getLocale() + ".json");
        }
    }

    private static void scheduleUpdateCheck() {
        new java.util.Timer().schedule(
                new java.util.TimerTask() {
                    @Override
                    public void run() {
                        try {
                            checkForUpdateSilent();
                        } catch (IOException e) {
                            Logger.error("IOException during update check", e, TimedAzkarService.class.getName() + ".scheduleUpdateCheck()");
                        } catch (Exception e) {
                            Logger.error("Exception during update check", e, TimedAzkarService.class.getName() + ".scheduleUpdateCheck()");
                        }
                    }
                },
                SCHEDULE_DELAY_MS
        );
    }
}
