package megaminds.inventorymaker;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InventoryMaker implements ModInitializer {
	public static final String MODID = "inventorymaker";
	public static final Logger LOGGER = LoggerFactory.getLogger(MODID);

	private static final Map<Identifier, SavableInventory> INVENTORIES = new HashMap<>();
	private static MinecraftServer server;

	@Override
	@SuppressWarnings("java:S2696")
	public void onInitialize() {
		ServerLifecycleEvents.SERVER_STARTED.register(s -> {
			server = s;
			InventoryLoader.list().forEach(i->INVENTORIES.putIfAbsent(i, null));
		});
		ServerLifecycleEvents.SERVER_STOPPED.register(s -> {
			INVENTORIES.clear();
			server = null;
		});

		PlaceHolderHelper.register();
		CommandRegistrationCallback.EVENT.register(Commands::register);
	}

	public static void addInventory(SavableInventory inventory) {
		INVENTORIES.put(inventory.getId(), inventory);
	}

	public static SavableInventory getInventory(Identifier id) {
		return INVENTORIES.computeIfAbsent(id, InventoryLoader::load);
	}

	public static SavableInventory removeInventory(Identifier id) {
		return INVENTORIES.remove(id);
	}

	public static Set<Identifier> listInventories() {
		return Set.copyOf(INVENTORIES.keySet());
	}

	public static MinecraftServer getServer() {
		return server;
	}
}