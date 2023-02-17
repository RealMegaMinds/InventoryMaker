package megaminds.inventorymaker;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.jetbrains.annotations.Nullable;

import eu.pb4.placeholders.api.PlaceholderContext;
import eu.pb4.placeholders.api.Placeholders;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.InventoryChangedListener;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.loot.condition.LootCondition;
import net.minecraft.loot.context.LootContext.Builder;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.loot.context.LootContextTypes;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerFactory;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class SavableInventory extends SimpleInventory {
	private static final String TYPE_KEY = "type";
	private static final String ID_KEY = "id";
	private static final String TITLE_KEY = "title";
	private static final String CHECKER_KEY = "checker";

	private static final InventoryChangedListener LISTENER = inv->InventoryLoader.save((SavableInventory)inv);

	private final ScreenHandlerType<?> type;
	private final Identifier id;
	private Text title;

	@Nullable
	private Identifier checker;

	private final Map<ServerPlayerEntity, ScreenHandler> currentPlayers;

	public SavableInventory(ScreenHandlerType<?> type, Identifier id, Text title) {
		super(Helper.getSize(type));
		addListener(LISTENER);
		this.type = Objects.requireNonNull(type);
		this.id = Objects.requireNonNull(id);
		this.title = Objects.requireNonNullElseGet(title, Text::empty);
		this.currentPlayers = new HashMap<>();
	}

	@Override
	public void onOpen(PlayerEntity player) {
		currentPlayers.put((ServerPlayerEntity)player, player.currentScreenHandler);
		super.onOpen(player);
	}

	@Override
	public void onClose(PlayerEntity player) {
		super.onClose(player);
		currentPlayers.remove(player);
	}

	public void open(ServerPlayerEntity player) {
		LootCondition check;
		if (checker == null || (check = InventoryMaker.getServer().getPredicateManager().get(checker)) == null || check.test(new Builder(player.getWorld())
				.parameter(LootContextParameters.ORIGIN, player.getPos())
				.optionalParameter(LootContextParameters.THIS_ENTITY, player)
				.build(LootContextTypes.COMMAND))) {
			var parsedTitle = Placeholders.parseText(title, PlaceholderContext.of(player));
			player.openHandledScreen(new SimpleNamedScreenHandlerFactory(getFactory(), parsedTitle));
		}
	}

	/**
	 * Closes this inventory for all players.
	 */
	public void close() {
		for (var e : currentPlayers.entrySet()) {
			if (e.getKey().currentScreenHandler.equals(e.getValue())) {
				e.getKey().closeHandledScreen();
			}
		}
		currentPlayers.clear();
	}

	public void setChecker(Identifier checker) {
		this.checker = checker;
		markDirty();
	}

	public void setTitle(Text title) {
		this.title = Objects.requireNonNullElseGet(title, Text::empty);
		markDirty();
	}

	@SuppressWarnings("java:S1452")
	public ScreenHandlerType<?> getType() {
		return type;
	}

	public Identifier getId() {
		return id;
	}

	public Text getTitle() {
		return title;
	}

	public Identifier getChecker() {
		return checker;
	}

	protected NbtCompound save() {
		var data = new NbtCompound();
		data.putString(TYPE_KEY, Registries.SCREEN_HANDLER.getId(type).toString());
		data.putString(ID_KEY, id.toString());
		data.putString(TITLE_KEY, Text.Serializer.toJson(title));
		if (checker != null) data.putString(CHECKER_KEY, checker.toString());
		Inventories.writeNbt(data, stacks);
		return data;
	}

	protected static SavableInventory load(NbtCompound data) {
		var type = Objects.requireNonNull(Registries.SCREEN_HANDLER.get(new Identifier(data.getString(TYPE_KEY))));
		var id = new Identifier(data.getString(ID_KEY));
		var title = Objects.requireNonNullElseGet(Text.Serializer.fromJson(data.getString(TITLE_KEY)), Text::empty);
		var checker = data.contains(CHECKER_KEY) ? new Identifier(data.getString(CHECKER_KEY)) : null;

		var inv = new SavableInventory(type, id, title);
		inv.checker = checker;
		Inventories.readNbt(data, inv.stacks);
		return inv;
	}

	private ScreenHandlerFactory getFactory() {
		return (syncId, playerInv, player) -> new ServerScreenHandler(type, syncId, this, playerInv);
	}
}
