package com.lantern.onlineplayertageditor.gui;

import com.lantern.onlineplayertageditor.util.PermissionUtil;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.*;
import java.util.function.Consumer;

public class PlayerActionMenuScreenHandler extends ScreenHandler {

    private static final int ROWS = 3;
    private static final int CONTAINER_SIZE = ROWS * 9; // 27

    private static final int SLOT_TAGS = 12;
    private static final int SLOT_SCOREBOARD = 14;
    private static final int SLOT_BACK = 21;
    private static final int SLOT_CLOSE = 23;

    private final ServerPlayerEntity viewer;
    private final UUID targetUuid;
    private String targetName;
    private final SimpleInventory inventory;
    private final Map<Integer, Consumer<ServerPlayerEntity>> slotActions = new HashMap<>();
    private boolean navigating = false;

    public PlayerActionMenuScreenHandler(int syncId, PlayerInventory playerInventory,
                                         ServerPlayerEntity viewer, ServerPlayerEntity target) {
        super(ScreenHandlerType.GENERIC_9X3, syncId);
        this.viewer = viewer;
        this.targetUuid = target.getUuid();
        this.targetName = target.getGameProfile().getName();
        this.inventory = new SimpleInventory(CONTAINER_SIZE);

        for (int i = 0; i < CONTAINER_SIZE; i++) {
            this.addSlot(new Slot(inventory, i, 8 + (i % 9) * 18, 18 + (i / 9) * 18));
        }

        int inventoryY = 18 + ROWS * 18;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, inventoryY + row * 18));
            }
        }

        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, inventoryY + 58));
        }

        updateDisplay();
    }

    public static void open(ServerPlayerEntity viewer, ServerPlayerEntity target) {
        NamedScreenHandlerFactory factory = new SimpleNamedScreenHandlerFactory(
                (syncId, inv, p) -> new PlayerActionMenuScreenHandler(syncId, inv,
                        (ServerPlayerEntity) p, target),
                Text.literal("管理玩家：" + target.getGameProfile().getName())
        );
        viewer.openHandledScreen(factory);
    }

    private ServerPlayerEntity getTarget() {
        return viewer.getServer().getPlayerManager().getPlayer(targetUuid);
    }

    private void updateDisplay() {
        slotActions.clear();
        for (int i = 0; i < CONTAINER_SIZE; i++) {
            inventory.setStack(i, ItemStack.EMPTY);
        }

        ServerPlayerEntity target = getTarget();
        if (target != null) {
            targetName = target.getGameProfile().getName();
        }

        // Tags Edit button
        ItemStack tagsItem = new ItemStack(Items.NAME_TAG);
        tagsItem.set(DataComponentTypes.CUSTOM_NAME, Text.literal("Tags 编辑").formatted(Formatting.GOLD));
        tagsItem.set(DataComponentTypes.LORE, new LoreComponent(List.of(
                Text.literal("编辑该玩家的原版 Entity Tags").formatted(Formatting.GRAY),
                Text.literal("对应原版 /tag 系统").formatted(Formatting.GRAY),
                Text.literal("点击进入").formatted(Formatting.YELLOW)
        )));
        inventory.setStack(SLOT_TAGS, tagsItem);
        slotActions.put(SLOT_TAGS, this::handleTagsEdit);

        // Scoreboard Edit button
        ItemStack scoreItem = new ItemStack(Items.EXPERIENCE_BOTTLE);
        scoreItem.set(DataComponentTypes.CUSTOM_NAME, Text.literal("monvhua 计分板编辑").formatted(Formatting.AQUA));
        scoreItem.set(DataComponentTypes.LORE, new LoreComponent(List.of(
                Text.literal("编辑该玩家的 monvhua 计分板分数").formatted(Formatting.GRAY),
                Text.literal("用于与服务器内已有数据包联动").formatted(Formatting.GRAY),
                Text.literal("点击进入").formatted(Formatting.YELLOW)
        )));
        inventory.setStack(SLOT_SCOREBOARD, scoreItem);
        slotActions.put(SLOT_SCOREBOARD, this::handleScoreboardEdit);

        // Back button
        ItemStack back = new ItemStack(Items.ARROW);
        back.set(DataComponentTypes.CUSTOM_NAME, Text.literal("返回玩家列表").formatted(Formatting.WHITE));
        inventory.setStack(SLOT_BACK, back);
        slotActions.put(SLOT_BACK, this::handleBack);

        // Close button
        ItemStack close = new ItemStack(Items.BARRIER);
        close.set(DataComponentTypes.CUSTOM_NAME, Text.literal("关闭").formatted(Formatting.RED));
        inventory.setStack(SLOT_CLOSE, close);
        slotActions.put(SLOT_CLOSE, p -> p.closeHandledScreen());
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return true;
    }

    @Override
    public void onClosed(PlayerEntity player) {
        super.onClosed(player);
        if (!navigating && player instanceof ServerPlayerEntity sp) {
            sp.getServer().execute(() -> PlayerListScreenHandler.open(sp));
        }
    }

    @Override
    public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player) {
        if (slotIndex >= 0 && slotIndex < CONTAINER_SIZE) {
            if (player instanceof ServerPlayerEntity sp) {
                handleClick(slotIndex, sp);
            }
            return;
        }
        super.onSlotClick(slotIndex, button, actionType, player);
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slot) {
        return ItemStack.EMPTY;
    }

    private void handleClick(int slotIndex, ServerPlayerEntity player) {
        if (!PermissionUtil.canOpenGui(player)) {
            player.sendMessage(Text.literal("你没有权限使用此功能").formatted(Formatting.RED));
            return;
        }

        Consumer<ServerPlayerEntity> action = slotActions.get(slotIndex);
        if (action != null) {
            action.accept(player);
        }
    }

    private void handleTagsEdit(ServerPlayerEntity player) {
        if (!PermissionUtil.canEditTags(player)) {
            player.sendMessage(Text.literal("你没有权限编辑 Tags").formatted(Formatting.RED));
            return;
        }

        ServerPlayerEntity target = getTarget();
        if (target == null) {
            player.sendMessage(Text.literal("目标玩家已离线").formatted(Formatting.RED));
            navigating = true;
            player.getServer().execute(() -> {
                player.closeHandledScreen();
                PlayerListScreenHandler.open(player);
            });
            return;
        }

        navigating = true;
        player.getServer().execute(() -> {
            player.closeHandledScreen();
            TagCategoryMenuScreenHandler.open(player, target);
        });
    }

    private void handleScoreboardEdit(ServerPlayerEntity player) {
        if (!PermissionUtil.canEditTags(player)) {
            player.sendMessage(Text.literal("你没有权限编辑计分板").formatted(Formatting.RED));
            return;
        }

        ServerPlayerEntity target = getTarget();
        if (target == null) {
            player.sendMessage(Text.literal("目标玩家已离线").formatted(Formatting.RED));
            navigating = true;
            player.getServer().execute(() -> {
                player.closeHandledScreen();
                PlayerListScreenHandler.open(player);
            });
            return;
        }

        navigating = true;
        player.getServer().execute(() -> {
            player.closeHandledScreen();
            PlayerScoreboardEditorScreenHandler.open(player, target);
        });
    }

    private void handleBack(ServerPlayerEntity player) {
        navigating = true;
        player.getServer().execute(() -> {
            player.closeHandledScreen();
            PlayerListScreenHandler.open(player);
        });
    }
}
