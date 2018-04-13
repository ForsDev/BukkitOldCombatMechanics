package kernitus.plugin.OldCombatMechanics.module;

import kernitus.plugin.OldCombatMechanics.OCMMain;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class ModuleDisableCrafting extends Module {

    private List<Material> denied;

    public ModuleDisableCrafting(OCMMain plugin){
        super(plugin, "disable-crafting");
        reload();
    }

    @Override
    public void reload(){
        denied = module().getStringList("denied").stream()
                .map(Material::matchMaterial)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPrepareItemCraft(PrepareItemCraftEvent e){
        if(e.getViewers().size() < 1) return;

        World world = e.getViewers().get(0).getWorld();
        if(!isEnabled(world)) return;

        CraftingInventory inv = e.getInventory();
        ItemStack result = inv.getResult();

        if(result != null && denied.contains(result.getType()))
            inv.setResult(null);
    }
}