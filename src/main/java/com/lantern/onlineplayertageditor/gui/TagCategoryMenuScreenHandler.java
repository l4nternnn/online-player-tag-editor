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

public class TagCategoryMenuScreenHandler extends ScreenHandler {

    private static final int ROWS = 3;
    private static final int CONTAINER_SIZE = ROWS * 9; // 27

    private static final int SLOT_CHARACTER = 11;
    private static final int SLOT_MAGIC = 13;
    private static final int SLOT_IDENTITY = 15;
    private static final int SLOT_BACK = 21;
    private static final int SLOT_CLOSE = 23;

    private final ServerPlayerEntity viewer;
    private final UUID targetUuid;
    private String targetName;
    private final SimpleInventory inventory;
    private final Map<Integer, Consumer<ServerPlayerEntity>> slotActions = new HashMap<>();
    private boolean navigating = false;

    public TagCategoryMenuScreenHandler(int syncId, PlayerInventory playerInventory,
                                        ServerPlayerEntity viewer, ServerPlayerEntity target) {
        super(ScreenHandlerType.GENERIC_9X3, syncId);
        this.viewer = viewer;
        this.targetUuid = target.getUuid();
        this.targetName = target.getGameProfile().getName();
        this.inventory = new SimpleInventory(CONTAINER_SIZE);

        for (int i = 0; i < CONTAINER_SIZE; i++) {
            this.addSlot(new Slot(inventory, i, 8 + (i % 9) * 18, 18 + (i / 9) * 18));
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 86 + row * 18));
            }
        }

        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 144));
        }

        updateDisplay();
    }

    public static void open(ServerPlayerEntity viewer, ServerPlayerEntity target) {
        NamedScreenHandlerFactory factory = new SimpleNamedScreenHandlerFactory(
                (syncId, inv, p) -> new TagCategoryMenuScreenHandler(syncId, inv,
                        (ServerPlayerEntity) p, target),
                Text.literal("Tags 分类编辑：" + target.getGameProfile().getName())
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

        // 角色 Tags 按钮
        ItemStack characterItem = new ItemStack(Items.PLAYER_HEAD);
        characterItem.set(DataComponentTypes.CUSTOM_NAME, Text.literal("角色 Tags 编辑").formatted(Formatting.GOLD));
        characterItem.set(DataComponentTypes.LORE, new LoreComponent(List.of(
                Text.literal("编辑角色相关的预设 Tags").formatted(Formatting.GRAY),
                Text.literal("包含所有角色名称 Tag").formatted(Formatting.GRAY),
                Text.literal("点击进入").formatted(Formatting.YELLOW)
        )));
        inventory.setStack(SLOT_CHARACTER, characterItem);
        slotActions.put(SLOT_CHARACTER, p -> handleCategory(p, 0));

        // 魔法 Tags 按钮
        ItemStack magicItem = new ItemStack(Items.BLAZE_POWDER);
        magicItem.set(DataComponentTypes.CUSTOM_NAME, Text.literal("魔法 Tags 编辑").formatted(Formatting.LIGHT_PURPLE));
        magicItem.set(DataComponentTypes.LORE, new LoreComponent(List.of(
                Text.literal("编辑魔法相关的预设 Tags").formatted(Formatting.GRAY),
                Text.literal("包含所有魔法能力 Tag").formatted(Formatting.GRAY),
                Text.literal("点击进入").formatted(Formatting.YELLOW)
        )));
        inventory.setStack(SLOT_MAGIC, magicItem);
        slotActions.put(SLOT_MAGIC, p -> handleCategory(p, 1));

        // 身份 Tags 按钮
        ItemStack identityItem = new ItemStack(Items.NAME_TAG);
        identityItem.set(DataComponentTypes.CUSTOM_NAME, Text.literal("身份 Tags 编辑").formatted(Formatting.AQUA));
        identityItem.set(DataComponentTypes.LORE, new LoreComponent(List.of(
                Text.literal("编辑身份相关的预设 Tags").formatted(Formatting.GRAY),
                Text.literal("包含身份/职业 Tag").formatted(Formatting.GRAY),
                Text.literal("点击进入").formatted(Formatting.YELLOW)
        )));
        inventory.setStack(SLOT_IDENTITY, identityItem);
        slotActions.put(SLOT_IDENTITY, p -> handleCategory(p, 2));

        // 返回按钮
        ItemStack back = new ItemStack(Items.ARROW);
        back.set(DataComponentTypes.CUSTOM_NAME, Text.literal("返回管理菜单").formatted(Formatting.WHITE));
        inventory.setStack(SLOT_BACK, back);
        slotActions.put(SLOT_BACK, this::handleBack);

        // 关闭按钮
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
            ServerPlayerEntity target = sp.getServer().getPlayerManager().getPlayer(targetUuid);
            if (target != null) {
                sp.getServer().execute(() -> PlayerActionMenuScreenHandler.open(sp, target));
            } else {
                sp.getServer().execute(() -> PlayerListScreenHandler.open(sp));
            }
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

    private void handleCategory(ServerPlayerEntity player, int category) {
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
            PlayerTagEditorScreenHandler.open(player, target, category);
        });
    }

    private void handleBack(ServerPlayerEntity player) {
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
            PlayerActionMenuScreenHandler.open(player, target);
        });
    }
}
