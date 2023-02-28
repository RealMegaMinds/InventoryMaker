package megaminds.inventorymaker;

import static net.minecraft.command.argument.EntityArgumentType.getOptionalPlayers;
import static net.minecraft.command.argument.EntityArgumentType.players;
import static net.minecraft.command.argument.IdentifierArgumentType.getIdentifier;
import static net.minecraft.command.argument.IdentifierArgumentType.identifier;
import static net.minecraft.command.argument.TextArgumentType.getTextArgument;
import static net.minecraft.command.argument.TextArgumentType.text;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;
import java.util.List;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.command.CommandSource;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

public class Commands {
	private static final DynamicCommandExceptionType ALREADY_EXISTS_EXCEPTION = new DynamicCommandExceptionType(id->new LiteralText("Another inventory with id: " + id + " exists."));
	private static final DynamicCommandExceptionType DOESNT_EXISTS_EXCEPTION = new DynamicCommandExceptionType(id->new LiteralText("No inventory with id: " + id + " exists."));

	private static final SuggestionProvider<ServerCommandSource> TYPE_SUGGESTER = (context, builder) -> CommandSource.suggestIdentifiers(Registry.SCREEN_HANDLER.stream().map(Registry.SCREEN_HANDLER::getId), builder);
	private static final SuggestionProvider<ServerCommandSource> CHECK_SUGGESTER = (context, builder) -> CommandSource.suggestIdentifiers(context.getSource().getServer().getPredicateManager().getIds(), builder);
	private static final SuggestionProvider<ServerCommandSource> ID_SUGGESTER = (context, builder) -> CommandSource.suggestIdentifiers(InventoryMaker.listInventories(), builder);	

	private static final String ID_ARG = "id";
	private static final String CREATE_ARG = "create";
	private static final String TYPE_ARG = "type";
	private static final String TITLE_ARG = "title";
	private static final String EDIT_ARG = "edit";
	private static final String CHECKER_ARG = "checker";
	private static final String OPEN_ARG = "open";
	private static final String TARGET_ARG = "target";
	private static final String DELETE_ARG = "delete";

	private Commands() {}

	@SuppressWarnings("java:S1172")
	public static void register(CommandDispatcher<ServerCommandSource> dispatcher, boolean dedicated) {
		var root = literal(InventoryMaker.MODID)
				.requires(Permissions.require(InventoryMaker.MODID, true));

		var create = literal(CREATE_ARG)
				.requires(Permissions.require(InventoryMaker.MODID+'.'+CREATE_ARG, 2))
				.then(argument(TYPE_ARG, identifier())
						.suggests(TYPE_SUGGESTER)
						.then(argument(ID_ARG, identifier())
								.executes(Commands::onCreate)
								.then(argument(TITLE_ARG, text())
										.executes(Commands::onCreate))));

		var edit = literal(EDIT_ARG)
				.requires(Permissions.require(InventoryMaker.MODID+'.'+EDIT_ARG, 2))
				.then(argument(ID_ARG, identifier())
						.suggests(ID_SUGGESTER)
						.then(literal(TITLE_ARG)
								.then(argument(TITLE_ARG, text())
										.executes(Commands::onEditTitle)))
						.then(literal(CHECKER_ARG)
								.then(argument(CHECKER_ARG, identifier())
										.suggests(CHECK_SUGGESTER)
										.executes(Commands::onEditChecker))));

		var open = literal(OPEN_ARG)
				.then(argument(ID_ARG, identifier())
						.suggests(ID_SUGGESTER)
						.requires(s->s.getEntity() instanceof ServerPlayerEntity)
						.executes(Commands::onOpen)
						.then(argument(TARGET_ARG, players())
								.requires(Permissions.require(InventoryMaker.MODID+'.'+OPEN_ARG+".others", 3))
								.executes(Commands::onOpen)));

		var delete = literal(DELETE_ARG)
				.requires(Permissions.require(InventoryMaker.MODID+'.'+DELETE_ARG, 2))
				.then(argument(ID_ARG, identifier())
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
			var type = getIdentifier(context, TYPE_ARG);
			var id = getIdentifier(context, ID_ARG);
			var title = ((ArgumentChecker)context).hasArgument(TITLE_ARG) ? getTextArgument(context, TITLE_ARG) : LiteralText.EMPTY;

			if (InventoryMaker.getInventory(id) != null) {
				throw ALREADY_EXISTS_EXCEPTION.create(id);
			}

			var inv = new SavableInventory(Registry.SCREEN_HANDLER.get(type), id, title);
			InventoryLoader.save(inv);
			InventoryMaker.addInventory(inv);

			context.getSource().sendFeedback(new LiteralText(id+" created"), false);
			return 1;
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
	}

	private static int onEditChecker(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
		var id = getIdentifier(context, ID_ARG);
		var checker = getIdentifier(context, CHECKER_ARG);

		getOrThrow(id).setChecker(checker);
		return 1;
	}

	private static int onEditTitle(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
		var id = getIdentifier(context, ID_ARG);
		var title = getTextArgument(context, TITLE_ARG);

		getOrThrow(id).setTitle(title);
		return 1;
	}

	private static int onOpen(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
		var id = getIdentifier(context, ID_ARG);
		var targets = ((ArgumentChecker)context).hasArgument(TARGET_ARG) ? getOptionalPlayers(context, TARGET_ARG) : List.of(context.getSource().getPlayer());

		targets.forEach(getOrThrow(id)::open);
		return targets.size();
	}

	private static int onDelete(CommandContext<ServerCommandSource> context) {
		var id = getIdentifier(context, ID_ARG);
		var inv = InventoryMaker.removeInventory(id);

		if (inv != null) {
			inv.close();
		}
		InventoryLoader.delete(id);
		return 1;
	}

	private static SavableInventory getOrThrow(Identifier id) throws CommandSyntaxException {
		var inventory = InventoryMaker.getInventory(id);
		if (inventory == null) {
			throw DOESNT_EXISTS_EXCEPTION.create(id);
		}
		return inventory;
	}
}
