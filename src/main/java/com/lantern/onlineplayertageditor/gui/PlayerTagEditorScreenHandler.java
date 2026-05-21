package com.lantern.onlineplayertageditor.gui;

import com.lantern.onlineplayertageditor.config.ConfigManager;
import com.lantern.onlineplayertageditor.tag.TagService;
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

public class PlayerTagEditorScreenHandler extends ScreenHandler {

    private static final int ROWS = 6;
    private static final int CONTAINER_SIZE = ROWS * 9; // 54
    private static final int TAG_SLOTS = (ROWS - 1) * 9; // 45

    private static final int NAV_ROW = TAG_SLOTS; // 45
    private static final int SLOT_BACK = NAV_ROW + 0;      // 45
    private static final int SLOT_REFRESH = NAV_ROW + 3;   // 48
    private static final int SLOT_VIEW_ALL = NAV_ROW + 5;  // 50
    private static final int SLOT_CLEAR = NAV_ROW + 8;     // 53

    // Category detection: character tags, magic tags, identity/other
    private static final Set<String> CHARACTER_TAGS = Set.of(
            "ema", "cero", "nnk", "mago", "milya",
            "sherry", "yalisa", "noa", "anan", "yuki",
            "mll", "coco", "hanna"
    );
    private static final Set<String> MAGIC_TAGS = Set.of(
            "WitchSlayer", "Reversal", "Floating", "Power", "BrainWash",
            "Imitation", "Heal", "VisionControl", "Clairvoyance", "FireControl",
            "LiquidControl", "Swap", "Vision", "Sandevistan", "Perception",
            "Intervention"
    );

    private static int getCategory(String tag) {
        if (CHARACTER_TAGS.contains(tag)) return 0;
        if (MAGIC_TAGS.contains(tag)) return 1;
        return 2; // identity & custom
    }

    private final ServerPlayerEntity viewer;
    private final UUID targetUuid;
    private String targetName;
    private final SimpleInventory inventory;
    private int page;
    private int[] tagSlots = new int[0];

