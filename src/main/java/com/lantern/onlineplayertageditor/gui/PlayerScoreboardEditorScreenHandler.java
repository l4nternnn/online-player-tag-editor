package com.lantern.onlineplayertageditor.gui;

import com.lantern.onlineplayertageditor.scoreboard.ScoreboardService;
import com.lantern.onlineplayertageditor.util.PermissionUtil;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.Item;
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

public class PlayerScoreboardEditorScreenHandler extends ScreenHandler {

    private static final int ROWS = 5;
    private static final int CONTAINER_SIZE = ROWS * 9; // 45

    private static final int SLOT_INFO = 31;
    private static final int SLOT_BACK = 36;
    private static final int SLOT_CLOSE = 44;

    private static final int[] THRESHOLD_SLOTS = {10, 11, 12, 13, 14, 15, 16, 17};
    private static final Item[] THRESHOLD_ITEMS = {
        Items.LIME_DYE, Items.LIGHT_BLUE_DYE, Items.CYAN_DYE, Items.YELLOW_DYE,
        Items.ORANGE_DYE, Items.RED_DYE, Items.PURPLE_DYE, Items.BLACK_DYE
    };

    private final ServerPlayerEntity viewer;
    private final UUID targetUuid;
    private String targetName;
    private final SimpleInventory inventory;
    private final Map<Integer, Consumer<ServerPlayerEntity>> slotActions = new HashMap<>();
    private boolean navigating = false;

    public PlayerScoreboardEditorScreenHandler(int syncId, PlayerInventory playerInventory,
                                               ServerPlayerEntity viewer, ServerPlayerEntity target) {
        super(ScreenHandlerType.GENERIC_9X5, syncId);
        this.viewer = viewer;
        this.targetUuid = target.getUuid();
        this.targetName = target.getGameProfile().getName();
        this.inventory = new SimpleInventory(CONTAINER_SIZE);

        for (int i = 0; i < CONTAINER_SIZE; i++) {
            this.addSlot(new Slot(inventory, i, 8 + (i % 9) * 18, 18 + (i / 9) * 18));
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 122 + row * 18));
            }
        }

        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 180));
        }

        updateDisplay();
    }

    public static void open(ServerPlayerEntity viewer, ServerPlayerEntity target) {
        NamedScreenHandlerFactory factory = new SimpleNamedScreenHandlerFactory(
                (syncId, inv, p) -> new PlayerScoreboardEditorScreenHandler(syncId, inv,
                        (ServerPlayerEntity) p, target),
                Text.literal("monvhua：" + target.getGameProfile().getName())
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

        boolean objectiveExists = ScoreboardService.objectiveExists(viewer.getServer());

        if (!objectiveExists) {
            // Objective not found - show warning
            ItemStack warning = new ItemStack(Items.BARRIER);
            warning.set(DataComponentTypes.CUSTOM_NAME,
                    Text.literal("计分板 objective 不存在").formatted(Formatting.RED));
            warning.set(DataComponentTypes.LORE, new LoreComponent(List.of(
                    Text.literal("monvhua 计分板 objective 不存在").formatted(Formatting.GRAY),
                    Text.literal("请先在服务器或数据包中创建：").formatted(Formatting.GRAY),
                    Text.literal("/scoreboard objectives add monvhua dummy").formatted(Formatting.YELLOW)
            )));
            inventory.setStack(22, warning);
        } else {
            // Threshold buttons
            List<ScoreboardService.MonvhuaLevel> levels = ScoreboardService.LEVELS;
            String targetPlayerName = target != null ? target.getGameProfile().getName() : targetName;
            int currentScore = ScoreboardService.getScore(viewer.getServer(), targetPlayerName);

            for (int i = 0; i < levels.size(); i++) {
                ScoreboardService.MonvhuaLevel level = levels.get(i);
                int slot = THRESHOLD_SLOTS[i];
                Item dyeItem = THRESHOLD_ITEMS[i];

                boolean isCurrent = currentScore == level.value();

                ItemStack item = new ItemStack(dyeItem);
                String prefix = isCurrent ? "■ " : "□ ";
                item.set(DataComponentTypes.CUSTOM_NAME,
                        Text.literal(prefix + level.value() + " - " + level.displayName())
                                .formatted(isCurrent ? Formatting.GREEN : Formatting.WHITE));

                List<Text> lore = new ArrayList<>();
                lore.add(Text.literal("目标玩家：" + targetPlayerName).formatted(Formatting.GRAY));
                lore.add(Text.literal("Objective：monvhua").formatted(Formatting.GRAY));
                lore.add(Text.literal("点击后设置分数为：" + level.value()).formatted(Formatting.GRAY));
                lore.add(Text.literal("当前状态：" + (isCurrent ? "已选中" : "未选中"))
                        .formatted(isCurrent ? Formatting.GREEN : Formatting.RED));

                item.set(DataComponentTypes.LORE, new LoreComponent(lore));
                inventory.setStack(slot, item);

                int thresholdValue = level.value();
                slotActions.put(slot, p -> handleSetScore(p, thresholdValue));
            }

            // Info button - current score
            ItemStack info = new ItemStack(Items.PAPER);
            info.set(DataComponentTypes.CUSTOM_NAME, Text.literal("当前 monvhua 值").formatted(Formatting.AQUA));

            String stageName = ScoreboardService.getStageName(currentScore);
            List<Text> infoLore = new ArrayList<>();
            infoLore.add(Text.literal("玩家：" + targetPlayerName).formatted(Formatting.GRAY));
            infoLore.add(Text.literal("Objective：monvhua").formatted(Formatting.GRAY));
            infoLore.add(Text.literal("当前分数：" + currentScore).formatted(Formatting.WHITE));
            infoLore.add(Text.literal("当前阶段：" + stageName).formatted(Formatting.GOLD));
            info.set(DataComponentTypes.LORE, new LoreComponent(infoLore));
            inventory.setStack(SLOT_INFO, info);
        }

        // Back button
        ItemStack back = new ItemStack(Items.ARROW);
        back.set(DataComponentTypes.CUSTOM_NAME, Text.literal("返回管理菜单").formatted(Formatting.WHITE));
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
        if (!PermissionUtil.canEditTags(player)) {
            player.sendMessage(Text.literal("你没有权限编辑计分板").formatted(Formatting.RED));
            return;
        }

        Consumer<ServerPlayerEntity> action = slotActions.get(slotIndex);
        if (action != null) {
            action.accept(player);
        }
    }

    private void handleSetScore(ServerPlayerEntity player, int value) {
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

        if (!ScoreboardService.objectiveExists(player.getServer())) {
            player.sendMessage(Text.literal("计分板 objective monvhua 不存在，请先在服务器或数据包中创建。")
                    .formatted(Formatting.RED));
            updateDisplay();
            return;
        }

        String playerName = target.getGameProfile().getName();
        ScoreboardService.setScore(player.getServer(), playerName, value);

        String stageName = ScoreboardService.getStageName(value);
        player.sendMessage(Text.literal("已将 " + playerName + " 的 monvhua 设置为 " + value + "（" + stageName + "）")
                .formatted(Formatting.GREEN));

        updateDisplay();
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
