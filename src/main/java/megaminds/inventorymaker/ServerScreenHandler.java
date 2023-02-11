package megaminds.inventorymaker;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.Slot;

public class ServerScreenHandler extends ScreenHandler {
	private final Inventory inventory;

	protected ServerScreenHandler(ScreenHandlerType<?> type, int syncId, Inventory inventory, PlayerInventory playerInv) {
		super(type, syncId);
		this.inventory = inventory;
		setUpSlots(playerInv);
	}

	private void setUpSlots(PlayerInventory playerInv) {
		//Inventory
		for (int i = 0; i < inventory.size(); i++) {
			addSlot(new Slot(inventory, i, 0, 0));
		}

		//Player Inventory
		for (int i = 0; i < 3; i++) {
			for (int j = 0; j < 9; j++) {
				addSlot(new Slot(playerInv, j + i * 9 + 9, 0, 0));
			}
		}

		//Player Hotbar
		for (int i = 0; i < 9; i++) {
			this.addSlot(new Slot(playerInv, i, 0, 0));
		}
	}

	@Override
	public boolean canUse(PlayerEntity player) {
		return true;
	}

	@Override
	public ItemStack quickMove(PlayerEntity player, int index) {
		var itemStack = ItemStack.EMPTY;
		var slot = slots.get(index);

		if (slot.hasStack()) {
			var itemStack2 = slot.getStack();
			itemStack = itemStack2.copy();
			if (index < inventory.size() ? !this.insertItem(itemStack2, inventory.size(), slots.size(), true) : !this.insertItem(itemStack2, 0, inventory.size(), false)) {
				return ItemStack.EMPTY;
			}

			if (itemStack2.isEmpty()) {
				slot.setStack(ItemStack.EMPTY);
			} else {
				slot.markDirty();
			}
		}
		return itemStack;
	}
}
