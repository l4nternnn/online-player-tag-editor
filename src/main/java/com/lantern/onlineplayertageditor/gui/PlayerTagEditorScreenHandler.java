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

    // Category detection: character tags, magic tags, identity/other
    private static final Set<String> CHARACTER_TAGS = Set.of(
            "ema", "cero", "nnk", "mago", "milya",
            "sherry", "yalisa", "noa", "anan", "yuki", "leiya",
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
        return 2;
    }

    private static int calculateRows(int categoryFilter) {
        if (categoryFilter == -1) return 6;
        int count = 0;
        for (String tag : ConfigManager.getConfig().presetTags) {
            if (getCategory(tag) == categoryFilter) count++;
        }
        int tagRows = Math.max(1, (int) Math.ceil((double) count / 9));
        return Math.min(6, tagRows + 3); // top pad + tags + gap + nav
    }

    private static ScreenHandlerType<?> getType(int rows) {
        return switch (rows) {
            case 1 -> ScreenHandlerType.GENERIC_9X1;
            case 2 -> ScreenHandlerType.GENERIC_9X2;
            case 3 -> ScreenHandlerType.GENERIC_9X3;
            case 4 -> ScreenHandlerType.GENERIC_9X4;
            case 5 -> ScreenHandlerType.GENERIC_9X5;
            default -> ScreenHandlerType.GENERIC_9X6;
        };
    }

    private final int rows;
    private final int containerSize;
    private final int tagSlotCount;
    private final int navRow;
    private final ServerPlayerEntity viewer;
    private final UUID targetUuid;
    private String targetName;
    private final SimpleInventory inventory;
    private int page;
    private int[] tagSlots = new int[0];
    private boolean navigating = false;
    private int categoryFilter = -1;

    public PlayerTagEditorScreenHandler(int syncId, PlayerInventory playerInventory,
                                        ServerPlayerEntity viewer, ServerPlayerEntity target, int categoryFilter) {
        super(getType(calculateRows(categoryFilter)), syncId);
        this.rows = calculateRows(categoryFilter);
        this.containerSize = rows * 9;
        this.tagSlotCount = (rows - 3) * 9;
        this.navRow = (rows - 1) * 9;
        this.viewer = viewer;
        this.targetUuid = target.getUuid();
        this.targetName = target.getGameProfile().getName();
        this.inventory = new SimpleInventory(containerSize);
        this.page = 0;
        this.categoryFilter = categoryFilter;

        // Container slots
        for (int i = 0; i < containerSize; i++) {
            this.addSlot(new Slot(inventory, i, 8 + (i % 9) * 18, 18 + (i / 9) * 18));
        }

        // Player inventory (3 rows) + hotbar, positioned below container
        int inventoryY = 18 + rows * 18;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, inventoryY + row * 18));
            }
        }

        // Hotbar
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, inventoryY + 58));
        }

        updateDisplay();
    }

    public static void open(ServerPlayerEntity viewer, ServerPlayerEntity target) {
        open(viewer, target, -1);
    }

    public static void open(ServerPlayerEntity viewer, ServerPlayerEntity target, int category) {
        String title = switch (category) {
            case 0 -> "角色 Tags 编辑：" + target.getGameProfile().getName();
            case 1 -> "魔法 Tags 编辑：" + target.getGameProfile().getName();
            case 2 -> "身份 Tags 编辑：" + target.getGameProfile().getName();
            default -> "编辑 Tags：" + target.getGameProfile().getName();
        };
        NamedScreenHandlerFactory factory = new SimpleNamedScreenHandlerFactory(
                (syncId, inv, p) -> new PlayerTagEditorScreenHandler(syncId, inv,
                        (ServerPlayerEntity) p, target, category),
                Text.literal(title)
        );
        viewer.openHandledScreen(factory);
    }

    private ServerPlayerEntity getTarget() {
        return viewer.getServer().getPlayerManager().getPlayer(targetUuid);
    }

    private void updateDisplay() {
        for (int i = 0; i < containerSize; i++) {
            inventory.setStack(i, ItemStack.EMPTY);
        }

        ServerPlayerEntity target = getTarget();
        if (target != null) {
            targetName = target.getGameProfile().getName();
        }

        List<String> allPresetTags = ConfigManager.getConfig().presetTags;
        List<String> presetTags;
        if (categoryFilter != -1) {
            presetTags = new ArrayList<>();
            for (String tag : allPresetTags) {
                if (getCategory(tag) == categoryFilter) {
                    presetTags.add(tag);
                }
            }
        } else {
            presetTags = new ArrayList<>(allPresetTags);
        }

        // Build slot mapping: when showing all categories, each category starts on a new row
        int currentRow = 0;
        int currentCol = 0;
        int lastCategory = -1;
        this.tagSlots = new int[presetTags.size()];

        for (int i = 0; i < presetTags.size(); i++) {
            int category = getCategory(presetTags.get(i));

            if (categoryFilter == -1 && category != lastCategory && lastCategory != -1) {
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
        int totalPages = Math.max(1, maxRow / (rows - 3) + 1);
        if (page >= totalPages) page = totalPages - 1;
        if (page < 0) page = 0;

        int pageStartSlot = page * tagSlotCount;
        int pageEndSlot = pageStartSlot + tagSlotCount;

        // Preset tag buttons (offset by 9 to skip top padding row)
        for (int i = 0; i < presetTags.size(); i++) {
            int globalSlot = tagSlots[i];
            if (globalSlot < pageStartSlot || globalSlot >= pageEndSlot) continue;

            String tag = presetTags.get(i);
            int slotIdx = 9 + (globalSlot - pageStartSlot);

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
        String backLabel = categoryFilter != -1 ? "返回分类选择" : "返回玩家列表";
        ItemStack back = new ItemStack(Items.ARROW);
        back.set(DataComponentTypes.CUSTOM_NAME, Text.literal(backLabel).formatted(Formatting.WHITE));
        inventory.setStack(navRow + 0, back);

        // Refresh button
        ItemStack refresh = new ItemStack(Items.CLOCK);
        refresh.set(DataComponentTypes.CUSTOM_NAME, Text.literal("刷新").formatted(Formatting.WHITE));
        if (totalPages > 1) {
            refresh.set(DataComponentTypes.LORE, new LoreComponent(List.of(
                    Text.literal("第 " + (page + 1) + " / " + totalPages + " 页").formatted(Formatting.GRAY)
            )));
        }
        inventory.setStack(navRow + 3, refresh);

        // View all tags
        ItemStack viewAll = new ItemStack(Items.BOOK);
        viewAll.set(DataComponentTypes.CUSTOM_NAME, Text.literal("查看全部 tags").formatted(Formatting.AQUA));
        viewAll.set(DataComponentTypes.LORE, new LoreComponent(List.of(
                Text.literal("在聊天栏显示目标玩家的所有 tags").formatted(Formatting.GRAY)
        )));
        inventory.setStack(navRow + 5, viewAll);

        // Clear preset tags
        ItemStack clear = new ItemStack(Items.BARRIER);
        clear.set(DataComponentTypes.CUSTOM_NAME, Text.literal("清除当前分类 tags").formatted(Formatting.RED));
        clear.set(DataComponentTypes.LORE, new LoreComponent(List.of(
                Text.literal("仅移除当前分类的预设 tags").formatted(Formatting.GRAY),
                Text.literal("不会删除玩家的其他 tags").formatted(Formatting.GRAY)
        )));
        inventory.setStack(navRow + 8, clear);
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
                if (categoryFilter != -1) {
                    sp.getServer().execute(() -> TagCategoryMenuScreenHandler.open(sp, target));
                } else {
                    sp.getServer().execute(() -> PlayerActionMenuScreenHandler.open(sp, target));
                }
            } else {
                sp.getServer().execute(() -> PlayerListScreenHandler.open(sp));
            }
        }
    }

    @Override
    public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player) {
        if (slotIndex >= 0 && slotIndex < containerSize) {
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

        // Back button
        if (slotIndex == navRow + 0) {
            navigating = true;
            ServerPlayerEntity backTarget = getTarget();
            player.getServer().execute(() -> {
                player.closeHandledScreen();
                if (categoryFilter != -1 && backTarget != null) {
                    TagCategoryMenuScreenHandler.open(player, backTarget);
                } else {
                    PlayerListScreenHandler.open(player);
                }
            });
            return;
        }

        // Refresh button
        if (slotIndex == navRow + 3) {
            page = 0;
            updateDisplay();
            return;
        }

        // View all tags
        if (slotIndex == navRow + 5) {
            viewAllTags(player);
            return;
        }

        // Clear preset tags
        if (slotIndex == navRow + 8) {
            clearPresetTags(player);
            return;
        }

        // Tag button clicked (tags start at slot 9, row 1)
        int relSlot = slotIndex - 9;
        if (relSlot >= 0 && relSlot < tagSlotCount) {
            int globalSlot = page * tagSlotCount + relSlot;
            for (int i = 0; i < tagSlots.length; i++) {
                if (tagSlots[i] == globalSlot) {
                    toggleTag(player, getActiveTag(i));
                    break;
                }
            }
        }
    }

    private void toggleTag(ServerPlayerEntity player, String tag) {
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

    private String getActiveTag(int index) {
        List<String> allTags = ConfigManager.getConfig().presetTags;
        if (categoryFilter == -1) {
            return allTags.get(index);
        }
        int found = 0;
        for (String tag : allTags) {
            if (getCategory(tag) == categoryFilter) {
                if (found == index) return tag;
                found++;
            }
        }
        return allTags.get(index);
    }

    private List<String> getActivePresetTags() {
        if (categoryFilter == -1) {
            return new ArrayList<>(ConfigManager.getConfig().presetTags);
        }
        List<String> filtered = new ArrayList<>();
        for (String tag : ConfigManager.getConfig().presetTags) {
            if (getCategory(tag) == categoryFilter) {
                filtered.add(tag);
            }
        }
        return filtered;
    }

    private void clearPresetTags(ServerPlayerEntity player) {
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

        List<String> tagsToClear = getActivePresetTags();
        TagService.clearPresetTags(target, tagsToClear);
        player.sendMessage(Text.literal("已清除 " + targetName + " 的当前分类预设 tags").formatted(Formatting.YELLOW));
        updateDisplay();
    }
}
