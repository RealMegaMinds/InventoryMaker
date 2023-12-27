package megaminds.inventorymaker;

import java.util.List;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import eu.pb4.placeholders.api.PlaceholderContext;
import eu.pb4.placeholders.api.Placeholders;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

public class PlaceHolderHelper {
	private PlaceHolderHelper() {}

	public static void addPlaceHolderSuggestions(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) {
		var id = builder.getRemaining();
		var suggestions = getPlaceholderSuggestions(id);
		if (!suggestions.isEmpty()) {
			suggestions.forEach(builder::suggest);
			return;
		}

		var parsed = Placeholders.parseText(Text.of(id), PlaceholderContext.of(context.getSource())).getString();
		var error = getPlaceHolderError(id, PlaceholderContext.of(context.getSource()));
		if (error != null) {
			builder.suggest(id+" ERROR: "+error, Text.literal("ERROR: "+error).styled(s->s.withColor(Formatting.RED)));
		} else if (!Identifier.isValid(Helper.stripQuotes(parsed))) {
			var idError = Text.translatable("argument.id.invalid").append(": "+parsed).styled(s->s.withColor(Formatting.RED));
			builder.suggest(id+" ERROR: "+idError.getString(), idError);
		}
	}

	public static List<String> getPlaceholderSuggestions(String s) {
		var index = Helper.find(s, '%');
		if (index == -1) return List.of();

		var arr = Helper.split(s, index+1);
		return Placeholders.getPlaceholders().keySet().stream().map(Object::toString).filter(id->id.startsWith(arr[1])).map(arr[0]::concat).toList();
	}

	@SuppressWarnings("deprecation")
	public static String getPlaceHolderError(String s, PlaceholderContext context) {
		var matcher = Placeholders.PLACEHOLDER_PATTERN.matcher(s);
		while (matcher.find()) {
			var placeholder = matcher.group("id").split(" ", 2);
			var result = Placeholders.parsePlaceholder(Identifier.tryParse(placeholder[0]), placeholder.length==2 ? placeholder[1] : null, context);
			if (!result.isValid()) return result.string();
		}
		return null;
	}
}
