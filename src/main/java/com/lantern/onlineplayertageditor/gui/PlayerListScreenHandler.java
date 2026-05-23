package com.lantern.onlineplayertageditor.gui;

import com.lantern.onlineplayertageditor.config.ConfigManager;
import com.lantern.onlineplayertageditor.tag.TagService;
import com.lantern.onlineplayertageditor.util.PermissionUtil;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.component.type.ProfileComponent;
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

public class PlayerListScreenHandler extends ScreenHandler {

    private static int calculateRows(int playerCount) {
        int tagRows = Math.max(1, Math.min(5, (int) Math.ceil((double) Math.max(1, playerCount) / 9)));
        return tagRows + 1; // +1 for navigation row
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
    private final int playerSlots;
    private final int navRow;

    private final ServerPlayerEntity viewer;
    private final SimpleInventory inventory;
    private List<ServerPlayerEntity> onlinePlayers = new ArrayList<>();
    private int page;

    public PlayerListScreenHandler(int syncId, PlayerInventory playerInventory, ServerPlayerEntity viewer) {
        super(getType(calculateRows(viewer.getServer().getPlayerManager().getPlayerList().size())), syncId);
        this.rows = calculateRows(viewer.getServer().getPlayerManager().getPlayerList().size());
        this.containerSize = rows * 9;
        this.playerSlots = (rows - 1) * 9;
        this.navRow = playerSlots;
        this.viewer = viewer;
        this.inventory = new SimpleInventory(containerSize);

        // Container slots
        for (int i = 0; i < containerSize; i++) {
            this.addSlot(new Slot(inventory, i, 8 + (i % 9) * 18, 18 + (i / 9) * 18));
        }

        // Player inventory (3 rows) + hotbar
        int inventoryY = 18 + rows * 18;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, inventoryY + row * 18));
            }
        }

        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, inventoryY + 58));
        }

        this.page = 0;
        updateDisplay();
    }

    public static void open(ServerPlayerEntity player) {
        NamedScreenHandlerFactory factory = new SimpleNamedScreenHandlerFactory(
                (syncId, inv, p) -> new PlayerListScreenHandler(syncId, inv, (ServerPlayerEntity) p),
                Text.literal(ConfigManager.getConfig().guiTitle)
        );
        player.openHandledScreen(factory);
    }

    private void updateDisplay() {
        for (int i = 0; i < containerSize; i++) {
            inventory.setStack(i, ItemStack.EMPTY);
        }

        onlinePlayers = getSortedOnlinePlayers();
        int totalPages = Math.max(1, (int) Math.ceil((double) onlinePlayers.size() / playerSlots));
        if (page >= totalPages) page = totalPages - 1;
        if (page < 0) page = 0;

        int start = page * playerSlots;
        int end = Math.min(start + playerSlots, onlinePlayers.size());

        // Player heads
        for (int i = start; i < end; i++) {
            ServerPlayerEntity target = onlinePlayers.get(i);
            int slotIdx = i - start;

            ItemStack head = new ItemStack(Items.PLAYER_HEAD);
            head.set(DataComponentTypes.CUSTOM_NAME,
                    Text.literal(target.getGameProfile().getName()).formatted(Formatting.GOLD));
            head.set(DataComponentTypes.PROFILE, new ProfileComponent(target.getGameProfile()));

            List<Text> lore = new ArrayList<>();
            int tagCount = TagService.getTagCount(target);
            lore.add(Text.literal("Tag 数量: " + tagCount).formatted(Formatting.GRAY));

            List<String> tags = TagService.getTags(target);
            int showCount = Math.min(5, tags.size());
            for (int j = 0; j < showCount; j++) {
                String tag = tags.get(j);
                String display = ConfigManager.getConfig().getDisplayName(tag);
                if (!display.equals(tag)) {
                    lore.add(Text.literal("  " + display + " (" + tag + ")").formatted(Formatting.DARK_GRAY));
                } else {
                    lore.add(Text.literal("  " + tag).formatted(Formatting.DARK_GRAY));
                }
            }
            if (tags.size() > 5) {
                lore.add(Text.literal("  ...还有 " + (tags.size() - 5) + " 个").formatted(Formatting.DARK_GRAY));
            }
            lore.add(Text.literal(""));
            lore.add(Text.literal("左键点击管理此玩家").formatted(Formatting.YELLOW));

            head.set(DataComponentTypes.LORE, new LoreComponent(lore));
            inventory.setStack(slotIdx, head);
        }

        // Previous page
        if (page > 0) {
            ItemStack prev = new ItemStack(Items.ARROW);
            prev.set(DataComponentTypes.CUSTOM_NAME, Text.literal("上一页").formatted(Formatting.WHITE));
            prev.set(DataComponentTypes.LORE, new LoreComponent(List.of(
                    Text.literal("第 " + (page + 1) + " / " + totalPages + " 页").formatted(Formatting.GRAY)
            )));
            inventory.setStack(navRow + 0, prev);
        }

        // Next page
        if (page < totalPages - 1) {
            ItemStack next = new ItemStack(Items.ARROW);
            next.set(DataComponentTypes.CUSTOM_NAME, Text.literal("下一页").formatted(Formatting.WHITE));
            next.set(DataComponentTypes.LORE, new LoreComponent(List.of(
                    Text.literal("第 " + (page + 1) + " / " + totalPages + " 页").formatted(Formatting.GRAY)
            )));
            inventory.setStack(navRow + 8, next);
        }

        // Refresh
        ItemStack refresh = new ItemStack(Items.CLOCK);
        refresh.set(DataComponentTypes.CUSTOM_NAME, Text.literal("刷新列表").formatted(Formatting.WHITE));
        inventory.setStack(navRow + 4, refresh);

        // Close
        ItemStack close = new ItemStack(Items.BARRIER);
        close.set(DataComponentTypes.CUSTOM_NAME, Text.literal("关闭").formatted(Formatting.RED));
        inventory.setStack(navRow + 7, close);
    }

    private List<ServerPlayerEntity> getSortedOnlinePlayers() {
        var players = viewer.getServer().getPlayerManager().getPlayerList();
        players.sort(Comparator.comparing(p -> p.getGameProfile().getName().toLowerCase()));
        return new ArrayList<>(players);
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return true;
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
        if (!PermissionUtil.canOpenGui(player)) {
            player.sendMessage(Text.literal("你没有权限使用此功能").formatted(Formatting.RED));
            return;
        }

        onlinePlayers = getSortedOnlinePlayers();
        int totalPages = Math.max(1, (int) Math.ceil((double) onlinePlayers.size() / playerSlots));

        if (slotIndex == navRow + 7) {
            player.closeHandledScreen();
            return;
        }

        if (slotIndex == navRow + 4) {
            page = 0;
            updateDisplay();
            return;
        }

        if (slotIndex == navRow + 0 && page > 0) {
            page--;
            updateDisplay();
            return;
        }

        if (slotIndex == navRow + 8 && page < totalPages - 1) {
            page++;
            updateDisplay();
            return;
        }

        // Player head clicked
        if (slotIndex >= 0 && slotIndex < playerSlots) {
            int playerIndex = page * playerSlots + slotIndex;
            if (playerIndex >= 0 && playerIndex < onlinePlayers.size()) {
                ServerPlayerEntity target = onlinePlayers.get(playerIndex);
                player.getServer().execute(() -> {
                    player.closeHandledScreen();
                    PlayerActionMenuScreenHandler.open(player, target);
                });
            }
        }
    }
}
