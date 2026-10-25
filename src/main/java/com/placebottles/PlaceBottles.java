package com.placebottles;

import java.util.List;
import java.util.Set;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Interaction;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Shift + right-click the top of a block with a bottle to place it.
 *
 * Each placed bottle is an item_display (renders the real item stack, so potion contents/color
 * are kept) plus an interaction entity (hitbox used to pick the bottle back up by left-clicking).
 * Each block top has 4 slots (2x2), similar to candles.
 */
public class PlaceBottles implements ModInitializer {
	public static final String MOD_ID = "placebottles";

	public static final TagKey<Item> PLACEABLE =
			TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MOD_ID, "placeable"));

	private static final String DISPLAY_TAG = MOD_ID + ".display";
	private static final String HITBOX_TAG = MOD_ID + ".hitbox";

	/** Vanilla items that have a 3D model in assets/placebottles/items. Others keep their normal look. */
	private static final Set<String> MODELLED = Set.of(
			"glass_bottle", "potion", "splash_potion", "lingering_potion",
			"ominous_bottle", "honey_bottle", "experience_bottle", "dragon_breath");

	private static final double BOTTLE_HEIGHT = 0.6;
	private static final float HITBOX_WIDTH = 0.35f;
	private static final float HITBOX_HEIGHT = 0.55f;

	@Override
	public void onInitialize() {
		UseBlockCallback.EVENT.register(this::onUseBlock);
		AttackEntityCallback.EVENT.register(this::onAttackEntity);
		PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, blockEntity) -> dropBottlesOn(level, pos));
	}

	private InteractionResult onUseBlock(Player player, Level level, InteractionHand hand, BlockHitResult hit) {
		if (player.isSpectator() || !player.isShiftKeyDown() || hit.getDirection() != Direction.UP) {
			return InteractionResult.PASS;
		}
		ItemStack held = player.getItemInHand(hand);
		if (!held.is(PLACEABLE)) {
			return InteractionResult.PASS;
		}
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}

		BlockPos pos = hit.getBlockPos();
		Vec3 loc = hit.getLocation();
		double x = pos.getX() + (loc.x - pos.getX() < 0.5 ? 0.25 : 0.75);
		double z = pos.getZ() + (loc.z - pos.getZ() < 0.5 ? 0.25 : 0.75);
		double y = loc.y;

		AABB cell = new AABB(x - 0.2, y, z - 0.2, x + 0.2, y + BOTTLE_HEIGHT, z + 0.2);
		boolean occupied = !level.getEntitiesOfClass(Interaction.class, cell, e -> e.entityTags().contains(HITBOX_TAG)).isEmpty();
		if (occupied || !level.noCollision(cell.deflate(0.01))) {
			return InteractionResult.FAIL;
		}

		spawnBottle(level, x, y, z, held.copyWithCount(1));
		level.playSound(null, x, y, z, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 1.0f, 1.0f);
		if (!player.getAbilities().instabuild) {
			held.shrink(1);
		}
		return InteractionResult.SUCCESS;
	}

	private void spawnBottle(Level level, double x, double y, double z, ItemStack stack) {
		ItemStack shown = stack.copy();
		Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
		if (id.getNamespace().equals("minecraft") && MODELLED.contains(id.getPath())
				&& !shown.has(DataComponents.ITEM_MODEL)) {
			shown.set(DataComponents.ITEM_MODEL, Identifier.fromNamespaceAndPath(MOD_ID, id.getPath()));
		}

		// The model's origin is its center, so lift the display by half a block.
		Display.ItemDisplay display = EntityTypes.ITEM_DISPLAY.create(level, EntitySpawnReason.COMMAND);
		display.setPos(x, y + 0.5, z);
		display.setYRot(level.getRandom().nextFloat() * 360.0f);
		display.addTag(DISPLAY_TAG);
		display.getSlot(0).set(shown);
		level.addFreshEntity(display);

		Interaction hitbox = EntityTypes.INTERACTION.create(level, EntitySpawnReason.COMMAND);
		CompoundTag data = new CompoundTag();
		data.putFloat("width", HITBOX_WIDTH);
		data.putFloat("height", HITBOX_HEIGHT);
		data.putBoolean("response", true);
		hitbox.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), data));
		hitbox.setPos(x, y, z);
		hitbox.addTag(HITBOX_TAG);
		level.addFreshEntity(hitbox);
	}

	private InteractionResult onAttackEntity(Player player, Level level, InteractionHand hand, Entity entity,
			net.minecraft.world.phys.EntityHitResult hit) {
		if (!(entity instanceof Interaction hitbox) || !hitbox.entityTags().contains(HITBOX_TAG) || player.isSpectator()) {
			return InteractionResult.PASS;
		}
		if (!level.isClientSide()) {
			removeBottle(level, hitbox, !player.getAbilities().instabuild);
		}
		return InteractionResult.SUCCESS;
	}

	private void dropBottlesOn(Level level, BlockPos pos) {
		AABB area = new AABB(pos).inflate(0.0, 0.0, 0.0).expandTowards(0.0, 1.01, 0.0);
		List<Interaction> hitboxes = level.getEntitiesOfClass(Interaction.class, area, e -> e.entityTags().contains(HITBOX_TAG)
				&& BlockPos.containing(e.getX(), e.getY() - 0.001, e.getZ()).equals(pos));
		for (Interaction hitbox : hitboxes) {
			removeBottle(level, hitbox, true);
		}
	}

	private void removeBottle(Level level, Interaction hitbox, boolean drop) {
		AABB area = new AABB(hitbox.getX() - 0.05, hitbox.getY(), hitbox.getZ() - 0.05,
				hitbox.getX() + 0.05, hitbox.getY() + 1.0, hitbox.getZ() + 0.05);
		List<Display.ItemDisplay> displays = level.getEntitiesOfClass(Display.ItemDisplay.class, area,
				e -> e.entityTags().contains(DISPLAY_TAG));
		for (Display.ItemDisplay display : displays) {
			ItemStack stack = display.getSlot(0).get().copy();
			Identifier model = stack.get(DataComponents.ITEM_MODEL);
			if (model != null && model.getNamespace().equals(MOD_ID)) {
				stack.remove(DataComponents.ITEM_MODEL);
			}
			if (drop && !stack.isEmpty()) {
				level.addFreshEntity(new ItemEntity(level, hitbox.getX(), hitbox.getY() + 0.1, hitbox.getZ(), stack));
			}
			display.discard();
		}
		level.playSound(null, hitbox.getX(), hitbox.getY(), hitbox.getZ(), SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 1.0f, 1.0f);
		hitbox.discard();
	}
}
