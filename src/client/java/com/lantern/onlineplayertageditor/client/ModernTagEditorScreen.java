package com.lantern.onlineplayertageditor.client;

import com.google.gson.JsonSyntaxException;
import com.lantern.onlineplayertageditor.network.EditorNetworking;
import com.lantern.onlineplayertageditor.network.EditorPayloads;
import com.lantern.onlineplayertageditor.network.EditorSnapshot;
import com.lantern.onlineplayertageditor.network.TagCategory;
import com.lantern.onlineplayertageditor.scoreboard.MonvhuaHistory;
import icyllis.modernui.animation.LayoutTransition;
import icyllis.modernui.animation.MotionEasingUtils;
import icyllis.modernui.animation.ObjectAnimator;
import icyllis.modernui.animation.ValueAnimator;
import icyllis.modernui.core.Context;
import icyllis.modernui.fragment.Fragment;
import icyllis.modernui.graphics.Canvas;
import icyllis.modernui.graphics.Paint;
import icyllis.modernui.graphics.drawable.ColorDrawable;
import icyllis.modernui.graphics.drawable.Drawable;
import icyllis.modernui.graphics.drawable.GradientDrawable;
import icyllis.modernui.graphics.drawable.RippleDrawable;
import icyllis.modernui.mc.MuiModApi;
import icyllis.modernui.mc.MuiScreen;
import icyllis.modernui.mc.MinecraftSurfaceView;
import icyllis.modernui.mc.ScreenCallback;
import icyllis.modernui.text.Editable;
import icyllis.modernui.text.TextWatcher;
import icyllis.modernui.util.ColorStateList;
import icyllis.modernui.view.Gravity;
import icyllis.modernui.view.KeyEvent;
import icyllis.modernui.view.LayoutInflater;
import icyllis.modernui.view.MotionEvent;
import icyllis.modernui.view.PointerIcon;
import icyllis.modernui.view.View;
import icyllis.modernui.view.ViewGroup;
import icyllis.modernui.widget.Button;
import icyllis.modernui.widget.EditText;
import icyllis.modernui.widget.FrameLayout;
import icyllis.modernui.widget.LinearLayout;
import icyllis.modernui.widget.ScrollView;
import icyllis.modernui.widget.TextView;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class ModernTagEditorScreen {
    private static final PointerIcon VERTICAL_RESIZE_CURSOR = createVerticalResizeCursor();
    private static final PointerIcon HORIZONTAL_RESIZE_CURSOR = createHorizontalResizeCursor();
    private static EditorFragment currentFragment;
    private static String activeTab = TagCategory.CHARACTER.id();
    private static String searchText = "";
    private static int playersColumnWidth = 280;

    private ModernTagEditorScreen() {
    }

    public static void open(String json) {
        EditorSnapshot snapshot;
        try {
            snapshot = EditorNetworking.GSON.fromJson(json, EditorSnapshot.class);
        } catch (JsonSyntaxException e) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player != null) {
                client.player.sendMessage(Text.literal("Tag 管理台数据解析失败").formatted(Formatting.RED), false);
            }
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        Screen screen = client.currentScreen;
        if (currentFragment != null && screen instanceof MuiScreen muiScreen && muiScreen.getFragment() == currentFragment) {
            currentFragment.applySnapshot(snapshot);
            return;
        }

        EditorFragment fragment = new EditorFragment(snapshot);
        currentFragment = fragment;
        Screen modernScreen = MuiModApi.get().createScreen(fragment, fragment, screen, snapshot.title());
        client.setScreen(modernScreen);
    }

    public static void showNotice(String message, boolean error) {
        if (currentFragment != null) {
            currentFragment.setNotice(message, error);
        }
    }

    public static final class EditorFragment extends Fragment implements ScreenCallback {
        private static final int COLOR_ROOT_TOP = 0xEE10151B;
        private static final int COLOR_ROOT_BOTTOM = 0xEE080B10;
        private static final int COLOR_PANEL = 0xF0182029;
        private static final int COLOR_PANEL_SOFT = 0xE01D2631;
        private static final int COLOR_PANEL_HOVER = 0xF0222D39;
        private static final int COLOR_BORDER = 0x553A4A5B;
        private static final int COLOR_BORDER_ACTIVE = 0xAA66D99B;
        private static final int COLOR_TEXT = 0xFFF3F7FB;
        private static final int COLOR_MUTED = 0xFF93A0AE;
        private static final int COLOR_FAINT = 0xFF687586;
        private static final int COLOR_GREEN = 0xFF66D99B;
        private static final int COLOR_CYAN = 0xFF82D8FF;
        private static final int COLOR_BLUE = 0xFF7AA7FF;
        private static final int COLOR_GOLD = 0xFFF1C76F;
        private static final int COLOR_RED = 0xFFFF7777;
        private static final int RADIUS_PANEL = 8;
        private static final int RADIUS_CONTROL = 7;
        private static final int SPACE = 10;
        private static final int MONVHUA_VALUE_SLOT_WIDTH = 170;
        private static final int MONVHUA_VALUE_SLOT_HEIGHT = 74;
        private static final int MONVHUA_VALUE_SLIDE = 8;
        private static final int MONVHUA_CHART_HEIGHT = 600;
        private static final int PLAYER_LIST_AVATAR_SIZE = 32;
        private static final int PLAYER_OVERVIEW_AVATAR_SIZE = 52;
        private static final int PLAYER_AVATAR_PADDING = 3;

        private EditorSnapshot snapshot;
        private FrameLayout root;
        private LinearLayout contentRoot;
        private LinearLayout playerList;
        private LinearLayout workspace;
        private LinearLayout currentTagsPanel;
        private LinearLayout tabsLayout;
        private LinearLayout editorPanel;
        private LinearLayout editorActions;
        private View playersColumn;
        private View dragHandle;
        private View dialogOverlay;
        private TextView toastView;
        private EditText monvhuaInlineInput;
        private Runnable hideToastRunnable;
        private boolean noticeIsError;
        private String noticeMessage = "";
        private boolean tagDeleteMode;
        private boolean currentTagDeleteMode;
        private boolean monvhuaLogicDetailsVisible;
        private String pendingCurrentTagDeleteConfirm;
        private int editorSwitchVersion;
        private Integer displayedMonvhuaPercent;
        private UUID displayedMonvhuaPlayer;
        private float dragStartRawX;
        private int dragStartPlayersWidth;

        private EditorFragment(EditorSnapshot snapshot) {
            this.snapshot = snapshot;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 icyllis.modernui.util.DataSet savedInstanceState) {
            Context context = requireContext();

            root = new FrameLayout(context);
            root.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                    new int[]{COLOR_ROOT_TOP, COLOR_ROOT_BOTTOM}));
            root.setLayoutParams(match());

            contentRoot = new LinearLayout(context);
            contentRoot.setOrientation(LinearLayout.HORIZONTAL);
            contentRoot.setPadding(16, 16, 16, 16);
            contentRoot.setLayoutTransition(layoutTransition());

            playersColumn = buildPlayersColumn(context);
            dragHandle = horizontalResizeHandle(context);
            workspace = buildWorkspace(context);

            contentRoot.addView(playersColumn, new LinearLayout.LayoutParams(playersColumnWidth, ViewGroup.LayoutParams.MATCH_PARENT));
            contentRoot.addView(dragHandle, new LinearLayout.LayoutParams(14, ViewGroup.LayoutParams.MATCH_PARENT));
            contentRoot.addView(workspace, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1.0f));
            root.addView(contentRoot, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

            rebuild();
            fadeIn(root, 180L);
            return root;
        }

        public void applySnapshot(EditorSnapshot snapshot) {
            this.snapshot = snapshot;
            runOnUi(this::rebuild);
        }

        public void setNotice(String message, boolean error) {
            noticeMessage = message;
            noticeIsError = error;
            runOnUi(() -> {
                showToast(message, error);
            });
        }

        @Override
        public boolean isPauseScreen() {
            return false;
        }

        @Override
        public void onDestroyView() {
            super.onDestroyView();
            if (currentFragment == this) {
                currentFragment = null;
            }
            root = null;
            contentRoot = null;
            playerList = null;
            workspace = null;
            currentTagsPanel = null;
            tabsLayout = null;
            editorPanel = null;
            editorActions = null;
            playersColumn = null;
            dragHandle = null;
            dialogOverlay = null;
            toastView = null;
            monvhuaInlineInput = null;
            hideToastRunnable = null;
            displayedMonvhuaPercent = null;
            displayedMonvhuaPlayer = null;
            pendingCurrentTagDeleteConfirm = null;
        }

        private View buildPlayersColumn(Context context) {
            LinearLayout column = panel(context, COLOR_PANEL, COLOR_BORDER);
            column.addView(titleBlock(context, "在线玩家", "搜索、选择目标，然后在右侧编辑"), fullWidthWrap());

            EditText search = new EditText(context);
            search.setHint("搜索玩家");
            search.setText(searchText);
            search.setSingleLine(true);
            search.setTextSize(14);
            search.setPadding(10, 8, 10, 8);
            search.setBackground(rounded(COLOR_PANEL_SOFT, COLOR_BORDER, RADIUS_CONTROL));
            search.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    searchText = s.toString();
                    rebuildPlayerList();
                }

                @Override
                public void afterTextChanged(Editable s) {
                }
            });
            column.addView(search, fullWidthWrapWithMargins(0, 8, 0, 10));

            ScrollView scroll = new ScrollView(context);
            playerList = new LinearLayout(context);
            playerList.setOrientation(LinearLayout.VERTICAL);
            playerList.setLayoutTransition(layoutTransition());
            scroll.addView(playerList, scrollChild());
            column.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f));

            Button refresh = actionButton(context, "刷新列表", COLOR_BLUE, false);
            refresh.setOnClickListener(v -> refreshSnapshot());
            column.addView(refresh, fullWidthWrapWithMargins(0, 10, 0, 0));
            return column;
        }

        private LinearLayout buildWorkspace(Context context) {
            LinearLayout column = new LinearLayout(context);
            column.setOrientation(LinearLayout.VERTICAL);
            return column;
        }

        private void rebuild() {
            if (root == null) {
                return;
            }
            rebuildPlayerList();
            rebuildWorkspace();
        }

        private void rebuildPlayerList() {
            if (playerList == null) {
                return;
            }
            playerList.removeAllViews();
            Context context = requireContext();
            String filter = searchText.toLowerCase(Locale.ROOT);
            int shown = 0;

            for (EditorSnapshot.PlayerEntry player : snapshot.players()) {
                if (!filter.isBlank() && !player.name().toLowerCase(Locale.ROOT).contains(filter)) {
                    continue;
                }
                shown++;
                boolean selected = player.uuid().equals(snapshot.selectedPlayerUuid());
                View entry = playerButton(context, player, selected);
                entry.setOnClickListener(v -> {
                    pulse(v);
                    send(new EditorPayloads.RefreshEditorC2S(player.uuid()));
                });
                playerList.addView(entry, fullWidthWrapWithMargins(0, 0, 0, 8));
            }

            if (shown == 0) {
                playerList.addView(emptyState(context, searchText.isBlank() ? "当前没有在线玩家" : "没有匹配的在线玩家"), fullWidthWrap());
            }
        }

        private void rebuildWorkspace() {
            workspace.removeAllViews();
            Context context = requireContext();
            workspace.addView(buildOverviewCard(context), fullWidthWrapWithMargins(0, 0, 0, SPACE));

            LinearLayout lower = new LinearLayout(context);
            lower.setOrientation(LinearLayout.HORIZONTAL);

            currentTagsPanel = panel(context, COLOR_PANEL, COLOR_BORDER);
            currentTagsPanel.addView(sectionHeader(context, "当前 Tags", selectedTagCount() + " 个已启用"), fullWidthWrap());
            rebuildCurrentTags(context);
            LinearLayout.LayoutParams summaryParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1.05f);
            summaryParams.setMargins(0, 0, SPACE, 0);
            lower.addView(currentTagsPanel, summaryParams);

            LinearLayout editorCard = panel(context, COLOR_PANEL, COLOR_BORDER);
            editorCard.addView(buildTabs(context), fullWidthWrapWithMargins(0, 0, 0, SPACE));

            ScrollView scroll = new ScrollView(context);
            editorPanel = new LinearLayout(context);
            editorPanel.setOrientation(LinearLayout.VERTICAL);
            scroll.addView(editorPanel, scrollChild());
            editorCard.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f));

            editorActions = new LinearLayout(context);
            editorActions.setOrientation(LinearLayout.HORIZONTAL);
            editorActions.setGravity(Gravity.RIGHT);
            editorCard.addView(editorActions, fullWidthWrapWithMargins(0, SPACE, 0, 0));
            lower.addView(editorCard, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1.0f));

            workspace.addView(lower, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f));
            rebuildEditor();
        }

        private View buildOverviewCard(Context context) {
            LinearLayout card = panel(context, COLOR_PANEL, COLOR_BORDER_ACTIVE);
            card.setBackground(new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                    new int[]{0xF01A2430, 0xF0141B23}));

            LinearLayout row = new LinearLayout(context);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);

            LinearLayout identity = new LinearLayout(context);
            identity.setOrientation(LinearLayout.VERTICAL);

            LinearLayout nameRow = new LinearLayout(context);
            nameRow.setOrientation(LinearLayout.HORIZONTAL);
            nameRow.setGravity(Gravity.CENTER_VERTICAL);
            nameRow.addView(title(context, snapshot.selectedPlayerName()), new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            if (snapshot.selectedPlayerUuid() != null) {
                LinearLayout.LayoutParams avatarParams = new LinearLayout.LayoutParams(PLAYER_OVERVIEW_AVATAR_SIZE, PLAYER_OVERVIEW_AVATAR_SIZE);
                avatarParams.setMargins(10, 0, 0, 0);
                nameRow.addView(playerAvatar(context, snapshot.selectedPlayerUuid(), PLAYER_OVERVIEW_AVATAR_SIZE), avatarParams);
            }
            identity.addView(nameRow, fullWidthWrap());
            String uuid = snapshot.selectedPlayerUuid() == null ? "未选择玩家" : shortUuid(snapshot.selectedPlayerUuid());
            identity.addView(body(context, (snapshot.selectedPlayerOnline() ? "在线" : "离线，无法写入") + " · " + uuid), fullWidthWrap());
            row.addView(identity, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

            LinearLayout stats = new LinearLayout(context);
            stats.setOrientation(LinearLayout.HORIZONTAL);
            stats.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
            stats.addView(metric(context, String.valueOf(selectedTagCount()), "Tags"), metricParams());
            if (snapshot.scoreboardObjectiveExists()) {
                stats.addView(metric(context, String.valueOf(snapshot.selectedScore()), snapshot.selectedStageName()), metricParams());
            } else {
                stats.addView(metric(context, "缺失", "monvhua objective"), metricParams());
            }
            row.addView(stats, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            card.addView(row, fullWidthWrap());

            card.addView(monvhuaReadout(context), fullWidthWrapWithMargins(0, 10, 0, 0));
            return card;
        }

        private View monvhuaReadout(Context context) {
            LinearLayout row = new LinearLayout(context);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);

            TextView label = small(context, "魔女化程度：");
            label.setTextColor(COLOR_TEXT);
            label.setTextSize(15);
            row.addView(label, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            int percent = currentMonvhuaPercent();
            UUID player = snapshot.selectedPlayerUuid();
            boolean samePlayer = player != null && player.equals(displayedMonvhuaPlayer);
            boolean animate = samePlayer && displayedMonvhuaPercent != null && !displayedMonvhuaPercent.equals(percent);
            int previous = displayedMonvhuaPercent == null ? percent : displayedMonvhuaPercent;
            displayedMonvhuaPercent = percent;
            displayedMonvhuaPlayer = player;

            FrameLayout valueSlot = new FrameLayout(context);
            valueSlot.addView(monvhuaValue(context, percent), monvhuaValueParams(0));
            if (animate) {
                int direction = percent >= previous ? 1 : -1;
                valueSlot.removeAllViews();
                TextView oldValue = monvhuaValue(context, previous);
                TextView newValue = monvhuaValue(context, percent);
                newValue.setAlpha(0.0f);
                valueSlot.addView(oldValue, monvhuaValueParams(0));
                valueSlot.addView(newValue, monvhuaValueParams(MONVHUA_VALUE_SLIDE * direction));
                animateValueShift(oldValue, 0, -MONVHUA_VALUE_SLIDE * direction, 1.0f, 0.0f, 150L);
                animateValueShift(newValue, MONVHUA_VALUE_SLIDE * direction, 0, 0.0f, 1.0f, 190L);
                valueSlot.postDelayed(() -> {
                    if (valueSlot != null) {
                        valueSlot.removeView(oldValue);
                    }
                }, 170L);
            }
            row.addView(valueSlot, new LinearLayout.LayoutParams(MONVHUA_VALUE_SLOT_WIDTH, MONVHUA_VALUE_SLOT_HEIGHT));
            row.addView(monvhuaInlineEditor(context), new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            return row;
        }

        private int currentMonvhuaPercent() {
            return snapshot.scoreboardObjectiveExists() ? clamp(snapshot.selectedScore(), 0, 100) : -1;
        }

        private TextView monvhuaValue(Context context, int percent) {
            TextView value = new TextView(context);
            value.setText(percent >= 0 ? percent + "%" : "--%");
            value.setTextSize(42);
            value.setTextColor(percent >= 0 ? monvhuaPercentColor(percent) : COLOR_RED);
            value.setGravity(Gravity.CENTER_VERTICAL);
            value.setPadding(0, 2, 0, 4);
            return value;
        }

        private View monvhuaInlineEditor(Context context) {
            LinearLayout editor = new LinearLayout(context);
            editor.setOrientation(LinearLayout.HORIZONTAL);
            editor.setGravity(Gravity.CENTER_VERTICAL);
            editor.setPadding(12, 0, 0, 0);

            monvhuaInlineInput = new EditText(context);
            monvhuaInlineInput.setHint("0-100");
            monvhuaInlineInput.setText(snapshot.scoreboardObjectiveExists() ? String.valueOf(clamp(snapshot.selectedScore(), 0, 100)) : "");
            monvhuaInlineInput.setSingleLine(true);
            monvhuaInlineInput.setTextSize(15);
            monvhuaInlineInput.setPadding(10, 8, 10, 8);
            monvhuaInlineInput.setEnabled(snapshot.scoreboardObjectiveExists() && snapshot.selectedPlayerUuid() != null);
            monvhuaInlineInput.setOnKeyListener((view, keyCode, event) -> {
                if (event.getAction() == KeyEvent.ACTION_UP
                        && (keyCode == KeyEvent.KEY_ENTER || keyCode == KeyEvent.KEY_KP_ENTER)) {
                    applyMonvhuaInput();
                    return true;
                }
                return false;
            });
            applyHoverBackground(monvhuaInlineInput, COLOR_PANEL_SOFT, COLOR_PANEL_HOVER, COLOR_BORDER, COLOR_BORDER_ACTIVE);
            editor.addView(monvhuaInlineInput, new LinearLayout.LayoutParams(92, ViewGroup.LayoutParams.WRAP_CONTENT));

            Button apply = actionButton(context, "应用", COLOR_GREEN, true);
            apply.setOnClickListener(v -> applyMonvhuaInput());
            editor.addView(apply, actionButtonParams(76, 8));

            Button cancel = actionButton(context, "取消", COLOR_MUTED, false);
            cancel.setOnClickListener(v -> resetMonvhuaInput());
            editor.addView(cancel, actionButtonParams(76, 8));
            return editor;
        }

        private void applyMonvhuaInput() {
            if (snapshot.selectedPlayerUuid() == null) {
                setNotice("请先选择在线玩家", true);
                return;
            }
            if (!snapshot.scoreboardObjectiveExists()) {
                setNotice("objective monvhua 不存在", true);
                return;
            }
            if (monvhuaInlineInput == null) {
                return;
            }

            String raw = monvhuaInlineInput.getText().toString().trim();
            int value;
            try {
                value = Integer.parseInt(raw);
            } catch (NumberFormatException e) {
                setNotice("魔女化值必须是 0-100 的整数", true);
                return;
            }
            if (value < 0 || value > 100) {
                setNotice("魔女化值必须在 0-100 之间", true);
                return;
            }

            send(new EditorPayloads.SetMonvhuaScoreC2S(snapshot.selectedPlayerUuid(), value));
        }

        private void resetMonvhuaInput() {
            if (monvhuaInlineInput != null) {
                monvhuaInlineInput.setText(snapshot.scoreboardObjectiveExists() ? String.valueOf(clamp(snapshot.selectedScore(), 0, 100)) : "");
            }
        }

        private FrameLayout.LayoutParams monvhuaValueParams(int topOffset) {
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.LEFT | Gravity.CENTER_VERTICAL);
            params.setMargins(2, topOffset, 0, 0);
            return params;
        }

        private void animateValueShift(View view, int fromTop, int toTop, float fromAlpha, float toAlpha, long duration) {
            ValueAnimator move = ValueAnimator.ofFloat(0.0f, 1.0f);
            move.setDuration(duration);
            move.setInterpolator(MotionEasingUtils.MOTION_EASING_STANDARD_DECELERATE);
            move.addUpdateListener(animation -> {
                Object value = animation.getAnimatedValue();
                float progress = value instanceof Number number ? number.floatValue() : animation.getAnimatedFraction();
                FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) view.getLayoutParams();
                params.setMargins(2, Math.round(fromTop + (toTop - fromTop) * progress), 0, 0);
                view.setLayoutParams(params);
            });
            move.start();

            ObjectAnimator alpha = ObjectAnimator.ofFloat(view, View.ALPHA, fromAlpha, toAlpha);
            alpha.setDuration(duration);
            alpha.setInterpolator(MotionEasingUtils.MOTION_EASING_STANDARD);
            alpha.start();
        }

        private int monvhuaPercentColor(int percent) {
            float progress = clamp(percent, 0, 100) / 100.0f;
            float hue = 138.0f * (1.0f - progress);
            float saturation = 0.68f + 0.22f * progress;
            float value = 0.86f - 0.36f * progress;
            return hsvColor(hue, saturation, value);
        }

        private int hsvColor(float hue, float saturation, float value) {
            float c = value * saturation;
            float h = ((hue % 360.0f) + 360.0f) % 360.0f;
            float x = c * (1.0f - Math.abs((h / 60.0f) % 2.0f - 1.0f));
            float m = value - c;
            float r = 0.0f;
            float g = 0.0f;
            float b = 0.0f;

            if (h < 60.0f) {
                r = c;
                g = x;
            } else if (h < 120.0f) {
                r = x;
                g = c;
            } else if (h < 180.0f) {
                g = c;
                b = x;
            } else if (h < 240.0f) {
                g = x;
                b = c;
            } else if (h < 300.0f) {
                r = x;
                b = c;
            } else {
                r = c;
                b = x;
            }

            int red = Math.round((r + m) * 255.0f);
            int green = Math.round((g + m) * 255.0f);
            int blue = Math.round((b + m) * 255.0f);
            return 0xFF000000 | (red << 16) | (green << 8) | blue;
        }

        private void rebuildCurrentTags(Context context) {
            LinearLayout columns = new LinearLayout(context);
            columns.setOrientation(LinearLayout.HORIZONTAL);
            addCurrentTagColumn(context, columns, TagCategory.CHARACTER, "角色");
            addCurrentTagColumn(context, columns, TagCategory.MAGIC, "魔法");
            addCurrentTagColumn(context, columns, TagCategory.IDENTITY, "身份");
            addCurrentTagColumn(context, columns, TagCategory.OTHER, "其他");
            currentTagsPanel.addView(columns, fullWidthWrapWithMargins(0, SPACE, 0, 0));
            currentTagsPanel.addView(new View(context), new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f));
            currentTagsPanel.addView(currentTagActions(context), fullWidthWrapWithMargins(0, SPACE, 0, 0));
        }

        private void addCurrentTagColumn(Context context, LinearLayout columns, TagCategory category, String title) {
            LinearLayout column = panel(context, COLOR_PANEL_SOFT, 0x223A4A5B);
            column.addView(smallCaps(context, title), fullWidthWrapWithMargins(0, 0, 0, 6));

            List<String> tags = selectedTagsForCategory(category);
            if (tags.isEmpty()) {
                column.addView(emptyState(context, "无"), fullWidthWrap());
            } else {
                for (String tag : tags) {
                    View chip = tagChip(context, displayTagWithRawName(tag), true, false);
                    if (currentTagDeleteMode) {
                        column.addView(currentTagDeleteRow(context, chip, tag), fullWidthWrapWithMargins(0, 0, 0, 6));
                    } else {
                        column.addView(chip, fullWidthWrapWithMargins(0, 0, 0, 6));
                    }
                }
            }

            columns.addView(column, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
        }

        private LinearLayout currentTagActions(Context context) {
            LinearLayout actions = new LinearLayout(context);
            actions.setOrientation(LinearLayout.HORIZONTAL);
            actions.setGravity(Gravity.RIGHT);

            Button add = actionButton(context, "手动添加", COLOR_GREEN, false);
            add.setOnClickListener(v -> showAddCurrentTagDialog());
            actions.addView(add, actionButtonParams(108, 0));

            Button remove = actionButton(context, currentTagDeleteMode ? "完成" : "删除模式",
                    currentTagDeleteMode ? COLOR_CYAN : COLOR_GOLD, false);
            remove.setOnClickListener(v -> {
                currentTagDeleteMode = !currentTagDeleteMode;
                pendingCurrentTagDeleteConfirm = null;
                rebuildWorkspace();
            });
            actions.addView(remove, actionButtonParams(108, 10));
            return actions;
        }

        private LinearLayout currentTagDeleteRow(Context context, View chip, String tag) {
            LinearLayout row = new LinearLayout(context);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setOnClickListener(v -> handleCurrentTagDeleteClick(row, tag));
            chip.setOnClickListener(v -> handleCurrentTagDeleteClick(chip, tag));
            row.addView(chip, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

            Button delete = dangerButton(context, "移除");
            delete.setOnClickListener(v -> handleCurrentTagDeleteClick(delete, tag));
            row.addView(delete, new LinearLayout.LayoutParams(74, ViewGroup.LayoutParams.WRAP_CONTENT));
            return row;
        }

        private View buildTabs(Context context) {
            LinearLayout tabs = new LinearLayout(context);
            tabs.setOrientation(LinearLayout.HORIZONTAL);
            tabs.setBackground(rounded(0xAA10161D, COLOR_BORDER, RADIUS_CONTROL));
            tabs.setPadding(4, 4, 4, 4);
            tabsLayout = tabs;
            rebuildTabs(context, false);
            return tabs;
        }

        private void rebuildTabs(Context context, boolean animateSelected) {
            if (tabsLayout == null) {
                return;
            }
            tabsLayout.removeAllViews();
            tabsLayout.addView(tabButton(context, TagCategory.CHARACTER.id(), "角色", animateSelected), equalWidthWrap());
            tabsLayout.addView(tabButton(context, TagCategory.MAGIC.id(), "魔法", animateSelected), equalWidthWrap());
            tabsLayout.addView(tabButton(context, TagCategory.IDENTITY.id(), "身份", animateSelected), equalWidthWrap());
            tabsLayout.addView(tabButton(context, TagCategory.OTHER.id(), "其他", animateSelected), equalWidthWrap());
            tabsLayout.addView(tabButton(context, "monvhua", "魔女化程度", animateSelected), equalWidthWrap());
        }

        private Button tabButton(Context context, String tab, String label, boolean animateSelected) {
            boolean selected = activeTab.equals(tab);
            Button button = flatButton(context, label, selected ? COLOR_GREEN : COLOR_MUTED);
            button.setBackground(rounded(selected ? 0x3366D99B : 0x0010161D, selected ? 0x8866D99B : 0x00000000, RADIUS_CONTROL));
            if (selected && animateSelected) {
                button.setAlpha(0.72f);
                ObjectAnimator alpha = ObjectAnimator.ofFloat(button, View.ALPHA, 0.72f, 1.0f);
                alpha.setDuration(160L);
                alpha.setInterpolator(MotionEasingUtils.MOTION_EASING_STANDARD_DECELERATE);
                alpha.start();
            }
            button.setOnClickListener(v -> {
                if (!activeTab.equals(tab)) {
                    activeTab = tab;
                    tagDeleteMode = false;
                    rebuildTabs(requireContext(), true);
                    animateEditorSwitch();
                }
            });
            return button;
        }

        private void animateEditorSwitch() {
            if (editorPanel == null || editorActions == null) {
                return;
            }
            int version = ++editorSwitchVersion;
            fade(editorPanel, editorPanel.getAlpha(), 0.0f, 90L, MotionEasingUtils.MOTION_EASING_STANDARD_ACCELERATE);
            fade(editorActions, editorActions.getAlpha(), 0.0f, 90L, MotionEasingUtils.MOTION_EASING_STANDARD_ACCELERATE);

            editorPanel.postDelayed(() -> {
                if (version != editorSwitchVersion || editorPanel == null || editorActions == null) {
                    return;
                }
                rebuildEditor();
                editorPanel.setAlpha(0.0f);
                editorActions.setAlpha(0.0f);
                fade(editorPanel, 0.0f, 1.0f, 140L, MotionEasingUtils.MOTION_EASING_STANDARD_DECELERATE);
                fade(editorActions, 0.0f, 1.0f, 140L, MotionEasingUtils.MOTION_EASING_STANDARD_DECELERATE);
            }, 95L);
        }

        private void rebuildEditor() {
            editorPanel.removeAllViews();
            editorActions.removeAllViews();
            Context context = requireContext();
            if ("monvhua".equals(activeTab)) {
                editorActions.setVisibility(View.GONE);
                rebuildScoreEditor(context);
                return;
            }
            editorActions.setVisibility(View.VISIBLE);
            rebuildTagEditor(context, activeTab);
            rebuildTagActions(context, activeTab);
        }

        private void rebuildTagEditor(Context context, String categoryId) {
            TagCategory category = TagCategory.fromId(categoryId);
            editorPanel.addView(sectionHeader(context, category.displayName(), selectedPresetCount(categoryId) + " 个已拥有"), fullWidthWrap());
            int count = 0;
            for (EditorSnapshot.TagEntry tag : snapshot.presetTags()) {
                if (!categoryId.equals(tag.categoryId())) {
                    continue;
                }
                count++;
                Button tagButton = tagButton(context, tag);
                tagButton.setOnClickListener(v -> {
                    if (snapshot.selectedPlayerUuid() != null) {
                        pulse(v);
                        send(new EditorPayloads.ToggleTagC2S(snapshot.selectedPlayerUuid(), tag.tag()));
                    }
                });
                if (tagDeleteMode) {
                    editorPanel.addView(tagDeleteRow(context, tagButton, categoryId, tag.tag()), fullWidthWrapWithMargins(0, 0, 0, 8));
                } else {
                    editorPanel.addView(tagButton, fullWidthWrapWithMargins(0, 0, 0, 8));
                }
            }

            if (count == 0) {
                editorPanel.addView(emptyState(context, "这个分类还没有预设 tag"), fullWidthWrapWithMargins(0, SPACE, 0, 0));
            }
        }

        private LinearLayout tagDeleteRow(Context context, Button tagButton, String categoryId, String tag) {
            LinearLayout row = new LinearLayout(context);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.addView(tagButton, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

            Button delete = dangerButton(context, "移除");
            delete.setOnClickListener(v -> showDeleteConfirmDialog(categoryId, tag));
            row.addView(delete, new LinearLayout.LayoutParams(74, ViewGroup.LayoutParams.WRAP_CONTENT));
            return row;
        }

        private void rebuildTagActions(Context context, String categoryId) {
            View spacer = new View(context);
            editorActions.addView(spacer, new LinearLayout.LayoutParams(0, 1, 1.0f));

            Button clear = dangerButton(context, "清空本类");
            clear.setOnClickListener(v -> showClearCategoryDialog(categoryId));
            editorActions.addView(clear, actionButtonParams(128, 0));

            Button add = actionButton(context, "添加", COLOR_GREEN, false);
            add.setOnClickListener(v -> showAddTagDialog(categoryId));
            editorActions.addView(add, actionButtonParams(86, 10));

            Button remove = actionButton(context, tagDeleteMode ? "完成" : "删除模式", tagDeleteMode ? COLOR_CYAN : COLOR_GOLD, false);
            remove.setOnClickListener(v -> {
                tagDeleteMode = !tagDeleteMode;
                rebuildEditor();
            });
            editorActions.addView(remove, actionButtonParams(108, 10));
        }

        private void rebuildScoreEditor(Context context) {
            editorPanel.addView(monvhuaHistoryHeader(context), fullWidthWrap());
            if (!snapshot.scoreboardObjectiveExists()) {
                TextView warning = body(context, "objective monvhua 不存在，请先在服务端创建。");
                warning.setTextColor(COLOR_RED);
                warning.setBackground(rounded(0x22FF7777, 0x55FF7777, RADIUS_CONTROL));
                warning.setPadding(10, 8, 10, 8);
                editorPanel.addView(warning, fullWidthWrapWithMargins(0, SPACE, 0, 0));
                return;
            }
            if (monvhuaLogicDetailsVisible) {
                editorPanel.addView(monvhuaHistoryLogicHint(context), fullWidthWrapWithMargins(0, SPACE, 0, 0));
            }
            editorPanel.addView(monvhuaHistoryChart(context), fullWidthFixedHeightWithMargins(MONVHUA_CHART_HEIGHT, 0, SPACE, 0, SPACE));
            editorPanel.addView(sectionHeader(context, "阶段参考", "点击可一键切换"), fullWidthWrapWithMargins(0, 0, 0, SPACE));
            EditorSnapshot.ScoreLevelEntry currentLevel = currentScoreLevel();
            for (EditorSnapshot.ScoreLevelEntry level : snapshot.scoreLevels()) {
                boolean current = currentLevel != null && level.value() == currentLevel.value();
                Button scoreButton = scoreReferenceButton(context, level, current);
                scoreButton.setOnClickListener(v -> {
                    if (snapshot.selectedPlayerUuid() != null) {
                        pulse(v);
                        send(new EditorPayloads.SetMonvhuaScoreC2S(snapshot.selectedPlayerUuid(), level.value()));
                    }
                });
                editorPanel.addView(scoreButton, fullWidthWrapWithMargins(0, 0, 0, 8));
            }
        }

        private View monvhuaHistoryHeader(Context context) {
            LinearLayout header = new LinearLayout(context);
            header.setOrientation(LinearLayout.HORIZONTAL);
            header.setGravity(Gravity.CENTER_VERTICAL);

            LinearLayout copy = new LinearLayout(context);
            copy.setOrientation(LinearLayout.VERTICAL);
            copy.addView(section(context, "魔女化值趋势"), fullWidthWrap());
            TextView detail = small(context, "最新记录 + 之前最多 " + MonvhuaHistory.PREVIOUS_POINTS_TO_SHOW
                    + " 点 · 变化超过 " + MonvhuaHistory.SIGNIFICANT_CHANGE_THRESHOLD + " 分才记录 · 图表点右键可撤回");
            detail.setTextColor(COLOR_MUTED);
            copy.addView(detail, fullWidthWrap());
            header.addView(copy, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

            Button toggle = actionButton(context, monvhuaLogicDetailsVisible ? "隐藏逻辑" : "逻辑显示",
                    monvhuaLogicDetailsVisible ? COLOR_CYAN : COLOR_GOLD, false);
            toggle.setOnClickListener(v -> {
                monvhuaLogicDetailsVisible = !monvhuaLogicDetailsVisible;
                rebuildEditor();
            });
            header.addView(toggle, actionButtonParams(108, SPACE));
            return header;
        }

        private View monvhuaHistoryLogicHint(Context context) {
            LinearLayout panel = panel(context, 0x44182029, 0x33425263);
            TextView rule = body(context, "记录规则：只显示最新记录和之前最多 "
                    + MonvhuaHistory.PREVIOUS_POINTS_TO_SHOW + " 个点；面板刷新会同步外部修改后的 > "
                    + MonvhuaHistory.SIGNIFICANT_CHANGE_THRESHOLD + " 分突变。");
            rule.setTextColor(COLOR_TEXT);
            panel.addView(rule, fullWidthWrap());

            TextView rollback = body(context, "撤回：在下方折线图中右键任一记录点，二次确认后回到该分值。");
            rollback.setTextColor(COLOR_MUTED);
            panel.addView(rollback, fullWidthWrapWithMargins(0, 2, 0, 0));
            return panel;
        }

        private EditorSnapshot.ScoreLevelEntry currentScoreLevel() {
            EditorSnapshot.ScoreLevelEntry current = null;
            for (EditorSnapshot.ScoreLevelEntry level : snapshot.scoreLevels()) {
                if (level.value() <= snapshot.selectedScore()) {
                    current = level;
                } else {
                    break;
                }
            }
            return current;
        }

        private View monvhuaHistoryChart(Context context) {
            return new MonvhuaHistoryChartPanel(context, snapshot.scoreHistory(), this::showMonvhuaRollbackDialog);
        }

        private void showMonvhuaRollbackDialog(EditorSnapshot.ScoreHistoryEntry entry) {
            UUID selected = snapshot.selectedPlayerUuid();
            if (selected == null) {
                setNotice("请先选择在线玩家", true);
                return;
            }

            Context context = requireContext();
            LinearLayout dialog = dialogPanel(context, "撤回魔女化值");
            TextView message = body(context, "确定将 " + snapshot.selectedPlayerName()
                    + " 的 monvhua 回到 " + entry.value() + "%（" + gameDayLabel(entry.day()) + "）?");
            message.setTextColor(COLOR_GOLD);
            dialog.addView(message, fullWidthWrap());

            LinearLayout actions = dialogActions(context);
            Button cancel = actionButton(context, "取消", COLOR_MUTED, false);
            cancel.setOnClickListener(v -> closeDialog());
            actions.addView(cancel, new LinearLayout.LayoutParams(92, ViewGroup.LayoutParams.WRAP_CONTENT));

            Button confirm = dangerButton(context, "撤回");
            confirm.setOnClickListener(v -> {
                closeDialog();
                send(new EditorPayloads.RollbackMonvhuaScoreC2S(selected, entry.value()));
            });
            actions.addView(confirm, new LinearLayout.LayoutParams(92, ViewGroup.LayoutParams.WRAP_CONTENT));
            dialog.addView(actions, fullWidthWrapWithMargins(0, SPACE, 0, 0));

            showDialog(dialog);
        }

        private View playerButton(Context context, EditorSnapshot.PlayerEntry player, boolean selected) {
            LinearLayout row = new LinearLayout(context);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setClickable(true);
            row.setPadding(8, 7, 10, 7);
            applyHoverBackground(row, selected ? 0x2266D99B : COLOR_PANEL_SOFT,
                    selected ? 0x3366D99B : COLOR_PANEL_HOVER,
                    selected ? COLOR_BORDER_ACTIVE : 0x223A4A5B,
                    selected ? COLOR_BORDER_ACTIVE : 0x44586B80);

            row.addView(playerAvatar(context, player.uuid(), PLAYER_LIST_AVATAR_SIZE),
                    new LinearLayout.LayoutParams(PLAYER_LIST_AVATAR_SIZE, PLAYER_LIST_AVATAR_SIZE));

            TextView label = body(context, player.name() + "  ·  " + player.tagCount());
            label.setTextColor(selected ? COLOR_GREEN : COLOR_TEXT);
            LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
            labelParams.setMargins(9, 0, 0, 0);
            row.addView(label, labelParams);
            return row;
        }

        private View playerAvatar(Context context, UUID uuid, int size) {
            FrameLayout shell = new FrameLayout(context);
            shell.setPadding(PLAYER_AVATAR_PADDING, PLAYER_AVATAR_PADDING, PLAYER_AVATAR_PADDING, PLAYER_AVATAR_PADDING);
            shell.setBackground(rounded(0xCC101820, 0x7766D99B, RADIUS_CONTROL));
            int surfaceSize = Math.max(1, size - PLAYER_AVATAR_PADDING * 2);
            shell.addView(new PlayerAvatarSurface(context, uuid), new FrameLayout.LayoutParams(surfaceSize, surfaceSize, Gravity.CENTER));
            return shell;
        }

        private Button tagButton(Context context, EditorSnapshot.TagEntry tag) {
            String prefix = tag.selected() ? "已启用  " : "未启用  ";
            Button button = flatButton(context, prefix + tag.displayName() + "  (" + tag.tag() + ")", tag.selected() ? COLOR_GREEN : COLOR_TEXT);
            button.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
            applyHoverBackground(button, tag.selected() ? 0x2266D99B : COLOR_PANEL_SOFT,
                    tag.selected() ? 0x3366D99B : COLOR_PANEL_HOVER,
                    tag.selected() ? COLOR_BORDER_ACTIVE : 0x223A4A5B,
                    tag.selected() ? COLOR_BORDER_ACTIVE : 0x44586B80);
            button.setPadding(10, 9, 10, 9);
            return button;
        }

        private Button scoreReferenceButton(Context context, EditorSnapshot.ScoreLevelEntry level, boolean current) {
            Button button = flatButton(context, (current ? "当前 · " : "") + level.value() + " · " + level.displayName(), current ? COLOR_GREEN : COLOR_TEXT);
            button.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
            applyHoverBackground(button, current ? 0x2266D99B : COLOR_PANEL_SOFT,
                    current ? 0x3366D99B : COLOR_PANEL_HOVER,
                    current ? COLOR_BORDER_ACTIVE : 0x223A4A5B,
                    current ? COLOR_BORDER_ACTIVE : 0x44586B80);
            button.setPadding(10, 9, 10, 9);
            return button;
        }

        private View tagChip(Context context, String text, boolean selected, boolean compact) {
            TextView chip = small(context, text);
            chip.setTextColor(selected ? COLOR_GREEN : COLOR_MUTED);
            chip.setPadding(compact ? 8 : 10, compact ? 4 : 6, compact ? 8 : 10, compact ? 4 : 6);
            chip.setBackground(rounded(selected ? 0x2266D99B : 0x4428323E, selected ? 0x6666D99B : 0x33425263, 99));
            return chip;
        }

        private View metric(Context context, String value, String label) {
            LinearLayout metric = new LinearLayout(context);
            metric.setOrientation(LinearLayout.VERTICAL);
            metric.setGravity(Gravity.RIGHT);
            TextView valueView = title(context, value);
            valueView.setTextColor(COLOR_GREEN);
            TextView labelView = small(context, label);
            labelView.setTextColor(COLOR_MUTED);
            metric.addView(valueView, fullWidthWrap());
            metric.addView(labelView, fullWidthWrap());
            return metric;
        }

        private View sectionHeader(Context context, String title, String detail) {
            LinearLayout header = new LinearLayout(context);
            header.setOrientation(LinearLayout.HORIZONTAL);
            header.setGravity(Gravity.CENTER_VERTICAL);
            TextView titleView = section(context, title);
            header.addView(titleView, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
            TextView detailView = small(context, detail);
            detailView.setTextColor(COLOR_MUTED);
            header.addView(detailView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            return header;
        }

        private LinearLayout titleBlock(Context context, String title, String subtitle) {
            LinearLayout block = new LinearLayout(context);
            block.setOrientation(LinearLayout.VERTICAL);
            block.addView(title(context, title), fullWidthWrap());
            TextView sub = small(context, subtitle);
            sub.setTextColor(COLOR_MUTED);
            block.addView(sub, fullWidthWrap());
            return block;
        }

        private TextView emptyState(Context context, String text) {
            TextView empty = small(context, text);
            empty.setTextColor(COLOR_FAINT);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(10, 14, 10, 14);
            empty.setBackground(rounded(0x33182029, 0x223A4A5B, RADIUS_CONTROL));
            return empty;
        }

        private void showAddCurrentTagDialog() {
            UUID selected = snapshot.selectedPlayerUuid();
            if (selected == null) {
                setNotice("请先选择在线玩家", true);
                return;
            }

            Context context = requireContext();
            LinearLayout dialog = dialogPanel(context, "手动添加 Tag");
            TextView hint = body(context, "直接添加到当前玩家身上，tag 只支持字母、数字、下划线、冒号、横线和点。");
            hint.setTextColor(COLOR_MUTED);
            dialog.addView(hint, fullWidthWrap());

            EditText tagInput = dialogInput(context, "输入 tag");
            dialog.addView(tagInput, fullWidthWrapWithMargins(0, SPACE, 0, 0));

            LinearLayout actions = dialogActions(context);
            Button cancel = actionButton(context, "取消", COLOR_MUTED, false);
            cancel.setOnClickListener(v -> closeDialog());
            actions.addView(cancel, new LinearLayout.LayoutParams(92, ViewGroup.LayoutParams.WRAP_CONTENT));

            Button confirm = actionButton(context, "确认", COLOR_GREEN, true);
            confirm.setOnClickListener(v -> {
                String tag = tagInput.getText().toString().trim();
                if (tag.isEmpty()) {
                    setNotice("Tag 不能为空", true);
                    return;
                }
                closeDialog();
                send(new EditorPayloads.AddPlayerTagC2S(selected, tag));
            });
            actions.addView(confirm, new LinearLayout.LayoutParams(92, ViewGroup.LayoutParams.WRAP_CONTENT));
            dialog.addView(actions, fullWidthWrapWithMargins(0, SPACE, 0, 0));

            showDialog(dialog);
            tagInput.requestFocus();
        }

        private void handleCurrentTagDeleteClick(View anchor, String tag) {
            UUID selected = snapshot.selectedPlayerUuid();
            if (selected == null) {
                setNotice("请先选择在线玩家", true);
                return;
            }

            if (tag.equals(pendingCurrentTagDeleteConfirm)) {
                pendingCurrentTagDeleteConfirm = null;
                pulse(anchor);
                send(new EditorPayloads.RemovePlayerTagC2S(selected, tag));
                return;
            }

            pendingCurrentTagDeleteConfirm = tag;
            anchor.postDelayed(() -> {
                if (tag.equals(pendingCurrentTagDeleteConfirm)) {
                    pendingCurrentTagDeleteConfirm = null;
                    showCurrentTagDeleteConfirmDialog(tag);
                }
            }, 280L);
        }

        private void showCurrentTagDeleteConfirmDialog(String tag) {
            UUID selected = snapshot.selectedPlayerUuid();
            if (selected == null) {
                setNotice("请先选择在线玩家", true);
                return;
            }

            Context context = requireContext();
            LinearLayout dialog = dialogPanel(context, "确认删除当前 Tag");
            TextView message = body(context, "确定从当前玩家移除 " + displayTagWithRawName(tag) + " ?");
            message.setTextColor(COLOR_GOLD);
            dialog.addView(message, fullWidthWrap());

            LinearLayout actions = dialogActions(context);
            Button cancel = actionButton(context, "取消", COLOR_MUTED, false);
            cancel.setOnClickListener(v -> closeDialog());
            actions.addView(cancel, new LinearLayout.LayoutParams(92, ViewGroup.LayoutParams.WRAP_CONTENT));

            Button confirm = dangerButton(context, "确认");
            confirm.setOnClickListener(v -> {
                closeDialog();
                send(new EditorPayloads.RemovePlayerTagC2S(selected, tag));
            });
            actions.addView(confirm, new LinearLayout.LayoutParams(92, ViewGroup.LayoutParams.WRAP_CONTENT));
            dialog.addView(actions, fullWidthWrapWithMargins(0, SPACE, 0, 0));

            showDialog(dialog);
        }

        private void showAddTagDialog(String categoryId) {
            UUID selected = snapshot.selectedPlayerUuid();
            if (selected == null) {
                setNotice("请先选择在线玩家", true);
                return;
            }

            Context context = requireContext();
            LinearLayout dialog = dialogPanel(context, "添加自定义 Tag");
            TextView hint = body(context, "tag 只支持字母、数字、下划线、冒号、横线和点。");
            hint.setTextColor(COLOR_MUTED);
            dialog.addView(hint, fullWidthWrap());

            EditText nameInput = dialogInput(context, "显示名称");
            dialog.addView(nameInput, fullWidthWrapWithMargins(0, SPACE, 0, 0));

            EditText tagInput = dialogInput(context, "实际写入的 tag");
            dialog.addView(tagInput, fullWidthWrapWithMargins(0, 8, 0, 0));

            LinearLayout actions = dialogActions(context);
            Button cancel = actionButton(context, "取消", COLOR_MUTED, false);
            cancel.setOnClickListener(v -> closeDialog());
            actions.addView(cancel, new LinearLayout.LayoutParams(92, ViewGroup.LayoutParams.WRAP_CONTENT));

            Button confirm = actionButton(context, "确认", COLOR_GREEN, true);
            confirm.setOnClickListener(v -> {
                String name = nameInput.getText().toString().trim();
                String tag = tagInput.getText().toString().trim();
                if (name.isEmpty()) {
                    setNotice("名称不能为空", true);
                    return;
                }
                if (tag.isEmpty()) {
                    setNotice("Tag 不能为空", true);
                    return;
                }
                closeDialog();
                tagDeleteMode = false;
                send(new EditorPayloads.AddPresetTagC2S(selected, categoryId, name, tag));
            });
            actions.addView(confirm, new LinearLayout.LayoutParams(92, ViewGroup.LayoutParams.WRAP_CONTENT));
            dialog.addView(actions, fullWidthWrapWithMargins(0, SPACE, 0, 0));

            showDialog(dialog);
            nameInput.requestFocus();
        }

        private void showDeleteConfirmDialog(String categoryId, String tag) {
            UUID selected = snapshot.selectedPlayerUuid();
            if (selected == null) {
                setNotice("请先选择在线玩家", true);
                return;
            }

            Context context = requireContext();
            LinearLayout dialog = dialogPanel(context, "确认移除面板项");
            TextView message = body(context, "确定从当前面板移除 " + displayNameFor(tag) + " (" + tag + ") ?");
            message.setTextColor(COLOR_GOLD);
            dialog.addView(message, fullWidthWrap());

            LinearLayout actions = dialogActions(context);
            Button cancel = actionButton(context, "取消", COLOR_MUTED, false);
            cancel.setOnClickListener(v -> closeDialog());
            actions.addView(cancel, new LinearLayout.LayoutParams(92, ViewGroup.LayoutParams.WRAP_CONTENT));

            Button confirm = dangerButton(context, "确认");
            confirm.setOnClickListener(v -> {
                closeDialog();
                send(new EditorPayloads.RemovePresetTagC2S(selected, categoryId, tag));
            });
            actions.addView(confirm, new LinearLayout.LayoutParams(92, ViewGroup.LayoutParams.WRAP_CONTENT));
            dialog.addView(actions, fullWidthWrapWithMargins(0, SPACE, 0, 0));

            showDialog(dialog);
        }

        private void showClearCategoryDialog(String categoryId) {
            UUID selected = snapshot.selectedPlayerUuid();
            if (selected == null) {
                setNotice("请先选择在线玩家", true);
                return;
            }

            Context context = requireContext();
            String label = TagCategory.fromId(categoryId).displayName();
            LinearLayout dialog = dialogPanel(context, "清空当前分类");
            TextView message = body(context, "将移除该玩家在 " + label + " 中已启用的预设 tags。");
            message.setTextColor(COLOR_GOLD);
            dialog.addView(message, fullWidthWrap());

            LinearLayout actions = dialogActions(context);
            Button cancel = actionButton(context, "取消", COLOR_MUTED, false);
            cancel.setOnClickListener(v -> closeDialog());
            actions.addView(cancel, new LinearLayout.LayoutParams(92, ViewGroup.LayoutParams.WRAP_CONTENT));

            Button confirm = dangerButton(context, "清空");
            confirm.setOnClickListener(v -> {
                closeDialog();
                send(new EditorPayloads.ClearPresetTagsC2S(selected, categoryId));
            });
            actions.addView(confirm, new LinearLayout.LayoutParams(92, ViewGroup.LayoutParams.WRAP_CONTENT));
            dialog.addView(actions, fullWidthWrapWithMargins(0, SPACE, 0, 0));

            showDialog(dialog);
        }

        private LinearLayout dialogPanel(Context context, String title) {
            LinearLayout dialog = panel(context, COLOR_PANEL, COLOR_BORDER_ACTIVE);
            dialog.addView(titleBlock(context, title, "请确认后继续"), fullWidthWrapWithMargins(0, 0, 0, SPACE));
            return dialog;
        }

        private EditText dialogInput(Context context, String hint) {
            EditText input = new EditText(context);
            input.setHint(hint);
            input.setSingleLine(true);
            input.setTextSize(14);
            input.setPadding(10, 8, 10, 8);
            input.setBackground(rounded(COLOR_PANEL_SOFT, COLOR_BORDER, RADIUS_CONTROL));
            return input;
        }

        private LinearLayout dialogActions(Context context) {
            LinearLayout actions = new LinearLayout(context);
            actions.setOrientation(LinearLayout.HORIZONTAL);
            actions.setGravity(Gravity.RIGHT);
            return actions;
        }

        private void showDialog(View dialog) {
            closeDialog();
            Context context = requireContext();

            FrameLayout overlay = new FrameLayout(context);
            overlay.setBackground(new ColorDrawable(0xAA000000));
            overlay.setOnClickListener(v -> closeDialog());

            dialog.setOnClickListener(v -> {
            });
            dialog.setScaleX(0.96f);
            dialog.setScaleY(0.96f);
            FrameLayout.LayoutParams dialogParams = new FrameLayout.LayoutParams(380, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER);
            overlay.addView(dialog, dialogParams);
            root.addView(overlay, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            dialogOverlay = overlay;
            fadeIn(overlay, 120L);
            ObjectAnimator.ofFloat(dialog, View.SCALE_X, 0.96f, 1.0f).setDuration(140L).start();
            ObjectAnimator.ofFloat(dialog, View.SCALE_Y, 0.96f, 1.0f).setDuration(140L).start();
        }

        private void closeDialog() {
            if (dialogOverlay != null && root != null) {
                root.removeView(dialogOverlay);
            }
            dialogOverlay = null;
        }

        private void showToast(String message, boolean error) {
            if (root == null || message == null || message.isBlank()) {
                return;
            }
            if (hideToastRunnable != null) {
                root.removeCallbacks(hideToastRunnable);
                hideToastRunnable = null;
            }
            if (toastView != null) {
                root.removeView(toastView);
                toastView = null;
            }

            TextView toast = body(requireContext(), message);
            toast.setTextColor(error ? COLOR_RED : COLOR_GREEN);
            toast.setPadding(14, 10, 14, 10);
            toast.setElevation(8.0f);
            toast.setBackground(rounded(error ? 0xEE2B1D25 : 0xEE172B24,
                    error ? 0xAAFF7777 : 0xAA66D99B, RADIUS_CONTROL));
            toast.setAlpha(0.0f);

            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(380,
                    ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP | Gravity.RIGHT);
            params.setMargins(0, -28, 28, 0);
            root.addView(toast, params);
            toastView = toast;

            animateToast(toast, -28, 24, 0.0f, 1.0f, 240L, true);
            hideToastRunnable = () -> hideToast(toast);
            root.postDelayed(hideToastRunnable, 2800L);
        }

        private void hideToast(TextView toast) {
            if (root == null || toastView != toast) {
                return;
            }
            animateToast(toast, 24, -28, 1.0f, 0.0f, 190L, false);
            root.postDelayed(() -> {
                if (root != null && toastView == toast) {
                    root.removeView(toast);
                    toastView = null;
                }
            }, 210L);
            hideToastRunnable = null;
        }

        private void animateToast(View view, int fromTop, int toTop, float fromAlpha, float toAlpha,
                                  long duration, boolean entering) {
            ValueAnimator move = ValueAnimator.ofFloat(0.0f, 1.0f);
            move.setDuration(duration);
            move.setInterpolator(entering
                    ? MotionEasingUtils.MOTION_EASING_STANDARD_DECELERATE
                    : MotionEasingUtils.MOTION_EASING_STANDARD_ACCELERATE);
            move.addUpdateListener(animation -> {
                Object value = animation.getAnimatedValue();
                float progress = value instanceof Number number ? number.floatValue() : animation.getAnimatedFraction();
                FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) view.getLayoutParams();
                int top = Math.round(fromTop + (toTop - fromTop) * progress);
                params.setMargins(0, top, 28, 0);
                view.setLayoutParams(params);
            });
            move.start();

            ObjectAnimator alpha = ObjectAnimator.ofFloat(view, View.ALPHA, fromAlpha, toAlpha);
            alpha.setDuration(duration);
            alpha.setInterpolator(MotionEasingUtils.MOTION_EASING_STANDARD);
            alpha.start();
        }

        private void refreshSnapshot() {
            UUID selected = snapshot.selectedPlayerUuid();
            if (selected != null) {
                send(new EditorPayloads.RefreshEditorC2S(selected));
            }
        }

        private void send(net.minecraft.network.packet.CustomPayload payload) {
            if (ClientPlayNetworking.canSend(payload.getId())) {
                ClientPlayNetworking.send(payload);
            } else {
                MinecraftClient client = MinecraftClient.getInstance();
                if (client.player != null) {
                    client.player.sendMessage(Text.literal("服务端未注册 Tag 管理台网络通道").formatted(Formatting.RED), false);
                }
            }
        }

        private View horizontalResizeHandle(Context context) {
            HorizontalResizeView wrapper = new HorizontalResizeView(context);
            wrapper.setPadding(5, 0, 5, 0);
            View line = new View(context);
            line.setBackground(rounded(0x667AA7FF, 0x00000000, 99));
            FrameLayout frame = new FrameLayout(context);
            frame.addView(line, new FrameLayout.LayoutParams(4, ViewGroup.LayoutParams.MATCH_PARENT, Gravity.CENTER));
            wrapper.addView(frame, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            wrapper.setOnTouchListener((view, event) -> {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN -> {
                        dragStartRawX = event.getRawX();
                        dragStartPlayersWidth = playersColumnWidth;
                        return true;
                    }
                    case MotionEvent.ACTION_MOVE -> {
                        int delta = Math.round(event.getRawX() - dragStartRawX);
                        playersColumnWidth = clamp(dragStartPlayersWidth + delta, minPlayersColumnWidth(), maxPlayersColumnWidth());
                        applyColumnWidths();
                        return true;
                    }
                    case MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        return true;
                    }
                    default -> {
                        return false;
                    }
                }
            });
            return wrapper;
        }

        private int minPlayersColumnWidth() {
            return Math.max(190, availableColumnWidth() / 8);
        }

        private int maxPlayersColumnWidth() {
            return Math.max(minPlayersColumnWidth(), availableColumnWidth() - 420);
        }

        private int availableColumnWidth() {
            if (contentRoot == null) {
                return playersColumnWidth + 800;
            }
            int width = contentRoot.getWidth() - contentRoot.getPaddingLeft() - contentRoot.getPaddingRight() - 14;
            return Math.max(10, width);
        }

        private void applyColumnWidths() {
            if (playersColumn == null || contentRoot == null) {
                return;
            }
            playersColumn.setLayoutParams(new LinearLayout.LayoutParams(playersColumnWidth, ViewGroup.LayoutParams.MATCH_PARENT));
            contentRoot.requestLayout();
        }

        private List<String> selectedTagsForCategory(TagCategory category) {
            List<String> tags = new ArrayList<>();
            for (String tag : snapshot.selectedTags()) {
                if (category.id().equals(categoryIdForSelectedTag(tag))) {
                    tags.add(tag);
                }
            }
            return tags;
        }

        private String categoryIdForSelectedTag(String tag) {
            for (EditorSnapshot.TagEntry presetTag : snapshot.presetTags()) {
                if (presetTag.tag().equals(tag)) {
                    return presetTag.categoryId();
                }
            }
            return TagCategory.forTag(tag).id();
        }

        private String displayTagWithRawName(String tag) {
            return displayNameFor(tag) + " (" + tag + ")";
        }

        private String displayNameFor(String tag) {
            String configured = snapshot.tagDisplayNames().get(tag);
            if (configured != null && !configured.isBlank()) {
                return configured;
            }
            return tag;
        }

        private int selectedTagCount() {
            return snapshot.selectedTags().size();
        }

        private int selectedPresetCount(String categoryId) {
            int count = 0;
            for (EditorSnapshot.TagEntry tag : snapshot.presetTags()) {
                if (categoryId.equals(tag.categoryId()) && tag.selected()) {
                    count++;
                }
            }
            return count;
        }

        private int clamp(int value, int min, int max) {
            return Math.max(min, Math.min(max, value));
        }

        private LinearLayout panel(Context context, int fill, int stroke) {
            LinearLayout panel = new LinearLayout(context);
            panel.setOrientation(LinearLayout.VERTICAL);
            panel.setPadding(12, 12, 12, 12);
            panel.setBackground(rounded(fill, stroke, RADIUS_PANEL));
            panel.setElevation(2.0f);
            return panel;
        }

        private TextView title(Context context, String text) {
            TextView view = new TextView(context);
            view.setText(text);
            view.setTextSize(22);
            view.setTextColor(COLOR_TEXT);
            view.setPadding(0, 0, 0, 4);
            return view;
        }

        private TextView section(Context context, String text) {
            TextView view = new TextView(context);
            view.setText(text);
            view.setTextSize(16);
            view.setTextColor(COLOR_CYAN);
            view.setPadding(0, 0, 0, 4);
            return view;
        }

        private TextView body(Context context, String text) {
            TextView view = new TextView(context);
            view.setText(text);
            view.setTextSize(14);
            view.setTextColor(COLOR_TEXT);
            view.setPadding(0, 4, 0, 4);
            return view;
        }

        private TextView small(Context context, String text) {
            TextView view = new TextView(context);
            view.setText(text);
            view.setTextSize(12);
            view.setTextColor(COLOR_MUTED);
            return view;
        }

        private TextView smallCaps(Context context, String text) {
            TextView view = small(context, text);
            view.setTextColor(COLOR_CYAN);
            return view;
        }

        private Button flatButton(Context context, String text, int color) {
            Button button = new Button(context);
            button.setText(text);
            button.setTextSize(14);
            button.setTextColor(color);
            button.setGravity(Gravity.CENTER);
            button.setPadding(8, 8, 8, 8);
            button.setClickable(true);
            return button;
        }

        private Button actionButton(Context context, String text, int color, boolean filled) {
            Button button = flatButton(context, text, filled ? 0xFF0D1512 : color);
            applyHoverBackground(button, filled ? color : 0x22182029, filled ? color : 0x33212B36, color, color);
            return button;
        }

        private Button dangerButton(Context context, String text) {
            Button button = flatButton(context, text, COLOR_RED);
            applyHoverBackground(button, 0x22FF7777, 0x33FF7777, 0x66FF7777, 0x88FF7777);
            return button;
        }

        private void applyHoverBackground(View view, int fill, int hoverFill, int stroke, int hoverStroke) {
            view.setBackground(ripple(fill, stroke, RADIUS_CONTROL));
            view.setOnHoverListener((hoveredView, event) -> {
                if (event.getAction() == MotionEvent.ACTION_HOVER_ENTER) {
                    hoveredView.setBackground(ripple(hoverFill, hoverStroke, RADIUS_CONTROL));
                    return true;
                }
                if (event.getAction() == MotionEvent.ACTION_HOVER_EXIT) {
                    hoveredView.setBackground(ripple(fill, stroke, RADIUS_CONTROL));
                    return true;
                }
                return false;
            });
        }

        private Drawable rounded(int fill, int stroke, int radius) {
            GradientDrawable drawable = new GradientDrawable();
            drawable.setShape(GradientDrawable.RECTANGLE);
            drawable.setColor(fill);
            drawable.setCornerRadius(radius);
            if (stroke != 0) {
                drawable.setStroke(1, stroke);
            }
            return drawable;
        }

        private Drawable ripple(int fill, int stroke, int radius) {
            return new RippleDrawable(ColorStateList.valueOf(0x4482D8FF), rounded(fill, stroke, radius), null);
        }

        private LayoutTransition layoutTransition() {
            LayoutTransition transition = new LayoutTransition();
            transition.setDuration(120L);
            transition.setAnimateParentHierarchy(false);
            return transition;
        }

        private void fadeIn(View view, long duration) {
            view.setAlpha(0.0f);
            ObjectAnimator.ofFloat(view, View.ALPHA, 0.0f, 1.0f).setDuration(duration).start();
        }

        private void fade(View view, float from, float to, long duration, icyllis.modernui.animation.TimeInterpolator interpolator) {
            ObjectAnimator alpha = ObjectAnimator.ofFloat(view, View.ALPHA, from, to);
            alpha.setDuration(duration);
            alpha.setInterpolator(interpolator);
            alpha.start();
        }

        private void pulse(View view) {
            view.setAlpha(0.82f);
            ObjectAnimator alpha = ObjectAnimator.ofFloat(view, View.ALPHA, 0.82f, 1.0f);
            alpha.setDuration(110L);
            alpha.setInterpolator(MotionEasingUtils.MOTION_EASING_STANDARD_DECELERATE);
            alpha.start();
        }

        private LinearLayout.LayoutParams actionButtonParams(int width, int leftMargin) {
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(width, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.setMargins(leftMargin, 0, 0, 0);
            return params;
        }

        private LinearLayout.LayoutParams metricParams() {
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(130, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.setMargins(SPACE, 0, 0, 0);
            return params;
        }

        private LinearLayout.LayoutParams fullWidthWrap() {
            return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        private LinearLayout.LayoutParams fullWidthWrapWithMargins(int left, int top, int right, int bottom) {
            LinearLayout.LayoutParams params = fullWidthWrap();
            params.setMargins(left, top, right, bottom);
            return params;
        }

        private LinearLayout.LayoutParams fullWidthFixedHeightWithMargins(int height, int left, int top, int right, int bottom) {
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height);
            params.setMargins(left, top, right, bottom);
            return params;
        }

        private LinearLayout.LayoutParams equalWidthWrap() {
            return new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        }

        private LinearLayout.LayoutParams match() {
            return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        }

        private ScrollView.LayoutParams scrollChild() {
            return new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        private void runOnUi(Runnable action) {
            if (root != null) {
                root.post(action);
            } else {
                MuiModApi.postToUiThread(action);
            }
        }

        private String shortUuid(UUID uuid) {
            String value = uuid.toString();
            return value.substring(0, 8) + "..." + value.substring(value.length() - 8);
        }
    }

    private static PointerIcon createVerticalResizeCursor() {
        try {
            Constructor<PointerIcon> constructor = PointerIcon.class.getDeclaredConstructor(int.class, long.class);
            constructor.setAccessible(true);
            return constructor.newInstance(-1, GLFW.glfwCreateStandardCursor(GLFW.GLFW_VRESIZE_CURSOR));
        } catch (ReflectiveOperationException e) {
            return PointerIcon.getSystemIcon(PointerIcon.TYPE_ARROW);
        }
    }

    private static PointerIcon createHorizontalResizeCursor() {
        try {
            Constructor<PointerIcon> constructor = PointerIcon.class.getDeclaredConstructor(int.class, long.class);
            constructor.setAccessible(true);
            return constructor.newInstance(-1, GLFW.glfwCreateStandardCursor(GLFW.GLFW_HRESIZE_CURSOR));
        } catch (ReflectiveOperationException e) {
            return PointerIcon.getSystemIcon(PointerIcon.TYPE_ARROW);
        }
    }

    private static SkinTextures skinTextures(UUID uuid) {
        if (uuid == null) {
            return null;
        }
        ClientPlayNetworkHandler handler = MinecraftClient.getInstance().getNetworkHandler();
        if (handler == null) {
            return null;
        }
        PlayerListEntry entry = handler.getPlayerListEntry(uuid);
        return entry == null ? null : entry.getSkinTextures();
    }

    private static int alphaColor(float alpha, int rgb) {
        int value = Math.max(0, Math.min(255, Math.round(alpha * 255.0f)));
        return (value << 24) | (rgb & 0x00FFFFFF);
    }

    private static boolean isSecondaryButtonPress(MotionEvent event) {
        int action = event.getActionMasked();
        return (action == MotionEvent.ACTION_BUTTON_PRESS && event.getActionButton() == MotionEvent.BUTTON_SECONDARY)
                || (action == MotionEvent.ACTION_DOWN && (event.getButtonState() & MotionEvent.BUTTON_SECONDARY) != 0);
    }

    @FunctionalInterface
    private interface ScoreHistoryPointHandler {
        void handle(EditorSnapshot.ScoreHistoryEntry entry);
    }

    private static final class MonvhuaHistoryChartPanel extends FrameLayout {
        private static final int VALUE_LABEL_WIDTH = 52;
        private static final int DAY_LABEL_WIDTH = 72;
        private static final int LABEL_HEIGHT = 22;
        private static final int CHART_HEIGHT = 600;
        private static final int CHART_RADIUS = 7;
        private static final int CHART_MUTED = 0xFF93A0AE;

        private final List<EditorSnapshot.ScoreHistoryEntry> history;
        private final MonvhuaHistoryChartView chartView;
        private final TextView topAxisLabel;
        private final TextView bottomAxisLabel;
        private final TextView emptyLabel;
        private final List<TextView> valueLabels = new ArrayList<>();
        private final List<TextView> dayLabels = new ArrayList<>();
        private final ScoreHistoryPointHandler pointHandler;

        private MonvhuaHistoryChartPanel(Context context, List<EditorSnapshot.ScoreHistoryEntry> history,
                                         ScoreHistoryPointHandler pointHandler) {
            super(context);
            this.history = history == null ? List.of() : List.copyOf(history);
            this.pointHandler = pointHandler;
            setPadding(8, 8, 8, 8);
            setMinimumHeight(CHART_HEIGHT);
            setBackground(chartBackground(0x44182029, 0x33425263, CHART_RADIUS));

            chartView = new MonvhuaHistoryChartView(context, this.history, pointHandler);
            addView(chartView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

            topAxisLabel = chartLabel(context, "100", CHART_MUTED, 15);
            bottomAxisLabel = chartLabel(context, "0", CHART_MUTED, 15);
            addView(topAxisLabel, labelParams(VALUE_LABEL_WIDTH, LABEL_HEIGHT));
            addView(bottomAxisLabel, labelParams(VALUE_LABEL_WIDTH, LABEL_HEIGHT));

            emptyLabel = chartLabel(context, "暂无历史记录", CHART_MUTED, 15);
            emptyLabel.setVisibility(this.history.isEmpty() ? View.VISIBLE : View.GONE);
            addView(emptyLabel, labelParams(140, LABEL_HEIGHT));

            for (EditorSnapshot.ScoreHistoryEntry entry : this.history) {
                int value = clampChartValue(entry.value());
                TextView valueLabel = chartLabel(context, value + "%", 0xFFD8F7FF, 14);
                TextView dayLabel = chartLabel(context, gameDayLabel(entry.day()), CHART_MUTED, 14);
                attachPointHandler(valueLabel, entry);
                attachPointHandler(dayLabel, entry);
                valueLabels.add(valueLabel);
                dayLabels.add(dayLabel);
                addView(valueLabel, labelParams(VALUE_LABEL_WIDTH, LABEL_HEIGHT));
                addView(dayLabel, labelParams(DAY_LABEL_WIDTH, LABEL_HEIGHT));
            }
        }

        private void attachPointHandler(View label, EditorSnapshot.ScoreHistoryEntry entry) {
            label.setOnGenericMotionListener((view, event) -> {
                if (pointHandler != null && isSecondaryButtonPress(event)) {
                    pointHandler.handle(entry);
                    return true;
                }
                return false;
            });
        }

        @Override
        protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
            int width = right - left;
            int height = bottom - top;
            int contentLeft = getPaddingLeft();
            int contentTop = getPaddingTop();
            int contentRight = Math.max(contentLeft, width - getPaddingRight());
            int contentBottom = Math.max(contentTop, height - getPaddingBottom());

            chartView.layout(contentLeft, contentTop, contentRight, contentBottom);

            ChartGeometry geometry = chartGeometry(contentRight - contentLeft, contentBottom - contentTop);
            layoutLabel(topAxisLabel, contentLeft + Math.round(geometry.left()) - VALUE_LABEL_WIDTH, contentTop + Math.round(geometry.top()) - 10,
                    VALUE_LABEL_WIDTH, LABEL_HEIGHT);
            layoutLabel(bottomAxisLabel, contentLeft + Math.round(geometry.left()) - VALUE_LABEL_WIDTH, contentTop + Math.round(geometry.bottom()) - 10,
                    VALUE_LABEL_WIDTH, LABEL_HEIGHT);
            layoutLabel(emptyLabel, contentLeft + (contentRight - contentLeft - 140) / 2, contentTop + (contentBottom - contentTop - LABEL_HEIGHT) / 2,
                    140, LABEL_HEIGHT);

            int count = history.size();
            for (int i = 0; i < count; i++) {
                EditorSnapshot.ScoreHistoryEntry entry = history.get(i);
                float x = dataX(geometry, i, count);
                float y = dataY(geometry, clampChartValue(entry.value()));
                int labelY = Math.round(y < geometry.top() + 24.0f ? y + 12.0f : y - 24.0f);
                layoutLabel(valueLabels.get(i), contentLeft + Math.round(x) - VALUE_LABEL_WIDTH / 2, contentTop + labelY,
                        VALUE_LABEL_WIDTH, LABEL_HEIGHT);
                layoutLabel(dayLabels.get(i), contentLeft + Math.round(x) - DAY_LABEL_WIDTH / 2, contentTop + Math.round(geometry.bottom()) + 12,
                        DAY_LABEL_WIDTH, LABEL_HEIGHT);
            }
        }

        private static TextView chartLabel(Context context, String text, int color, int textSize) {
            TextView label = new TextView(context);
            label.setText(text);
            label.setTextColor(color);
            label.setTextSize(textSize);
            label.setGravity(Gravity.CENTER);
            label.setIncludeFontPadding(false);
            label.setClickable(false);
            return label;
        }

        private static FrameLayout.LayoutParams labelParams(int width, int height) {
            return new FrameLayout.LayoutParams(width, height);
        }

        private static void layoutLabel(View label, int left, int top, int width, int height) {
            label.layout(left, top, left + width, top + height);
        }

        private static Drawable chartBackground(int fill, int stroke, int radius) {
            GradientDrawable drawable = new GradientDrawable();
            drawable.setColor(fill);
            drawable.setStroke(1, stroke);
            drawable.setCornerRadius(radius);
            return drawable;
        }
    }

    private static final class MonvhuaHistoryChartView extends View {
        private static final float POINT_HIT_RADIUS = 16.0f;

        private final List<EditorSnapshot.ScoreHistoryEntry> history;
        private final ScoreHistoryPointHandler pointHandler;
        private final Paint backgroundPaint = new Paint();
        private final Paint gridPaint = new Paint();
        private final Paint axisPaint = new Paint();
        private final Paint linePaint = new Paint();
        private final Paint pointPaint = new Paint();
        private final Paint pointCorePaint = new Paint();

        private MonvhuaHistoryChartView(Context context, List<EditorSnapshot.ScoreHistoryEntry> history,
                                        ScoreHistoryPointHandler pointHandler) {
            super(context);
            this.history = history == null ? List.of() : List.copyOf(history);
            this.pointHandler = pointHandler;
            setWillNotDraw(false);

            backgroundPaint.setColor(0xFF121922);
            backgroundPaint.setStyle(Paint.Style.FILL);

            gridPaint.setColor(0x66425263);
            gridPaint.setAntiAlias(true);
            gridPaint.setStyle(Paint.Style.STROKE);
            gridPaint.setStrokeWidth(1.2f);

            axisPaint.setColor(0xFF66D99B);
            axisPaint.setAntiAlias(true);
            axisPaint.setStyle(Paint.Style.STROKE);
            axisPaint.setStrokeWidth(2.0f);
            axisPaint.setStrokeCap(Paint.Cap.ROUND);

            linePaint.setColor(0xFF78D7FF);
            linePaint.setAntiAlias(true);
            linePaint.setStyle(Paint.Style.STROKE);
            linePaint.setStrokeWidth(2.0f);
            linePaint.setStrokeCap(Paint.Cap.ROUND);
            linePaint.setStrokeJoin(Paint.Join.ROUND);

            pointPaint.setColor(0xFF66D99B);
            pointPaint.setAntiAlias(true);
            pointPaint.setStyle(Paint.Style.FILL);

            pointCorePaint.setColor(0xFFD9FFF0);
            pointCorePaint.setAntiAlias(true);
            pointCorePaint.setStyle(Paint.Style.FILL);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            int width = Math.max(1, getWidth());
            int height = Math.max(1, getHeight());
            ChartGeometry geometry = chartGeometry(width, height);

            canvas.drawRect(0.0f, 0.0f, width, height, backgroundPaint);
            for (int i = 0; i <= 4; i++) {
                float y = geometry.top() + geometry.plotHeight() * (i / 4.0f);
                canvas.drawLine(geometry.left(), y, geometry.right(), y, gridPaint);
            }
            canvas.drawLine(geometry.left(), geometry.top(), geometry.left(), geometry.bottom(), axisPaint);
            canvas.drawLine(geometry.left(), geometry.bottom(), geometry.right(), geometry.bottom(), axisPaint);

            if (history.isEmpty()) {
                return;
            }

            int count = history.size();
            float previousX = 0.0f;
            float previousY = 0.0f;
            for (int i = 0; i < count; i++) {
                EditorSnapshot.ScoreHistoryEntry entry = history.get(i);
                float x = dataX(geometry, i, count);
                float y = dataY(geometry, clampChartValue(entry.value()));
                if (i > 0) {
                    canvas.drawLine(previousX, previousY, x, y, linePaint);
                }
                canvas.drawCircle(x, y, 4.4f, pointPaint);
                canvas.drawCircle(x, y, 2.0f, pointCorePaint);
                previousX = x;
                previousY = y;
            }
        }

        @Override
        public boolean onGenericMotionEvent(MotionEvent event) {
            if (isSecondaryButtonPress(event) && triggerPointRollback(event.getX(), event.getY())) {
                return true;
            }
            return super.onGenericMotionEvent(event);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if (isSecondaryButtonPress(event) && triggerPointRollback(event.getX(), event.getY())) {
                return true;
            }
            return super.onTouchEvent(event);
        }

        private boolean triggerPointRollback(float touchX, float touchY) {
            if (pointHandler == null || history.isEmpty()) {
                return false;
            }

            int index = pointIndexAt(touchX, touchY);
            if (index < 0) {
                return false;
            }

            pointHandler.handle(history.get(index));
            return true;
        }

        private int pointIndexAt(float touchX, float touchY) {
            int width = Math.max(1, getWidth());
            int height = Math.max(1, getHeight());
            ChartGeometry geometry = chartGeometry(width, height);
            int count = history.size();
            float bestDistance = POINT_HIT_RADIUS * POINT_HIT_RADIUS;
            int bestIndex = -1;

            for (int i = 0; i < count; i++) {
                EditorSnapshot.ScoreHistoryEntry entry = history.get(i);
                float pointX = dataX(geometry, i, count);
                float pointY = dataY(geometry, clampChartValue(entry.value()));
                float dx = touchX - pointX;
                float dy = touchY - pointY;
                float distance = dx * dx + dy * dy;
                if (distance <= bestDistance) {
                    bestDistance = distance;
                    bestIndex = i;
                }
            }
            return bestIndex;
        }
    }

    private record ChartGeometry(float left, float top, float right, float bottom) {
        private float plotWidth() {
            return Math.max(1.0f, right - left);
        }

        private float plotHeight() {
            return Math.max(1.0f, bottom - top);
        }
    }

    private static ChartGeometry chartGeometry(int width, int height) {
        float left = 58.0f;
        float top = 42.0f;
        float right = Math.max(left + 24.0f, width - 22.0f);
        float bottom = Math.max(top + 24.0f, height - 64.0f);
        return new ChartGeometry(left, top, right, bottom);
    }

    private static float dataX(ChartGeometry geometry, int index, int count) {
        return count == 1
                ? geometry.left() + geometry.plotWidth() / 2.0f
                : geometry.left() + geometry.plotWidth() * (index / (float) (count - 1));
    }

    private static float dataY(ChartGeometry geometry, int value) {
        return geometry.top() + (100 - clampChartValue(value)) * (geometry.plotHeight() / 100.0f);
    }

    private static int clampChartValue(int value) {
        return Math.max(0, Math.min(100, value));
    }

    private static String gameDayLabel(String day) {
        if (day == null || day.isBlank()) {
            return "";
        }
        try {
            long gameDay = Long.parseLong(day);
            return "第" + gameDay + "天";
        } catch (NumberFormatException ignored) {
            return "";
        }
    }

    private static final class MonvhuaHistoryChartSurface extends MinecraftSurfaceView {
        private final List<EditorSnapshot.ScoreHistoryEntry> history;
        private int surfaceWidth;
        private int surfaceHeight;

        private MonvhuaHistoryChartSurface(Context context, List<EditorSnapshot.ScoreHistoryEntry> history) {
            super(context);
            this.history = history == null ? List.of() : List.copyOf(history);
            setRenderer(new Renderer() {
                @Override
                public void onSurfaceChanged(int width, int height) {
                    surfaceWidth = width;
                    surfaceHeight = height;
                }

                @Override
                public void onDraw(DrawContext context, int mouseX, int mouseY, float deltaTick, double guiScale, float alpha) {
                    int width = Math.max(1, (int) Math.floor(surfaceWidth / Math.max(1.0d, guiScale)));
                    int height = Math.max(1, (int) Math.floor(surfaceHeight / Math.max(1.0d, guiScale)));
                    context.fill(0, 0, width, height, alphaColor(alpha, 0x121922));

                    int left = 34;
                    int top = 16;
                    int right = Math.max(left + 20, width - 10);
                    int bottom = Math.max(top + 20, height - 28);
                    int plotWidth = Math.max(1, right - left);
                    int plotHeight = Math.max(1, bottom - top);

                    for (int i = 0; i <= 4; i++) {
                        int y = top + Math.round(plotHeight * (i / 4.0f));
                        context.fill(left, y, right, y + 1, alphaColor(alpha, 0x33425263));
                    }
                    context.fill(left, top, left + 1, bottom, alphaColor(alpha, 0x5566D99B));
                    context.fill(left, bottom, right, bottom + 1, alphaColor(alpha, 0x5566D99B));

                    if (history.isEmpty()) {
                        context.drawCenteredTextWithShadow(MinecraftClient.getInstance().textRenderer,
                                "暂无历史记录", width / 2, Math.max(0, height / 2 - 4), alphaColor(alpha, 0x93A0AE));
                        return;
                    }

                    int previousX = -1;
                    int previousY = -1;
                    int count = history.size();
                    for (int i = 0; i < count; i++) {
                        EditorSnapshot.ScoreHistoryEntry entry = history.get(i);
                        int x = count == 1 ? left + plotWidth / 2 : left + Math.round(plotWidth * (i / (float) (count - 1)));
                        int value = clampChartValue(entry.value());
                        int y = top + Math.round((100 - value) * (plotHeight / 100.0f));

                        if (previousX >= 0) {
                            drawAntiAliasedLine(context, previousX, previousY, x, y, alpha, 0x78D7FF);
                        }
                        context.fill(x - 2, y - 2, x + 3, y + 3, alphaColor(alpha, 0x66D99B));
                        context.fill(x - 1, y - 1, x + 2, y + 2, alphaColor(alpha, 0xD9FFF0));

                        int valueLabelY = y < top + 18 ? y + 10 : y - 16;
                        context.drawCenteredTextWithShadow(MinecraftClient.getInstance().textRenderer,
                                value + "%", x, valueLabelY, alphaColor(alpha, 0xD8F7FF));

                        String label = gameDayLabel(entry.day());
                        context.drawCenteredTextWithShadow(MinecraftClient.getInstance().textRenderer,
                                label, x, bottom + 7, alphaColor(alpha, 0x93A0AE));

                        previousX = x;
                        previousY = y;
                    }

                    context.drawTextWithShadow(MinecraftClient.getInstance().textRenderer, "100", 4, top - 4, alphaColor(alpha, 0x93A0AE));
                    context.drawTextWithShadow(MinecraftClient.getInstance().textRenderer, "0", 18, bottom - 6, alphaColor(alpha, 0x93A0AE));
                }
            });
        }

        private static void drawAntiAliasedLine(DrawContext context, int startX, int startY, int endX, int endY, float alpha, int rgb) {
            if (startX == endX && startY == endY) {
                plotLinePixel(context, false, startX, startY, alpha, rgb);
                return;
            }

            boolean steep = Math.abs(endY - startY) > Math.abs(endX - startX);
            if (steep) {
                int oldStartX = startX;
                startX = startY;
                startY = oldStartX;
                int oldEndX = endX;
                endX = endY;
                endY = oldEndX;
            }

            if (startX > endX) {
                int oldStartX = startX;
                int oldStartY = startY;
                startX = endX;
                startY = endY;
                endX = oldStartX;
                endY = oldStartY;
            }

            int dx = endX - startX;
            int dy = endY - startY;
            double gradient = dx == 0 ? 1.0d : dy / (double) dx;

            double firstY = startY;
            int firstPixelY = integerPart(firstY);
            plotLinePixel(context, steep, startX, firstPixelY, alpha * reverseFractionalPart(firstY), rgb);
            plotLinePixel(context, steep, startX, firstPixelY + 1, alpha * fractionalPart(firstY), rgb);

            double y = startY + gradient;
            for (int x = startX + 1; x < endX; x++) {
                int pixelY = integerPart(y);
                plotLinePixel(context, steep, x, pixelY, alpha * reverseFractionalPart(y), rgb);
                plotLinePixel(context, steep, x, pixelY + 1, alpha * fractionalPart(y), rgb);
                y += gradient;
            }

            double lastY = endY;
            int lastPixelY = integerPart(lastY);
            plotLinePixel(context, steep, endX, lastPixelY, alpha * reverseFractionalPart(lastY), rgb);
            plotLinePixel(context, steep, endX, lastPixelY + 1, alpha * fractionalPart(lastY), rgb);
        }

        private static void plotLinePixel(DrawContext context, boolean steep, int x, int y, float coverage, int rgb) {
            if (coverage <= 0.0f) {
                return;
            }
            int color = alphaColor(Math.min(1.0f, coverage), rgb);
            if (steep) {
                context.fill(y, x, y + 1, x + 1, color);
            } else {
                context.fill(x, y, x + 1, y + 1, color);
            }
        }

        private static int integerPart(double value) {
            return (int) Math.floor(value);
        }

        private static float fractionalPart(double value) {
            return (float) (value - Math.floor(value));
        }

        private static float reverseFractionalPart(double value) {
            return 1.0f - fractionalPart(value);
        }

        private static int clampChartValue(int value) {
            return Math.max(0, Math.min(100, value));
        }

        private static String gameDayLabel(String day) {
            if (day == null || day.isBlank()) {
                return "";
            }
            try {
                long gameDay = Long.parseLong(day);
                return "第" + gameDay + "天";
            } catch (NumberFormatException ignored) {
                return "";
            }
        }
    }

    private static final class PlayerAvatarSurface extends MinecraftSurfaceView {
        private final UUID uuid;
        private int surfaceWidth;
        private int surfaceHeight;

        private PlayerAvatarSurface(Context context, UUID uuid) {
            super(context);
            this.uuid = uuid;
            setRenderer(new Renderer() {
                @Override
                public void onSurfaceChanged(int width, int height) {
                    surfaceWidth = width;
                    surfaceHeight = height;
                }

                @Override
                public void onDraw(DrawContext context, int mouseX, int mouseY, float deltaTick, double guiScale, float alpha) {
                    int viewWidth = Math.max(1, (int) Math.floor(surfaceWidth / Math.max(1.0d, guiScale)));
                    int viewHeight = Math.max(1, (int) Math.floor(surfaceHeight / Math.max(1.0d, guiScale)));
                    int drawSize = Math.max(1, Math.min(viewWidth, viewHeight));
                    int drawX = Math.max(0, (viewWidth - drawSize) / 2);
                    int drawY = Math.max(0, (viewHeight - drawSize) / 2);
                    context.fill(drawX, drawY, drawX + drawSize, drawY + drawSize, alphaColor(alpha, 0x17212B));
                    SkinTextures skin = skinTextures(uuid);
                    if (skin != null) {
                        PlayerSkinDrawer.draw(context, skin, drawX, drawY, drawSize, alphaColor(alpha, 0xFFFFFF));
                    } else {
                        int color = alphaColor(alpha, 0x93A0AE);
                        context.drawCenteredTextWithShadow(MinecraftClient.getInstance().textRenderer,
                                "?", drawX + drawSize / 2, drawY + Math.max(0, (drawSize - 8) / 2), color);
                    }
                }
            });
        }
    }

    private static final class HorizontalResizeView extends FrameLayout {
        private HorizontalResizeView(Context context) {
            super(context);
        }

        @Override
        public PointerIcon onResolvePointerIcon(MotionEvent event) {
            return HORIZONTAL_RESIZE_CURSOR;
        }
    }
}
