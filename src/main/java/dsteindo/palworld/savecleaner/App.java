package dsteindo.palworld.savecleaner;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class App {
    public static void main(String[] args) throws Exception {
        String steamId = "";
        String worldId = "1AB5E022419B663B446DA28C105FB01D";
        ObjectMapper mapper = new ObjectMapper();
        String appDataPath = System.getenv("APPDATA");
        Path inputPath = Paths.get(appDataPath + "/../Local/Pal/Saved/SaveGames", steamId, worldId, "Level.sav.json");
        JsonNode obj = mapper.readValue(inputPath.toFile(), JsonNode.class);

        JsonNode worldData = obj.get("properties").get("worldSaveData").get("value");

        List<String> palInstanceIds = getPalInstanceIds(worldData);

        removeAssignedPals(worldData, palInstanceIds);

        deleteUnassignedParameters(worldData, palInstanceIds);

        deleteUnassignedGroupSaveData(worldData, palInstanceIds);

        Path outputPath = Paths.get(appDataPath + "/../Local/Pal/Saved/SaveGames", steamId, worldId, "Level.sav.modified.json");
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
                if (palInstanceIds.contains(instanceId)) {
                    palInstanceIds.remove(instanceId);
                }
            }
        }
        System.out.println("Pal instances unassigned: " + palInstanceIds.size());
    }

    private static void deleteUnassignedParameters(JsonNode worldData, List<String> unassigned) {
        int deleteCount = 0;
        Iterator<JsonNode> iterator = worldData.get("CharacterSaveParameterMap").get("value").iterator();
        while (iterator.hasNext()) {
            JsonNode parameter = iterator.next();
            String instanceId = parameter.get("key").get("InstanceId").get("value").asText();
            if (unassigned.contains(instanceId)) {
                System.out.println(instanceId);
                deleteCount++;
                iterator.remove();
            }
        }
        System.out.println("Pal parameters removed: " + deleteCount);
    }

    private static void deleteUnassignedGroupSaveData(JsonNode worldData, List<String> unassigned) {
        int deleteCount = 0;
        for (JsonNode group : worldData.get("GroupSaveDataMap").get("value")) {
            JsonNode rawData = group.get("value").get("RawData").get("value");
            Iterator<JsonNode> iterator = rawData.get("individual_character_handle_ids").iterator();
            while (iterator.hasNext()) {
                JsonNode handle = iterator.next();
                String instanceId = handle.get("instance_id").asText();
                if (unassigned.contains(instanceId))
                {
                    System.out.println(instanceId);
                    deleteCount++;
                    iterator.remove();
                }
            }
        }
        System.out.println("Pal group save data removed: " + deleteCount);
    }
}
