package megaminds.inventorymaker;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

import java.util.List;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;

import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.command.argument.RegistryEntryArgumentType;
import net.minecraft.command.argument.TextArgumentType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

public class Commands {
	private static final DynamicCommandExceptionType ALREADY_EXISTS_EXCEPTION = new DynamicCommandExceptionType(id->Text.literal("Another inventory with id: " + id + " exists."));
	private static final DynamicCommandExceptionType DOESNT_EXISTS_EXCEPTION = new DynamicCommandExceptionType(id->Text.literal("No inventory with id: " + id + " exists."));

	public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
		var root = literal("inv");

		var create = literal("create");
		var edit = literal("edit");
		var open = literal("open");
		var delete = literal("delete")
				.then(argument("id", IdentifierArgumentType.identifier())
						.suggests(null)
						.executes(null));


		var typeArg = argument("type", RegistryEntryArgumentType.registryEntry(accessor, RegistryKeys.SCREEN_HANDLER))
				.suggests((context, builder) -> CommandSource.suggestFromIdentifier(Registries.SCREEN_HANDLER.stream(), builder, Registries.SCREEN_HANDLER::getId, t->Text.empty()));
		var idArg = argument("id", IdentifierArgumentType.identifier())
				.executes(this::onMake);
		var titleArg = argument("title", TextArgumentType.text())
				.executes(this::onMake);
		var checker = literal("checker");
		var checkerArg = argument("checker", IdentifierArgumentType.identifier())
				.suggests((context, builder) -> CommandSource.suggestIdentifiers(context.getSource().getServer().getPredicateManager().getIds(), builder))
				.executes(this::onMake);

		var id2Arg = argument("id", IdentifierArgumentType.identifier())
				.suggests((context, builder) -> CommandSource.suggestIdentifiers(inventories.keySet(), builder))
				.requires(ServerCommandSource::isExecutedByPlayer)
				.executes(this::onOpen);
		var target = argument("target", EntityArgumentType.players())
				.executes(this::onOpen);
		var id3Arg = argument("id", IdentifierArgumentType.identifier())
				.suggests((context, builder) -> CommandSource.suggestIdentifiers(inventories.keySet(), builder))
				.executes(this::onDelete);

		root.then(create.then(typeArg.then(idArg.then(titleArg))));
		root.then(edit.then(checker.then(checkerArg)));
		root.then(open.then(id2Arg.then(target)));
		root.then(delete.then(id3Arg));

		dispatcher.register(root);
	}
	
	private int onMake(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
		var type = RegistryEntryArgumentType.getRegistryEntry(context, "type", RegistryKeys.SCREEN_HANDLER);
		var id = IdentifierArgumentType.getIdentifier(context, "id");
		var title = ((ArgumentChecker)context).hasArgument("title") ? TextArgumentType.getTextArgument(context, "title") : Text.empty();

		if (inventories.containsKey(id)) {
			throw ALREADY_EXISTS_EXCEPTION.create(id);
		}

		var inv = new SavableInventory(type.value(), id, title);
		InventoryLoader.save(inv);
		inventories.put(id, inv);

		context.getSource().sendFeedback(Text.literal(id+" created"), false);
		return 1;
	}

	private int onEdit(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
		var checker = IdentifierArgumentType.getIdentifier(context, "checker");
		return 1;
	}

	private int onOpen(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
		var id = IdentifierArgumentType.getIdentifier(context, "id");
		var targets = ((ArgumentChecker)context).hasArgument("target") ? EntityArgumentType.getOptionalPlayers(context, "target") : List.of(context.getSource().getPlayer());

		var inventory = inventories.get(id);
		if (inventory == null) {
			inventory = ;
			if (inventory == null) {
				throw DOESNT_EXISTS_EXCEPTION.create(id);
			}

			inventories.put(id, inventory);
		}

		targets.forEach(inventory::open);
		return targets.size();
	}

	private int onDelete(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
		var id = IdentifierArgumentType.getIdentifier(context, "id");
		var inv = inventories.remove(id);
		if (inv != null) {
			inv.close();
		}
		InventoryLoader.delete(id);
		return 1;
	}
}