    public PlayerTagEditorScreenHandler(int syncId, PlayerInventory playerInventory,
                                        ServerPlayerEntity viewer, ServerPlayerEntity target) {
        super(ScreenHandlerType.GENERIC_9X6, syncId);
        this.viewer = viewer;
        this.targetUuid = target.getUuid();
        this.targetName = target.getGameProfile().getName();
        this.inventory = new SimpleInventory(CONTAINER_SIZE);
        this.page = 0;

        // Container slots
        for (int i = 0; i < CONTAINER_SIZE; i++) {
            this.addSlot(new Slot(inventory, i, 8 + (i % 9) * 18, 18 + (i / 9) * 18));
        }

        // Player inventory (3 rows)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 140 + row * 18));
            }
        }

        // Hotbar
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 198));
        }

        updateDisplay();
    }

    public static void open(ServerPlayerEntity viewer, ServerPlayerEntity target) {
        NamedScreenHandlerFactory factory = new SimpleNamedScreenHandlerFactory(
                (syncId, inv, p) -> new PlayerTagEditorScreenHandler(syncId, inv,
                        (ServerPlayerEntity) p, target),
                Text.literal("编辑 Tags: " + target.getGameProfile().getName())
        );
        viewer.openHandledScreen(factory);
    }

    private ServerPlayerEntity getTarget() {
        return viewer.getServer().getPlayerManager().getPlayer(targetUuid);
    }

    private void updateDisplay() {
        for (int i = 0; i < CONTAINER_SIZE; i++) {
            inventory.setStack(i, ItemStack.EMPTY);
        }

        ServerPlayerEntity target = getTarget();
        if (target != null) {
            targetName = target.getGameProfile().getName();
        }

        List<String> presetTags = ConfigManager.getConfig().presetTags;

        // Build category-aware slot mapping: each category starts on a new row
        int currentRow = 0;
        int currentCol = 0;
        int lastCategory = -1;
        this.tagSlots = new int[presetTags.size()];

        for (int i = 0; i < presetTags.size(); i++) {
            int category = getCategory(presetTags.get(i));

            if (category != lastCategory && lastCategory != -1) {
                currentRow++;
                currentCol = 0;
            }

            if (currentCol >= 9) {
                currentRow++;
                currentCol = 0;
            }

            tagSlots[i] = currentRow * 9 + currentCol;
            currentCol++;
            lastCategory = category;
        }

        int maxRow = currentRow;
        int totalPages = Math.max(1, maxRow / (ROWS - 1) + 1);
        if (page >= totalPages) page = totalPages - 1;
        if (page < 0) page = 0;

        int pageStartSlot = page * TAG_SLOTS;
        int pageEndSlot = pageStartSlot + TAG_SLOTS;

        // Preset tag buttons
        for (int i = 0; i < presetTags.size(); i++) {
            int globalSlot = tagSlots[i];
            if (globalSlot < pageStartSlot || globalSlot >= pageEndSlot) continue;

            String tag = presetTags.get(i);
            int slotIdx = globalSlot - pageStartSlot;

            boolean hasTag = target != null && TagService.hasTag(target, tag);
            String displayName = ConfigManager.getConfig().getDisplayName(tag);

            ItemStack item;
            if (hasTag) {
                item = new ItemStack(Items.LIME_DYE);
                item.set(DataComponentTypes.CUSTOM_NAME,
                        Text.literal("✓ " + displayName).formatted(Formatting.GREEN));
            } else {
                item = new ItemStack(Items.GRAY_DYE);
                item.set(DataComponentTypes.CUSTOM_NAME,
                        Text.literal("✗ " + displayName).formatted(Formatting.GRAY));
            }

            List<Text> lore = new ArrayList<>();
            lore.add(Text.literal("状态: " + (hasTag ? "已拥有" : "未拥有"))
                    .formatted(hasTag ? Formatting.GREEN : Formatting.RED));
            lore.add(Text.literal("左键: " + (hasTag ? "移除此 tag" : "添加此 tag"))
                    .formatted(Formatting.YELLOW));
            if (!displayName.equals(tag)) {
                lore.add(Text.literal("显示名: " + displayName).formatted(Formatting.GRAY));
            }
            lore.add(Text.literal("真实 tag: " + tag).formatted(Formatting.DARK_GRAY));

            item.set(DataComponentTypes.LORE, new LoreComponent(lore));
            inventory.setStack(slotIdx, item);
        }

        // Navigation items
        // Back button
        ItemStack back = new ItemStack(Items.ARROW);
        back.set(DataComponentTypes.CUSTOM_NAME, Text.literal("返回玩家列表").formatted(Formatting.WHITE));
        inventory.setStack(SLOT_BACK, back);

        // Refresh button
        ItemStack refresh = new ItemStack(Items.CLOCK);
        refresh.set(DataComponentTypes.CUSTOM_NAME, Text.literal("刷新").formatted(Formatting.WHITE));
        if (totalPages > 1) {
            refresh.set(DataComponentTypes.LORE, new LoreComponent(List.of(
                    Text.literal("第 " + (page + 1) + " / " + totalPages + " 页").formatted(Formatting.GRAY)
            )));
        }
        inventory.setStack(SLOT_REFRESH, refresh);

        // View all tags
        ItemStack viewAll = new ItemStack(Items.BOOK);
        viewAll.set(DataComponentTypes.CUSTOM_NAME, Text.literal("查看全部 tags").formatted(Formatting.AQUA));
        viewAll.set(DataComponentTypes.LORE, new LoreComponent(List.of(
                Text.literal("在聊天栏显示目标玩家的所有 tags").formatted(Formatting.GRAY)
        )));
        inventory.setStack(SLOT_VIEW_ALL, viewAll);

        // Clear preset tags
        ItemStack clear = new ItemStack(Items.BARRIER);
        clear.set(DataComponentTypes.CUSTOM_NAME, Text.literal("清除配置内 tags").formatted(Formatting.RED));
        clear.set(DataComponentTypes.LORE, new LoreComponent(List.of(
                Text.literal("仅移除预设列表中的 tags").formatted(Formatting.GRAY),
                Text.literal("不会删除玩家的其他 tags").formatted(Formatting.GRAY)
        )));
        inventory.setStack(SLOT_CLEAR, clear);
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return true;
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
        if (!PermissionUtil.canEditTags(player)) {
            player.sendMessage(Text.literal("你没有权限编辑 Tags").formatted(Formatting.RED));
            return;
        }

        // Navigation handlers
        if (slotIndex == SLOT_BACK) {
            player.getServer().execute(() -> {
                player.closeHandledScreen();
                PlayerListScreenHandler.open(player);
            });
            return;
        }

        if (slotIndex == SLOT_REFRESH) {
            page = 0;
            updateDisplay();
            return;
        }

        if (slotIndex == SLOT_VIEW_ALL) {
            viewAllTags(player);
            return;
        }

        if (slotIndex == SLOT_CLEAR) {
            clearPresetTags(player);
            return;
        }

        // Tag button clicked
        if (slotIndex >= 0 && slotIndex < TAG_SLOTS) {
            int globalSlot = page * TAG_SLOTS + slotIndex;
            for (int i = 0; i < tagSlots.length; i++) {
                if (tagSlots[i] == globalSlot) {
                    List<String> presetTags = ConfigManager.getConfig().presetTags;
                    if (i < presetTags.size()) {
                        toggleTag(player, presetTags.get(i));
                    }
                    break;
                }
            }
        }
    }

    private void toggleTag(ServerPlayerEntity player, String tag) {
        ServerPlayerEntity target = getTarget();
        if (target == null) {
            player.sendMessage(Text.literal("目标玩家已离线").formatted(Formatting.RED));
            player.getServer().execute(() -> {
                player.closeHandledScreen();
                PlayerListScreenHandler.open(player);
            });
            return;
        }

        boolean added = TagService.toggleTag(target, tag);
        String displayName = ConfigManager.getConfig().getDisplayName(tag);
        if (added) {
            player.sendMessage(Text.literal("已为 " + targetName + " 添加 tag: " + displayName + " (" + tag + ")")
                    .formatted(Formatting.GREEN));
        } else {
            player.sendMessage(Text.literal("已从 " + targetName + " 移除 tag: " + displayName + " (" + tag + ")")
                    .formatted(Formatting.YELLOW));
        }

        updateDisplay();
    }

    private void viewAllTags(ServerPlayerEntity viewer) {
        ServerPlayerEntity target = getTarget();
        if (target == null) {
            viewer.sendMessage(Text.literal("目标玩家已离线").formatted(Formatting.RED));
            return;
        }

        List<String> tags = TagService.getTags(target);
        viewer.sendMessage(Text.literal("===== " + targetName + " 的所有 Tags (" + tags.size() + "个) =====")
                .formatted(Formatting.GOLD));

        if (tags.isEmpty()) {
            viewer.sendMessage(Text.literal("  (无)").formatted(Formatting.GRAY));
        } else {
            for (String tag : tags) {
                String display = ConfigManager.getConfig().getDisplayName(tag);
                if (!display.equals(tag)) {
                    viewer.sendMessage(Text.literal("  " + display + " (" + tag + ")").formatted(Formatting.WHITE));
                } else {
                    viewer.sendMessage(Text.literal("  " + tag).formatted(Formatting.WHITE));
                }
            }
        }
    }

    private void clearPresetTags(ServerPlayerEntity player) {
        ServerPlayerEntity target = getTarget();
        if (target == null) {
            player.sendMessage(Text.literal("目标玩家已离线").formatted(Formatting.RED));
            player.getServer().execute(() -> {
                player.closeHandledScreen();
                PlayerListScreenHandler.open(player);
            });
            return;
        }

        TagService.clearPresetTags(target, ConfigManager.getConfig().presetTags);
        player.sendMessage(Text.literal("已清除 " + targetName + " 的所有预设 tags").formatted(Formatting.YELLOW));
        updateDisplay();
    }
}
