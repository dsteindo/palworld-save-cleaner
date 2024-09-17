package dsteindo.palworld.savecleaner;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class App {
    public static void main(String[] args) throws Exception {
        String basePath = System.getenv("APPDATA") + "\\..\\Local\\Pal\\Saved\\SaveGames";
        String steamId = getSteamId(args, basePath);
        List<String> worldIds = getWorldIds(args, basePath + '\\' + steamId);

        for (String worldId : worldIds) {
            Path worlPath = Paths.get(basePath, steamId, worldId);
            handleInternal(worlPath);
        }
    }

    private static void handleInternal(Path worldPath) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        String fileName = "Level.sav.json";
        File inputFile = worldPath.resolve(fileName).toFile();

        if (!inputFile.exists()) {
            Path worldId = worldPath.getFileName();
            System.out.println("Input file \"" + fileName + "\" does not exist in world " + worldId + ", skipping ...");
            return;
        }

        JsonNode obj = mapper.readValue(inputFile, JsonNode.class);

        JsonNode worldData = obj.get("properties").get("worldSaveData").get("value");

        List<String> palInstanceIds = getPalInstanceIds(worldData);

        removeAssignedPals(worldData, palInstanceIds);

        palInstanceIds = deleteUnassignedParameters(worldData, palInstanceIds);

        deleteUnassignedGroupSaveData(worldData, palInstanceIds);

        resetRespawnTimers(worldData);

        exportPalParameters(worldPath, worldData);
        importPalParameters(worldPath, worldData);

        Path outputPath = worldPath.resolve("Level.sav.modified.json");
        mapper.writeValue(outputPath.toFile(), obj);
    }

    private static List<String> getPalInstanceIds(JsonNode worldData) {
        List<String> palInstanceIds = new ArrayList<>();
        for (JsonNode group : worldData.get("GroupSaveDataMap").get("value")) {
            JsonNode rawData = group.get("value").get("RawData").get("value");
            for (JsonNode handle : rawData.get("individual_character_handle_ids")) {
                String instanceId = handle.get("instance_id").asText();
                palInstanceIds.add(instanceId);
            }
        }
        System.out.println("Pal instances found: " + palInstanceIds.size());
        return palInstanceIds;
    }

    private static void removeAssignedPals(JsonNode worldData, List<String> palInstanceIds) {
        for (JsonNode container : worldData.get("CharacterContainerSaveData").get("value")) {
            JsonNode slots = container.get("value").get("Slots").get("value");
            for (JsonNode slot : slots.get("values")) {
                String instanceId = slot.get("RawData").get("value").get("instance_id").asText();
                palInstanceIds.remove(instanceId);
            }
        }
        System.out.println("Pal instances unassigned: " + palInstanceIds.size());
        // palInstanceIds.forEach(instanceId -> System.out.println(instanceId));
    }

    private static List<String> deleteUnassignedParameters(JsonNode worldData, List<String> unassigned) {
        List<String> pals = new ArrayList<>();
        Iterator<JsonNode> iterator = worldData.get("CharacterSaveParameterMap").get("value").iterator();
        while (iterator.hasNext()) {
            JsonNode parameter = iterator.next();
            String instanceId = parameter.get("key").get("InstanceId").get("value").asText();
            if (unassigned.contains(instanceId) && !isPlayer(parameter)) {
                System.out.println(instanceId);
                iterator.remove();
                pals.add(instanceId);
            }
        }
        System.out.println("Pal parameters removed: " + pals.size());
        return pals;
    }

    private static boolean isPlayer(JsonNode parameter) {
        JsonNode objectRef = parameter.get("value").get("RawData").get("value").get("object");
        return objectRef.get("SaveParameter").get("value").has("IsPlayer");
    }

    private static void deleteUnassignedGroupSaveData(JsonNode worldData, List<String> unassigned) {
        int deleteCount = 0;
        for (JsonNode group : worldData.get("GroupSaveDataMap").get("value")) {
            JsonNode rawData = group.get("value").get("RawData").get("value");
            Iterator<JsonNode> iterator = rawData.get("individual_character_handle_ids").iterator();
            while (iterator.hasNext()) {
                JsonNode handle = iterator.next();
                String instanceId = handle.get("instance_id").asText();
                if (unassigned.contains(instanceId)) {
                    // System.out.println(instanceId);
                    deleteCount++;
                    iterator.remove();
                }
            }
        }
        System.out.println("Pal group save data removed: " + deleteCount);
    }

    private static String getSteamId(String[] args, String basePath) {
        System.out.println("Steam base path used: " + basePath);
        if (args.length > 0 && !args[0].isBlank()) {
            return args[0];
        }
        File[] files = Paths.get(basePath).toFile().listFiles();
        for (int i = 0; i < files.length; i++) {
            File file = files[i];
            if (file.isDirectory()) {
                System.out.println("Fallback to steam id: " + file.getName());
                return file.getName();
            }
        }
        throw new IllegalStateException("No steam id resolved");
    }

    private static List<String> getWorldIds(String[] args, String saveFolderPath) {
        if (args.length > 1 && !args[1].isBlank()) {
            String fileName = args[1];
            return Collections.singletonList(fileName);
        }
        File[] files = Paths.get(saveFolderPath).toFile().listFiles(pathname -> pathname.isDirectory());
        if (files.length > 0) {
            List<String> result = Arrays.stream(files).map(file -> file.getName()).toList();
            // System.out.println("Fallback to world ids: " + result);
            return result;
        }
        throw new IllegalStateException("No world id resolved");
    }

    private static void resetRespawnTimers(JsonNode worldData) {
        List<String> worldObjectIds = new ArrayList<>();
        resetWorldObjects(worldData, worldObjectIds);
        JsonNode entries = worldData.get("MapObjectSpawnerInStageSaveData").get("value");
        int count = 0;
        int removedInstancesCount = 0;
        for (JsonNode entry : entries) {
            Iterator<JsonNode> instances = entry.get("value").get("SpawnerDataMapByLevelObjectInstanceId").get("value").iterator();
            while (instances.hasNext()) {
                JsonNode instance = instances.next();
                boolean remove = true;
                JsonNode items = instance.get("value").get("ItemMap").get("value");
                for (JsonNode item : items) {
                    String objectInstanceId = item.get("value").get("MapObjectInstanceId").get("value").asText();
                    if (!worldObjectIds.contains(objectInstanceId)) {
                        continue;
                    }
                    remove = false;
                    JsonNode lotteryTime = item.get("value").get("NextLotteryGameTime");
                    long time = lotteryTime.get("value").asLong();
                    if (time > 0) {
                        count++;
                        ((ObjectNode) lotteryTime).put("value", 0);
                    }
                }
                if (remove) {
                    instances.remove();
                    removedInstancesCount++;
                }
            }
        }
        System.out.println("Reset timer of objects: " + count);
        System.out.println("Removed timer instances count: " + removedInstancesCount);
        resetBossTimers(worldData);
        resetEnemyCamps(worldData);
        if (worldData.has("DungeonPointMarkerSaveData")) {
            resetDungeons((ObjectNode) worldData);
        }
    }

    private static void resetBossTimers(JsonNode worldData) {
        ((ObjectNode) worldData).remove("BossSpawnerSaveData");
        System.out.println("Reset boss timers ...");
    }

    private static void resetEnemyCamps(JsonNode worldData) {
        ((ObjectNode) worldData).remove("EnemyCampSaveData");
        System.out.println("Reset enemy camps ...");
        ((ObjectNode) worldData).remove("OilrigSaveData");
        System.out.println("Reset oil rig ...");
        ((ObjectNode) worldData).remove("FoliageGridSaveDataMap");
        System.out.println("Reset foliage ...");
    }

    private static void resetDungeons(ObjectNode worldData) {
        worldData.remove("DungeonPointMarkerSaveData");
        worldData.remove("DungeonSaveData");
        System.out.println("Reset dungeons ...");
    }

    private static void resetWorldObjects(JsonNode worldData, List<String> worldObjectIds) {
        Iterator<JsonNode> iterator = worldData.get("MapObjectSaveData").get("value").get("values").iterator();
        int deleteCount = 0;
        while(iterator.hasNext()) {
            JsonNode worldObject = iterator.next();
            String objectName = worldObject.get("MapObjectId").get("value").asText();
            if (objectName.startsWith("PickupItem_") || objectName.startsWith("DamagableRock")) {
                iterator.remove();
                deleteCount++;
            }
            else {
                worldObjectIds.add(worldObject.get("MapObjectInstanceId").get("value").asText());
            }
        }
        System.out.println("World objects removed: " + deleteCount);
    }

    private static void exportPalParameters(Path worldPath, JsonNode worldData) throws Exception {
        File exportFile = worldPath.resolve("pal-parameters-export.json").toFile();
        ObjectMapper mapper = new ObjectMapper();
        mapper.writeValue(exportFile, worldData.get("CharacterSaveParameterMap"));
    }

    private static void importPalParameters(Path worldPath, JsonNode worldData) throws Exception {
        File importFile = worldPath.resolve("pal-parameters-import.json").toFile();
        if (!importFile.exists()) {
            return;
        }
        Map<String, JsonNode> importInstances = getImportPalParameters(importFile);
        JsonNode valueArray = worldData.get("CharacterSaveParameterMap").get("value");
        for (int index = 0; index < valueArray.size(); index++) {
            JsonNode parameter = valueArray.get(index);
            String instanceId = parameter.get("key").get("InstanceId").get("value").asText();
            JsonNode other = importInstances.get(instanceId);
            if (other != null && !other.equals(parameter) && !isPlayer(parameter)) {
                System.out.println("Pal parameter " + instanceId + " is different from import, overriding value ...");
                ((ArrayNode) valueArray).remove(index);
                ((ArrayNode) valueArray).insert(index, other);
            }
        }
    }

    private static Map<String, JsonNode> getImportPalParameters(File importFile) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode importNode = mapper.readValue(importFile, JsonNode.class);
        Map<String, JsonNode> result = new HashMap<>();
        for (JsonNode parameter : importNode.get("value")) {
            String instanceId = parameter.get("key").get("InstanceId").get("value").asText();
            result.put(instanceId, parameter);
        }
        return result;
    }
}
