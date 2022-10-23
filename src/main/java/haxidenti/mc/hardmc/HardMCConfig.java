package haxidenti.mc.hardmc;

import com.google.gson.Gson;

import java.io.*;
import java.util.HashMap;
import java.util.Optional;

public class HardMCConfig {
    public HashMap<String, PlayerData> playerData = new HashMap<>();

    public PlayerData getPlayerData(HardMC plugin, String playerUid) {
        // Get player data otherwise create new one
        return Optional.of(playerData.get(playerUid)).orElseGet(() -> {
            PlayerData dat = new PlayerData();
            playerData.put(playerUid, dat);
            return dat;
        });
    }

    public void saveConfig(HardMC plugin) throws IOException {
        File dataFolder = plugin.getDataFolder();
        dataFolder.mkdir(); // If not exist - create
        File file = new File(dataFolder, "data.json");
        Gson gson = new Gson();
        FileWriter writer = new FileWriter(file);
        gson.toJson(this, writer);
        writer.close();
    }

    public static HardMCConfig loadConfig(HardMC plugin) throws FileNotFoundException {
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.isDirectory()) return new HardMCConfig();
        Gson gson = new Gson();
        FileReader reader = new FileReader(new File(dataFolder, "data.json"));
        return gson.fromJson(reader, HardMCConfig.class);
    }
}
