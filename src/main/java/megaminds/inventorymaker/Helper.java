package megaminds.inventorymaker;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.screen.ScreenHandlerType;

public class Helper {
	private Helper() {}

	private static final Object2IntMap<ScreenHandlerType<?>> SIZE_MAP;
	static {
		var sizeMap = new Object2IntOpenHashMap<ScreenHandlerType<?>>();
		sizeMap.put(ScreenHandlerType.GENERIC_9X1, 9);
		sizeMap.put(ScreenHandlerType.GENERIC_9X2, 18);
		sizeMap.put(ScreenHandlerType.GENERIC_9X3, 27);
		sizeMap.put(ScreenHandlerType.GENERIC_9X4, 36);
		sizeMap.put(ScreenHandlerType.GENERIC_9X5, 45);
		sizeMap.put(ScreenHandlerType.GENERIC_9X6, 54);
		sizeMap.put(ScreenHandlerType.GENERIC_3X3, 9);
		sizeMap.put(ScreenHandlerType.CRAFTER_3X3, 10);
		sizeMap.put(ScreenHandlerType.ANVIL, 3);
		sizeMap.put(ScreenHandlerType.BEACON, 1);
		sizeMap.put(ScreenHandlerType.BLAST_FURNACE, 3);
		sizeMap.put(ScreenHandlerType.BREWING_STAND, 5);
		sizeMap.put(ScreenHandlerType.CRAFTING, 10);
		sizeMap.put(ScreenHandlerType.ENCHANTMENT, 2);
		sizeMap.put(ScreenHandlerType.FURNACE, 3);
		sizeMap.put(ScreenHandlerType.GRINDSTONE, 3);
		sizeMap.put(ScreenHandlerType.HOPPER, 5);
		sizeMap.put(ScreenHandlerType.LECTERN, 1);
		sizeMap.put(ScreenHandlerType.LOOM, 4);
		sizeMap.put(ScreenHandlerType.MERCHANT, 3);
		sizeMap.put(ScreenHandlerType.SHULKER_BOX, 27);
		sizeMap.put(ScreenHandlerType.SMITHING, 3);
		sizeMap.put(ScreenHandlerType.SMOKER, 3);
		sizeMap.put(ScreenHandlerType.CARTOGRAPHY_TABLE, 3);
		sizeMap.put(ScreenHandlerType.STONECUTTER, 2);
		SIZE_MAP = Object2IntMaps.unmodifiable(sizeMap);
	}

	public static int getSize(ScreenHandlerType<?> type) {
		return SIZE_MAP.getInt(type);
	}

	public static int find(String s, char c) {
		var lastIndex = -1;
		var index = s.indexOf(c);
		var open = false;
		while (index != -1) {
			if (index == 0 || s.charAt(index-1) != '\\' || index > 1 && s.charAt(index-2) == '\\') {
				open = !open;
			}
			lastIndex = index;
			index = s.indexOf(c, index+1);
		}
		return open ? lastIndex : -1;
	}

	public static String[] split(String s, int i) {
		return new String[] {s.substring(0, i), s.substring(i)};
	}

	public static String stripQuotes(String s) {
		if (s.isEmpty() || "\"".equals(s)) return "";
		var start = s.startsWith("\"") ? 1 : 0;
		var end = s.endsWith("\"") ? 1 : 0;
		return s.substring(start, s.length() - end);
	}
}
