package megaminds.inventorymaker;

import static com.mojang.brigadier.arguments.StringArgumentType.string;
import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static net.minecraft.command.argument.EntityArgumentType.getOptionalPlayers;
import static net.minecraft.command.argument.EntityArgumentType.players;
import static net.minecraft.command.argument.IdentifierArgumentType.getIdentifier;
import static net.minecraft.command.argument.IdentifierArgumentType.identifier;
import static net.minecraft.command.argument.RegistryEntryArgumentType.getRegistryEntry;
import static net.minecraft.command.argument.RegistryEntryArgumentType.registryEntry;
import static net.minecraft.command.argument.TextArgumentType.getTextArgument;
import static net.minecraft.command.argument.TextArgumentType.text;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;
import static net.minecraft.command.argument.ItemStackArgumentType.itemStack;
import static net.minecraft.command.argument.ItemStackArgumentType.getItemStackArgument;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;

import java.util.List;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import eu.pb4.placeholders.api.PlaceholderContext;
import eu.pb4.placeholders.api.Placeholders;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.CommandSource;
import net.minecraft.loot.LootDataType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.command.CommandManager.RegistrationEnvironment;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class Commands {
	private static final DynamicCommandExceptionType ALREADY_EXISTS_EXCEPTION = new DynamicCommandExceptionType(id->Text.literal("Another inventory with id: " + id + " exists."));
	private static final DynamicCommandExceptionType DOESNT_EXISTS_EXCEPTION = new DynamicCommandExceptionType(id->Text.literal("No inventory with id: " + id + " exists."));
	private static final DynamicCommandExceptionType INVALID_SLOT_EXCEPTION = new DynamicCommandExceptionType(slot->Text.literal("Invalid slot for this inventory: " + slot));

	private static final SuggestionProvider<ServerCommandSource> CREATE_ID_SUGGESTER = (context, builder) -> {
		PlaceHolderHelper.addPlaceHolderSuggestions(context, builder);
		return builder.buildFuture();
	};
	private static final SuggestionProvider<ServerCommandSource> ID_SUGGESTER = (context, builder) -> {
		InventoryMaker.listInventories().stream().map(id->"\""+id+"\"").forEach(builder::suggest);
		PlaceHolderHelper.addPlaceHolderSuggestions(context, builder);
		return builder.buildFuture();
	};
	private static final SuggestionProvider<ServerCommandSource> TYPE_SUGGESTER = (context, builder) -> CommandSource.suggestIdentifiers(Registries.SCREEN_HANDLER.stream().filter(h -> Helper.getStatus(h) == Helper.Status.IMPLEMENTED).map(Registries.SCREEN_HANDLER::getId), builder);
	private static final SuggestionProvider<ServerCommandSource> CHECK_SUGGESTER = (context, builder) -> CommandSource.suggestIdentifiers(context.getSource().getServer().getLootManager().getIds(LootDataType.PREDICATES), builder);

	private static final String ID_ARG = "id";
	private static final String CREATE_ARG = "create";
	private static final String TYPE_ARG = "type";
	private static final String TITLE_ARG = "title";
	private static final String EDIT_ARG = "edit";
	private static final String CHECKER_ARG = "checker";
	private static final String OPEN_ARG = "open";
	private static final String TARGET_ARG = "target";
	private static final String DELETE_ARG = "delete";
	private static final String SLOT_ARG = "slot";
	private static final String ITEM_ARG = "item";
	private static final String COUNT_ARG = "count";

	private Commands() {}

	@SuppressWarnings("java:S1172")
	public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, RegistrationEnvironment environment) {
		var root = literal(InventoryMaker.MODID)
				.requires(Permissions.require(InventoryMaker.MODID, true));

		var create = literal(CREATE_ARG)
				.requires(Permissions.require(InventoryMaker.MODID+'.'+CREATE_ARG, 2))
				.then(argument(TYPE_ARG, registryEntry(registryAccess, RegistryKeys.SCREEN_HANDLER))
						.suggests(TYPE_SUGGESTER)
						.then(argument(ID_ARG, string())
								.suggests(CREATE_ID_SUGGESTER)
								.executes(Commands::onCreate)
								.then(argument(TITLE_ARG, text())
										.executes(Commands::onCreate))));

		var edit = literal(EDIT_ARG)
				.requires(Permissions.require(InventoryMaker.MODID+'.'+EDIT_ARG, 2))
				.then(argument(ID_ARG, string())
						.suggests(ID_SUGGESTER)
						.then(literal(SLOT_ARG)
								.then(argument(SLOT_ARG, integer(0))
										.then(argument(ITEM_ARG, itemStack(registryAccess))
												.then(argument(COUNT_ARG, integer(1))
														.executes(Commands::onEditSlot)))))
						.then(literal(TITLE_ARG)
								.then(argument(TITLE_ARG, text())
										.executes(Commands::onEditTitle)))
						.then(literal(CHECKER_ARG)
								.then(argument(CHECKER_ARG, identifier())
										.suggests(CHECK_SUGGESTER)
										.executes(Commands::onEditChecker))));

		var open = literal(OPEN_ARG)
				.then(argument(ID_ARG, string())
						.suggests(ID_SUGGESTER)
						.executes(Commands::onOpen)
						.then(argument(TARGET_ARG, players())
								.requires(Permissions.require(InventoryMaker.MODID+'.'+OPEN_ARG+".others", 3))
								.executes(Commands::onOpen)));

		var delete = literal(DELETE_ARG)
				.requires(Permissions.require(InventoryMaker.MODID+'.'+DELETE_ARG, 2))
				.then(argument(ID_ARG, string())
						.suggests(ID_SUGGESTER)
						.executes(Commands::onDelete));

		root.then(create);
		root.then(edit);
		root.then(open);
		root.then(delete);

		dispatcher.register(root);
	}

	private static int onCreate(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
		try {
			var type = getRegistryEntry(context, TYPE_ARG, RegistryKeys.SCREEN_HANDLER);
			var typeStatus = Helper.getStatus(type.value());
			if (typeStatus == Helper.Status.DISALLOWED) {
				context.getSource().sendError(Text.literal("Type '"+type.registryKey().getValue()+"' is disallowed."));
				return 0;
			} else if (typeStatus == Helper.Status.UNIMPLEMENTED) {
				context.getSource().sendError(Text.literal("Type '"+type.registryKey().getValue()+"' is unimplemented. Please contact the developer."));
				return 0;
			}

			var id = getId(context);
			var title = ((ArgumentChecker)context).hasArgument(TITLE_ARG) ? getTextArgument(context, TITLE_ARG) : Text.empty();

			if (InventoryMaker.getInventory(id) != null) {
				throw ALREADY_EXISTS_EXCEPTION.create(id);
			}

			var inv = new SavableInventory(type.value(), id, title);
			InventoryLoader.save(inv);
			InventoryMaker.addInventory(inv);

			context.getSource().sendFeedback(() -> Text.literal(id+" created"), false);
			return 1;
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
	}

	private static int onEditChecker(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
		var id = getId(context);
		var checker = getIdentifier(context, CHECKER_ARG);

		getOrThrow(id).setChecker(checker);
		return 1;
	}

	private static int onEditTitle(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
		var id = getId(context);
		var title = getTextArgument(context, TITLE_ARG);

		getOrThrow(id).setTitle(title);
		return 1;
	}

	private static int onEditSlot(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
		var id = getId(context);
		var slot = getInteger(context, SLOT_ARG);
		var inventory = getOrThrow(id);
		if (inventory.size() < slot) {
			throw INVALID_SLOT_EXCEPTION.create(slot);
		}

		var item = getItemStackArgument(context, ITEM_ARG);
		var count = getInteger(context, COUNT_ARG);
		var stack = item.createStack(count, true);

		inventory.setStack(slot, stack);
		return 1;
	}

	private static int onOpen(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
		try {
			var id = getId(context);
			var targets = ((ArgumentChecker)context).hasArgument(TARGET_ARG) ? getOptionalPlayers(context, TARGET_ARG) : List.of(context.getSource().getPlayerOrThrow());

			targets.forEach(getOrThrow(id)::open);
			return targets.size();
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
	}

	private static int onDelete(CommandContext<ServerCommandSource> context) {
		var id = getId(context);
		var inv = InventoryMaker.removeInventory(id);

		if (inv != null) {
			inv.close();
		}
		InventoryLoader.delete(id);
		return 1;
	}

	private static Identifier getId(CommandContext<ServerCommandSource> context) {
		return new Identifier(Placeholders.parseText(Text.of(getString(context, ID_ARG)), PlaceholderContext.of(context.getSource())).getString());
	}

	private static SavableInventory getOrThrow(Identifier id) throws CommandSyntaxException {
		var inventory = InventoryMaker.getInventory(id);
		if (inventory == null) {
			throw DOESNT_EXISTS_EXCEPTION.create(id);
		}
		return inventory;
	}
}
