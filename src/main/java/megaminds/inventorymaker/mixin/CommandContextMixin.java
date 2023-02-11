package megaminds.inventorymaker.mixin;

import java.util.Map;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.ParsedArgument;

import megaminds.inventorymaker.ArgumentChecker;

@Mixin(CommandContext.class)
public abstract class CommandContextMixin implements ArgumentChecker {
	@Shadow
	@Final
	private Map<String, ParsedArgument<?, ?>> arguments;

	@Override
	public boolean hasArgument(String name) {
		return arguments.get(name) != null;
	}
}