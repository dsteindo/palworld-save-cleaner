package dsteindo.palworld.savecleaner;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class App {
    public static void main(String[] args) throws Exception {
        String basePath = System.getenv("APPDATA") + "/../Local/Pal/Saved/SaveGames";
        String steamId = getSteamId(args, basePath);
        String worldId = getWorldId(args, basePath + '/' + steamId);

        ObjectMapper mapper = new ObjectMapper();
        Path inputPath = Paths.get(basePath, steamId, worldId, "Level.sav.json");
        JsonNode obj = mapper.readValue(inputPath.toFile(), JsonNode.class);

        JsonNode worldData = obj.get("properties").get("worldSaveData").get("value");

        List<String> palInstanceIds = getPalInstanceIds(worldData);

        removeAssignedPals(worldData, palInstanceIds);

        palInstanceIds = deleteUnassignedParameters(worldData, palInstanceIds);

        deleteUnassignedGroupSaveData(worldData, palInstanceIds);

        resetRespawnTimers(worldData);

        Path outputPath = Paths.get(basePath, steamId, worldId, "Level.sav.modified.json");
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
        if (args.length > 0 && !args[0].isEmpty()) {
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
        throw new IllegalStateException("No steam id provided/resolved");
    }

    private static String getWorldId(String[] args, String saveFolderPath) {
        if (args.length > 1 && !args[1].isEmpty()) {
            return args[1];
        }
        File[] files = Paths.get(saveFolderPath).toFile().listFiles();
        for (int i = 0; i < files.length; i++) {
            File file = files[i];
            if (file.isDirectory()) {
                System.out.println("Fallback to world id: " + file.getName());
                return file.getName();
            }
        }
        throw new IllegalStateException("No world id provided/resolved");
    }

    private static void resetRespawnTimers(JsonNode worldData) {
        JsonNode entries = worldData.get("MapObjectSpawnerInStageSaveData").get("value");
        int count = 0;
        for (JsonNode entry : entries) {
            JsonNode instances = entry.get("value").get("SpawnerDataMapByLevelObjectInstanceId").get("value");
            for (JsonNode instance : instances) {
                JsonNode items = instance.get("value").get("ItemMap").get("value");
                for (JsonNode item : items) {
                    JsonNode lotteryTime = item.get("value").get("NextLotteryGameTime");
                    long time = lotteryTime.get("value").asLong();
                    if (time > 0) {
                        count++;
                        ((ObjectNode)lotteryTime).put("value", 0);
                    }
                }
            }
        }
        System.out.println("Reset timer of objects: " + count);
    }
}
