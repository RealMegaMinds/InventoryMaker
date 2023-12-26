package megaminds.inventorymaker;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtSizeTracker;
import net.minecraft.nbt.visitor.StringNbtWriter;
import net.minecraft.util.Identifier;
import net.minecraft.util.WorldSavePath;

public class InventoryLoader {
	private static final String EXT = ".dat";

	private InventoryLoader() {}

	public static List<Identifier> list() {
		var dirO = getDir();
		if (dirO.isEmpty()) Collections.emptyList();
		var dir = dirO.orElseThrow();

		var fs = FileSystems.getDefault();
		var sep = fs.getSeparator();
		var matcher = fs.getPathMatcher("glob:**"+EXT);
		try (var files = Files.find(dir, Integer.MAX_VALUE, (path, attributes)->attributes.isRegularFile() && matcher.matches(path))) {
			return files.map(p->{
				var pathStr = dir.relativize(p).toString();
				var splitIndex = pathStr.lastIndexOf(sep);
				return new Identifier(pathStr.substring(0, splitIndex), pathStr.substring(splitIndex+sep.length(), pathStr.lastIndexOf('.')));
			}).toList();
		} catch (IOException e) {
			e.printStackTrace();
			InventoryMaker.LOGGER.warn("Error listing files.");
			return Collections.emptyList();
		}
	}

	public static SavableInventory load(Identifier id) {
		var path = getPath(id);
		if (path.isEmpty() || !Files.exists(path.get())) {
			return null;
		}

		try (var in = Files.newInputStream(path.get())) {
			var data = NbtIo.readCompressed(in, NbtSizeTracker.ofUnlimitedBytes());
			return SavableInventory.load(data);
		} catch (IOException e) {
			e.printStackTrace();
			InventoryMaker.LOGGER.warn("Error loading {}.", id);
			return null;
		}
	}

	public static void save(SavableInventory inventory) {
		var path = getPath(inventory.getId());
		if (path.isEmpty()) return;

		var data = inventory.save();
		try (var out = Files.newOutputStream(path.get())) {
			NbtIo.writeCompressed(inventory.save(), out);
		} catch (IOException e) {
			e.printStackTrace();
			InventoryMaker.LOGGER.warn("Error saving {}. Inventory data: {}", inventory.getId(), new StringNbtWriter().apply(data));
		}
	}

	public static void delete(Identifier id) {
		getPath(id).ifPresent(path->{
			try {
				Files.deleteIfExists(path);
			} catch (IOException e) {
				e.printStackTrace();
				InventoryMaker.LOGGER.warn("Error deleting {}.", id);
			}
		});
	}

	private static Optional<Path> getPath(Identifier id) {
		return getDir().flatMap(p->createDir(p.resolve(id.getNamespace()))).map(p->p.resolve(id.getPath()+EXT));
	}

	private static Optional<Path> getDir() {
		return createDir(InventoryMaker.getServer().getSavePath(WorldSavePath.ROOT).normalize().resolve(InventoryMaker.MODID));
	}

	private static Optional<Path> createDir(Path dir) {
		try {
			return Optional.of(Files.createDirectories(dir));
		} catch (IOException e) {
			e.printStackTrace();
			InventoryMaker.LOGGER.warn("Error creating {} directory.", dir);
			return Optional.empty();
		}
	}
}
