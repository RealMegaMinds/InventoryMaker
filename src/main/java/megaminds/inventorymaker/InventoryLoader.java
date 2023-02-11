package megaminds.inventorymaker;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.minecraft.nbt.NbtIo;
import net.minecraft.util.Identifier;
import net.minecraft.util.WorldSavePath;

public class InventoryLoader {
	private static final Gson GSON = new GsonBuilder()
			.setPrettyPrinting()
			.create();

	private InventoryLoader() {}

	public static SavableInventory load(Identifier id) {
		var path = getPath(id);
		if (!Files.exists(path)) {
			return null;
		}

		try (var in = Files.newInputStream(path)) {
			var data = NbtIo.readCompressed(in);
			return SavableInventory.load(data);
		} catch (IOException e) {
			e.printStackTrace();
			InventoryMaker.LOGGER.warn("Error loading {}.", id);
			return null;
		}
	}

	public static void save(SavableInventory inventory) {
		var path = getPath(inventory.getId());

		try (var out = Files.newOutputStream(path)) {
			NbtIo.writeCompressed(inventory.save(), out);
		} catch (IOException e) {
			e.printStackTrace();
			InventoryMaker.LOGGER.warn("Error saving {}. Inventory contents: {}", inventory.getId(), GSON.toJson(inventory));
		}
	}

	public static void delete(Identifier id) {
		var path = getPath(id);
		try {
			Files.deleteIfExists(path);
		} catch (IOException e) {
			e.printStackTrace();
			InventoryMaker.LOGGER.warn("Error deleting {}.", id);
		}
	}

	private static Path getPath(Identifier id) {
		var dir = InventoryMaker.getServer().getSavePath(WorldSavePath.ROOT).normalize().resolve(InventoryMaker.MODID).resolve(id.getNamespace());
		try {
			Files.createDirectories(dir.getParent());
		} catch (IOException e) {
			e.printStackTrace();
			InventoryMaker.LOGGER.warn("Error creating {} directory.", dir);
		}
		return dir.resolve(id.getPath()+".dat");
	}
}
