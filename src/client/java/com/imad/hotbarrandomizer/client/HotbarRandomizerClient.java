package com.imad.hotbarrandomizer.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.BlockItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class HotbarRandomizerClient implements ClientModInitializer {

	// [1] State
	private static final String MOD_ID = "hotbar-randomizer";
	private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
			Identifier.fromNamespaceAndPath(MOD_ID, "main"));
	private static final Random RANDOM = new Random();

	private static KeyMapping toggleKey;
	private static boolean enabled = false;
	private static boolean switchPending = false;

	@Override
	public void onInitializeClient() {

		// [2] Register the \ keybind
		toggleKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.hotbar-randomizer.toggle",
				InputConstants.KEY_BACKSLASH,
				CATEGORY));

		// [3] Notice when you right-click a block while holding a block
		UseBlockCallback.EVENT.register((player, level, hand, hitResult) -> {
			if (enabled
					&& level.isClientSide()
					&& hand == InteractionHand.MAIN_HAND
					&& player.getMainHandItem().getItem() instanceof BlockItem) {
				switchPending = true;
			}
			return InteractionResult.PASS;
		});

		// [4] Every tick: handle the toggle key, then do any pending switch
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (toggleKey.consumeClick()) {
				enabled = !enabled;
				switchPending = false;
				if (client.player != null) {
					client.player.sendSystemMessage(Component.literal(
							"Hotbar Randomizer: " + (enabled ? "ON" : "OFF")));
				}
			}

			if (switchPending && client.player != null) {
				switchPending = false;
				switchToRandomBlockSlot(client.player);
			}
		});
	}

	// [5] Pick a random hotbar slot that holds a block
	private static void switchToRandomBlockSlot(LocalPlayer player) {
		Inventory inventory = player.getInventory();
		int current = inventory.getSelectedSlot();

		List<Integer> candidates = new ArrayList<>();
		for (int slot = 0; slot < 9; slot++) {
			if (slot != current && inventory.getItem(slot).getItem() instanceof BlockItem) {
				candidates.add(slot);
			}
		}

		if (!candidates.isEmpty()) {
			inventory.setSelectedSlot(candidates.get(RANDOM.nextInt(candidates.size())));
		}
	}
}