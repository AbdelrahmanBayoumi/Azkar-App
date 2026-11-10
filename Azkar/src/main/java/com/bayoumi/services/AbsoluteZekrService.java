package com.bayoumi.services;

import com.bayoumi.models.azkar.AbsoluteZekr;
import com.bayoumi.models.azkar.AbsoluteZekrDTO;
import com.bayoumi.models.azkar.AbsoluteZekrExtend;
import com.bayoumi.storage.preferences.Preferences;
import com.bayoumi.storage.preferences.PreferencesType;
import com.bayoumi.util.Logger;
import com.bayoumi.util.VersionComparator;
import com.bayoumi.util.web.WebUtilities;
import com.fasterxml.jackson.core.type.TypeReference;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.bayoumi.models.azkar.AbsoluteZekrExtend.absoluteZekrObservableListJSON;

public class AbsoluteZekrService {
    private static final String USER_AND_REPO = "sharafabacery/absolute_zekr_azkar_db";
    public static void init() {
        try {
            scheduleUpdateCheck();
        } catch (Exception e) {
            Logger.error("IOException during initialization", e, AbsoluteZekrDTO.class.getName() + ".init()");
        }
    }
    private static void checkForUpdateSilent() throws Exception {
        final String latestVersion = WebUtilities.getLatestVersion("https://api.github.com/repos/" + USER_AND_REPO + "/releases");
        final String storedVersion = Preferences.getInstance().get(PreferencesType.ABSOLUTE_AZKAR_DATA_VERSION);
        boolean baseVersion=VersionComparator.baseVersion(storedVersion);
         if (VersionComparator.isNewerVersion(latestVersion, storedVersion)) {
            Logger.debug("[AbsoluteAzkarService] New version available: " + latestVersion);
             List<AbsoluteZekrDTO>latestVerionList=downloadVersionReleaseFilesSilent(latestVersion);
             List<AbsoluteZekrDTO>storedVerionList=new ArrayList<>();
             if(!baseVersion){
                 storedVerionList=downloadVersionReleaseFilesSilent(storedVersion);
             }
             List<AbsoluteZekrDTO> diffList=diffrenceList(latestVerionList,storedVerionList);
             conflictResolution(diffList);
            Preferences.getInstance().set(PreferencesType.ABSOLUTE_AZKAR_DATA_VERSION, latestVersion);
        } else {
            Logger.debug("[AbsoluteAzkarService] No new version available.");
        }
    }
    private static List<AbsoluteZekrDTO>downloadVersionReleaseFilesSilent(String version) throws Exception {
        Logger.debug("[AbsoluteAzkarService] Downloading ");
        List<AbsoluteZekrDTO> azkarsJSON=WebUtilities.getDeserializeResponse("https://github.com/" + USER_AND_REPO + "/releases/download/" + version + "/" +"absolute_zekr.json"
                ,new TypeReference<List<AbsoluteZekrDTO>>() {});
        return  azkarsJSON;
    }
    private static List<AbsoluteZekrDTO> diffrenceList(List<AbsoluteZekrDTO> latestVerionList,List<AbsoluteZekrDTO>storedVerionList){
        List<AbsoluteZekrDTO> diff = latestVerionList.stream()
                .filter(zekr -> !storedVerionList.contains(zekr))
                .collect(Collectors.toList());
        return  diff;
    }
    private static void conflictResolution(List<AbsoluteZekrDTO> azkarsJSON) throws Exception {

        Logger.debug("[AbsoluteAzkarService] Downloading ");
        List<AbsoluteZekr> candidateUpdates=new ArrayList<>();
        AbsoluteZekrExtend.fetchDataConflictResolution();
        absoluteZekrObservableListJSON.forEach((e)->{
            AbsoluteZekrDTO candidate=new AbsoluteZekrDTO();
            candidate.setZekr(e.getText());
            int matchedZekr = azkarsJSON.indexOf(candidate);
            if (matchedZekr>-1){
                AbsoluteZekr zekr=new AbsoluteZekr(e.getId(),e.getText(),azkarsJSON.get(matchedZekr).getUuid());
                candidateUpdates.add(zekr);
                azkarsJSON.remove(matchedZekr);
            }
        });
        AbsoluteZekrExtend.update(candidateUpdates);
        candidateUpdates.clear();
        azkarsJSON.forEach((e)->{
            AbsoluteZekr zekr = new AbsoluteZekr(0, e.getZekr().trim(),e.getUuid());
            candidateUpdates.add(zekr);
        });
        AbsoluteZekrExtend.insertBatched(candidateUpdates);
        AbsoluteZekr.fetchData();
        absoluteZekrObservableListJSON.clear();
        Logger.debug("[AbsoluteAzkarService] Downloaded ");
    }
    private static void scheduleUpdateCheck() {
        new java.util.Timer().schedule(
                new java.util.TimerTask() {
                    @Override
                    public void run() {
                        try {
                            checkForUpdateSilent();
                        } catch (IOException e) {
                            Logger.error("IOException during update check", e, AbsoluteZekrService.class.getName() + ".scheduleUpdateCheck()");
                        } catch (Exception e) {
                            Logger.error("Exception during update check", e, AbsoluteZekrService.class.getName() + ".scheduleUpdateCheck()");
                        }
                    }
                },
                1000 * 60 * 15 // 15 minutes
        );
    }
}
