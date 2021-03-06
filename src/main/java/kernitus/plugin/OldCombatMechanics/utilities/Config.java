package kernitus.plugin.OldCombatMechanics.utilities;

import java.io.InputStreamReader;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import kernitus.plugin.OldCombatMechanics.ModuleLoader;
import kernitus.plugin.OldCombatMechanics.OCMMain;
import kernitus.plugin.OldCombatMechanics.module.Module;
import kernitus.plugin.OldCombatMechanics.utilities.damage.EntityDamageByEntityListener;
import kernitus.plugin.OldCombatMechanics.utilities.damage.WeaponDamages;

/**
 * Created by Rayzr522 on 6/14/16.
 */

public class Config {

    private static OCMMain plugin;
    private static FileConfiguration config;
    private static Set<Material> interactive = Collections.emptySet();

    public static void initialise(OCMMain plugin){
        Config.plugin = plugin;
        config = plugin.getConfig();

        reload();
    }

    /**
     * @return Whether config was changed or not
     */
    private static boolean checkConfigVersion(){
        YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(plugin.getResource("config.yml")));

        if(config.getInt("config-version") != defaultConfig.getInt("config-version")){
            plugin.getLogger().warning("Config version does not match, backing up old config and creating a new one");
            plugin.upgradeConfig();
            reload();
            return true;
        }

        return false;
    }


    public static void reload(){
        if(plugin.doesConfigExist()){
            plugin.reloadConfig();
            config = plugin.getConfig();
        } else
            plugin.upgradeConfig();

        if(checkConfigVersion()){
            // checkConfigVersion will call #reload() again anyways
            return;
        }

        Messenger.DEBUG_ENABLED = config.getBoolean("debug.enabled");

        WeaponDamages.initialise(plugin); //Reload weapon damages from config

        // Load all interactive blocks (used by sword blocking and elytra modules)
        reloadInteractiveBlocks();

        //Set EntityDamagedByEntityListener to enabled if either of these modules is enabled
        EntityDamageByEntityListener.getINSTANCE().setEnabled(
                moduleEnabled("old-tool-damage") || moduleEnabled("old-potion-effects"));

        // Dynamically registers / unregisters all event listeners for optimal performance!
        ModuleLoader.toggleModules();

        ModuleLoader.getModules().stream()
                .filter(Module::isEnabled)
                .forEach(module -> {
                    try{
                        module.reload();
                    } catch(Exception e){
                        plugin.getLogger()
                                .log(Level.WARNING, "Error reloading module '" + module.toString() + "'", e);
                    }
                });
    }

    public static boolean moduleEnabled(String name, World world){
        ConfigurationSection section = config.getConfigurationSection(name);

        if(section == null){
            plugin.getLogger().warning("Tried to check module '" + name + "', but it didn't exist!");
            return false;
        }

        if(section.getBoolean("enabled")){
            if(world == null){
                return true;
            }

            List<String> list = section.getStringList("worlds");

            return list == null || list.size() <= 0 || list.stream().anyMatch(entry -> entry.equalsIgnoreCase(world.getName()));
        }

        return false;
    }

    public static boolean moduleEnabled(String name){
        return moduleEnabled(name, null);
    }

    public static boolean debugEnabled(){
        return moduleEnabled("debug", null);
    }

    public static List<?> getWorlds(String moduleName){
        return config.getList(moduleName + ".worlds");
    }

    public static boolean moduleSettingEnabled(String moduleName, String moduleSettingName){
        return config.getBoolean(moduleName + "." + moduleSettingName);
    }

    public static void setModuleSetting(String moduleName, String moduleSettingName, boolean value){
        config.set(moduleName + "." + moduleSettingName, value);
        plugin.saveConfig();
    }

    private static void reloadInteractiveBlocks(){
        interactive = EnumSet.copyOf(ConfigUtils.loadMaterialList(config, "interactive"));
    }

    public static Set<Material> getInteractiveBlocks(){
        return interactive;
    }

    /**
     * Only use if you can't access config through plugin instance
     *
     * @return config.yml instance
     */
    public static FileConfiguration getConfig(){
        return plugin.getConfig();
    }
}
