package com.lantern.onlineplayertageditor.client;

import com.google.gson.JsonSyntaxException;
import com.lantern.onlineplayertageditor.network.EditorNetworking;
import com.lantern.onlineplayertageditor.network.EditorPayloads;
import com.lantern.onlineplayertageditor.network.EditorSnapshot;
import com.lantern.onlineplayertageditor.network.TagCategory;
import icyllis.modernui.core.Context;
import icyllis.modernui.fragment.Fragment;
import icyllis.modernui.graphics.drawable.ColorDrawable;
import icyllis.modernui.mc.MuiModApi;
import icyllis.modernui.mc.MuiScreen;
import icyllis.modernui.mc.ScreenCallback;
import icyllis.modernui.text.Editable;
import icyllis.modernui.text.TextWatcher;
import icyllis.modernui.view.Gravity;
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
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Constructor;
import java.util.Locale;
import java.util.UUID;

public final class ModernTagEditorScreen {
    private static final PointerIcon VERTICAL_RESIZE_CURSOR = createVerticalResizeCursor();
    private static final PointerIcon HORIZONTAL_RESIZE_CURSOR = createHorizontalResizeCursor();
    private static EditorFragment currentFragment;
    private static String activeTab = TagCategory.CHARACTER.id();
    private static String searchText = "";
    private static int playersColumnWidth = 250;
    private static int editorColumnWidth = 430;

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
        private EditorSnapshot snapshot;
        private FrameLayout root;
        private LinearLayout contentRoot;
        private LinearLayout playerList;
        private LinearLayout detailPanel;
        private LinearLayout editorPanel;
        private LinearLayout editorActions;
        private View playersColumn;
        private View detailColumn;
        private View editorColumn;
        private View dialogOverlay;
        private TextView noticeView;
        private boolean noticeIsError;
        private String noticeMessage = "";
        private boolean tagDeleteMode;
        private float dragStartRawX;
        private int dragStartPlayersWidth;
        private int dragStartEditorWidth;

        private EditorFragment(EditorSnapshot snapshot) {
            this.snapshot = snapshot;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 icyllis.modernui.util.DataSet savedInstanceState) {
            Context context = requireContext();

            root = new FrameLayout(context);
            root.setBackground(new ColorDrawable(0xCC101318));
            root.setLayoutParams(match());

            contentRoot = new LinearLayout(context);
            contentRoot.setOrientation(LinearLayout.HORIZONTAL);
            contentRoot.setPadding(14, 14, 14, 14);
            playersColumn = buildPlayersColumn(context);
            detailColumn = buildDetailColumn(context);
            editorColumn = buildEditorColumn(context);
            contentRoot.addView(playersColumn, new LinearLayout.LayoutParams(playersColumnWidth, ViewGroup.LayoutParams.MATCH_PARENT));
            contentRoot.addView(horizontalResizeHandle(context, true), new LinearLayout.LayoutParams(10, ViewGroup.LayoutParams.MATCH_PARENT));
            contentRoot.addView(detailColumn, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1.0f));
            contentRoot.addView(horizontalResizeHandle(context, false), new LinearLayout.LayoutParams(10, ViewGroup.LayoutParams.MATCH_PARENT));
            contentRoot.addView(editorColumn, new LinearLayout.LayoutParams(editorColumnWidth, ViewGroup.LayoutParams.MATCH_PARENT));
            root.addView(contentRoot, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

            rebuild();
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
                if (noticeView == null) {
                    return;
                }
                noticeView.setText(message);
                noticeView.setTextColor(error ? 0xFFFF6B6B : 0xFF69D28D);
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
            detailPanel = null;
            editorPanel = null;
            editorActions = null;
            playersColumn = null;
            detailColumn = null;
            editorColumn = null;
            dialogOverlay = null;
            noticeView = null;
        }

        private View buildPlayersColumn(Context context) {
            LinearLayout column = panel(context);
            column.addView(title(context, "在线玩家"));

            EditText search = new EditText(context);
            search.setHint("搜索玩家");
            search.setText(searchText);
            search.setSingleLine(true);
            search.setTextSize(14);
            search.setPadding(8, 8, 8, 8);
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
            column.addView(search, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            ScrollView scroll = new ScrollView(context);
            playerList = new LinearLayout(context);
            playerList.setOrientation(LinearLayout.VERTICAL);
            scroll.addView(playerList, scrollChild());
            column.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f));

            Button refresh = button(context, "刷新列表");
            refresh.setOnClickListener(v -> refreshSnapshot());
            column.addView(refresh, fullWidthWrap());
            column.addView(resizeBoundary(context), fullWidthFixed(8));
            return framedPanel(context, column);
        }

        private View buildDetailColumn(Context context) {
            LinearLayout column = panel(context);
            ScrollView scroll = new ScrollView(context);
            detailPanel = new LinearLayout(context);
            detailPanel.setOrientation(LinearLayout.VERTICAL);
            scroll.addView(detailPanel, scrollChild());
            column.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f));
            column.addView(resizeBoundary(context), fullWidthFixed(8));
            return framedPanel(context, column);
        }

        private View buildEditorColumn(Context context) {
            LinearLayout column = panel(context);
            noticeView = body(context, noticeMessage);
            noticeView.setTextColor(noticeIsError ? 0xFFFF6B6B : 0xFF69D28D);
            column.addView(noticeView, fullWidthWrap());

            LinearLayout tabs = new LinearLayout(context);
            tabs.setOrientation(LinearLayout.HORIZONTAL);
            tabs.addView(tabButton(context, TagCategory.CHARACTER.id(), "角色"), equalWidthWrap());
            tabs.addView(tabButton(context, TagCategory.MAGIC.id(), "魔法"), equalWidthWrap());
            tabs.addView(tabButton(context, TagCategory.IDENTITY.id(), "身份"), equalWidthWrap());
            tabs.addView(tabButton(context, "monvhua", "monvhua"), equalWidthWrap());
            column.addView(tabs, fullWidthWrap());

            ScrollView scroll = new ScrollView(context);
            editorPanel = new LinearLayout(context);
            editorPanel.setOrientation(LinearLayout.VERTICAL);
            scroll.addView(editorPanel, scrollChild());
            column.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f));

            editorActions = new LinearLayout(context);
            editorActions.setOrientation(LinearLayout.HORIZONTAL);
            editorActions.setGravity(Gravity.RIGHT);
            column.addView(editorActions, fullWidthWrap());
            column.addView(resizeBoundary(context), fullWidthFixed(8));
            return framedPanel(context, column);
        }

        private View tabButton(Context context, String tab, String label) {
            Button button = button(context, activeTab.equals(tab) ? "● " + label : label);
            button.setOnClickListener(v -> {
                activeTab = tab;
                tagDeleteMode = false;
                rebuild();
            });
            return button;
        }

        private void rebuild() {
            if (root == null) {
                return;
            }
            rebuildPlayerList();
            rebuildDetail();
            rebuildEditor();
        }

        private void rebuildPlayerList() {
            if (playerList == null) {
                return;
            }
            playerList.removeAllViews();
            Context context = requireContext();
            String filter = searchText.toLowerCase(Locale.ROOT);

            for (EditorSnapshot.PlayerEntry player : snapshot.players()) {
                if (!filter.isBlank() && !player.name().toLowerCase(Locale.ROOT).contains(filter)) {
                    continue;
                }
                boolean selected = player.uuid().equals(snapshot.selectedPlayerUuid());
                Button entry = button(context, (selected ? "● " : "") + player.name() + "  (" + player.tagCount() + ")");
                entry.setTextColor(selected ? 0xFF69D28D : 0xFFECEFF4);
                entry.setOnClickListener(v -> send(new EditorPayloads.RefreshEditorC2S(player.uuid())));
                playerList.addView(entry, fullWidthWrap());
            }
        }

        private void rebuildDetail() {
            detailPanel.removeAllViews();
            Context context = requireContext();
            detailPanel.addView(title(context, snapshot.selectedPlayerName()));
            detailPanel.addView(body(context, snapshot.selectedPlayerOnline() ? "在线" : "离线，无法写入"));
            if (snapshot.selectedPlayerUuid() != null) {
                detailPanel.addView(body(context, shortUuid(snapshot.selectedPlayerUuid())));
            }
            detailPanel.addView(section(context, "当前 Tags"));
            if (snapshot.selectedTags().isEmpty()) {
                detailPanel.addView(body(context, "无"));
            } else {
                detailPanel.addView(body(context, String.join(", ", snapshot.selectedTags())));
            }
            detailPanel.addView(section(context, "monvhua"));
            if (snapshot.scoreboardObjectiveExists()) {
                detailPanel.addView(body(context, snapshot.selectedScore() + " / " + snapshot.selectedStageName()));
            } else {
                TextView warning = body(context, "objective monvhua 不存在");
                warning.setTextColor(0xFFFF6B6B);
                detailPanel.addView(warning);
            }
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
            editorPanel.addView(section(context, TagCategory.fromId(categoryId).displayName()));
            for (EditorSnapshot.TagEntry tag : snapshot.presetTags()) {
                if (!categoryId.equals(tag.categoryId())) {
                    continue;
                }
                Button tagButton = button(context, (tag.selected() ? "已拥有  " : "未拥有  ") + tag.displayName());
                tagButton.setTextColor(tag.selected() ? 0xFF69D28D : 0xFFECEFF4);
                tagButton.setOnClickListener(v -> {
                    if (snapshot.selectedPlayerUuid() != null) {
                        send(new EditorPayloads.ToggleTagC2S(snapshot.selectedPlayerUuid(), tag.tag()));
                    }
                });
                if (tagDeleteMode) {
                    editorPanel.addView(tagDeleteRow(context, tagButton, categoryId, tag.tag()), fullWidthWrap());
                } else {
                    editorPanel.addView(tagButton, fullWidthWrap());
                }
            }

            Button clear = button(context, "清除当前分类预设 Tags");
            clear.setTextColor(0xFFFF6B6B);
            clear.setOnClickListener(v -> {
                if (snapshot.selectedPlayerUuid() != null) {
                    send(new EditorPayloads.ClearPresetTagsC2S(snapshot.selectedPlayerUuid(), categoryId));
                }
            });
            editorPanel.addView(clear, fullWidthWrap());
        }

        private LinearLayout tagDeleteRow(Context context, Button tagButton, String categoryId, String tag) {
            LinearLayout row = new LinearLayout(context);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.addView(tagButton, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

            Button delete = button(context, "X");
            delete.setTextColor(0xFFFF6B6B);
            delete.setOnClickListener(v -> showDeleteConfirmDialog(categoryId, tag));
            row.addView(delete, new LinearLayout.LayoutParams(48, ViewGroup.LayoutParams.WRAP_CONTENT));
            return row;
        }

        private void rebuildTagActions(Context context, String categoryId) {
            View spacer = new View(context);
            editorActions.addView(spacer, new LinearLayout.LayoutParams(0, 1, 1.0f));

            Button add = button(context, "增加");
            add.setTextColor(0xFF69D28D);
            add.setOnClickListener(v -> showAddTagDialog(categoryId));
            editorActions.addView(add, new LinearLayout.LayoutParams(92, ViewGroup.LayoutParams.WRAP_CONTENT));

            Button remove = button(context, tagDeleteMode ? "完成" : "删除");
            remove.setTextColor(tagDeleteMode ? 0xFF8FD3FF : 0xFFFF6B6B);
            remove.setOnClickListener(v -> {
                tagDeleteMode = !tagDeleteMode;
                rebuildEditor();
            });
            editorActions.addView(remove, new LinearLayout.LayoutParams(92, ViewGroup.LayoutParams.WRAP_CONTENT));
        }

        private String displayNameFor(String tag) {
            String configured = snapshot.tagDisplayNames().get(tag);
            if (configured != null && !configured.isBlank()) {
                return configured;
            }
            return tag;
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
            dialog.addView(hint, fullWidthWrap());

            EditText nameInput = new EditText(context);
            nameInput.setHint("名称");
            nameInput.setSingleLine(true);
            nameInput.setTextSize(14);
            nameInput.setPadding(8, 8, 8, 8);
            dialog.addView(nameInput, fullWidthWrap());

            EditText tagInput = new EditText(context);
            tagInput.setHint("实际需要赋予的 tag");
            tagInput.setSingleLine(true);
            tagInput.setTextSize(14);
            tagInput.setPadding(8, 8, 8, 8);
            dialog.addView(tagInput, fullWidthWrap());

            LinearLayout actions = new LinearLayout(context);
            actions.setOrientation(LinearLayout.HORIZONTAL);
            actions.setGravity(Gravity.RIGHT);

            Button cancel = button(context, "取消");
            cancel.setOnClickListener(v -> closeDialog());
            actions.addView(cancel, new LinearLayout.LayoutParams(92, ViewGroup.LayoutParams.WRAP_CONTENT));

            Button confirm = button(context, "确认");
            confirm.setTextColor(0xFF69D28D);
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
            dialog.addView(actions, fullWidthWrap());

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
            message.setTextColor(0xFFFFD166);
            dialog.addView(message, fullWidthWrap());

            LinearLayout actions = new LinearLayout(context);
            actions.setOrientation(LinearLayout.HORIZONTAL);
            actions.setGravity(Gravity.RIGHT);

            Button cancel = button(context, "取消");
            cancel.setOnClickListener(v -> closeDialog());
            actions.addView(cancel, new LinearLayout.LayoutParams(92, ViewGroup.LayoutParams.WRAP_CONTENT));

            Button confirm = button(context, "确认");
            confirm.setTextColor(0xFFFF6B6B);
            confirm.setOnClickListener(v -> {
                closeDialog();
                send(new EditorPayloads.RemovePresetTagC2S(selected, categoryId, tag));
            });
            actions.addView(confirm, new LinearLayout.LayoutParams(92, ViewGroup.LayoutParams.WRAP_CONTENT));
            dialog.addView(actions, fullWidthWrap());

            showDialog(dialog);
        }

        private void rebuildScoreEditor(Context context) {
            editorPanel.addView(section(context, "monvhua 计分板"));
            if (!snapshot.scoreboardObjectiveExists()) {
                TextView warning = body(context, "objective monvhua 不存在，请先在服务端创建。");
                warning.setTextColor(0xFFFF6B6B);
                editorPanel.addView(warning, fullWidthWrap());
                return;
            }
            for (EditorSnapshot.ScoreLevelEntry level : snapshot.scoreLevels()) {
                boolean current = level.value() == snapshot.selectedScore();
                Button scoreButton = button(context, (current ? "● " : "") + level.value() + " - " + level.displayName());
                scoreButton.setTextColor(current ? 0xFF69D28D : 0xFFECEFF4);
                scoreButton.setOnClickListener(v -> {
                    if (snapshot.selectedPlayerUuid() != null) {
                        send(new EditorPayloads.SetMonvhuaScoreC2S(snapshot.selectedPlayerUuid(), level.value()));
                    }
                });
                editorPanel.addView(scoreButton, fullWidthWrap());
            }
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

        private View horizontalResizeHandle(Context context, boolean resizePlayersColumn) {
            HorizontalResizeView handle = new HorizontalResizeView(context);
            handle.setBackground(new ColorDrawable(0xFF8FD3FF));
            handle.setOnTouchListener((view, event) -> {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN -> {
                        dragStartRawX = event.getRawX();
                        dragStartPlayersWidth = playersColumnWidth;
                        dragStartEditorWidth = editorColumnWidth;
                        return true;
                    }
                    case MotionEvent.ACTION_MOVE -> {
                        int delta = Math.round(event.getRawX() - dragStartRawX);
                        int minWidth = minColumnWidth();
                        if (resizePlayersColumn) {
                            playersColumnWidth = clamp(dragStartPlayersWidth + delta, minWidth, maxPlayersColumnWidth(minWidth));
                        } else {
                            editorColumnWidth = clamp(dragStartEditorWidth - delta, minWidth, maxEditorColumnWidth(minWidth));
                        }
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
            return handle;
        }

        private int minColumnWidth() {
            return Math.max(1, availableColumnWidth() / 10);
        }

        private int maxPlayersColumnWidth(int minWidth) {
            return Math.max(minWidth, availableColumnWidth() - editorColumnWidth - minWidth);
        }

        private int maxEditorColumnWidth(int minWidth) {
            return Math.max(minWidth, availableColumnWidth() - playersColumnWidth - minWidth);
        }

        private int availableColumnWidth() {
            if (contentRoot == null) {
                return playersColumnWidth + editorColumnWidth + 200;
            }
            int handleWidth = 20;
            int width = contentRoot.getWidth() - contentRoot.getPaddingLeft() - contentRoot.getPaddingRight() - handleWidth;
            return Math.max(10, width);
        }

        private void applyColumnWidths() {
            if (playersColumn == null || editorColumn == null) {
                return;
            }
            playersColumn.setLayoutParams(new LinearLayout.LayoutParams(playersColumnWidth, ViewGroup.LayoutParams.MATCH_PARENT));
            editorColumn.setLayoutParams(new LinearLayout.LayoutParams(editorColumnWidth, ViewGroup.LayoutParams.MATCH_PARENT));
            contentRoot.requestLayout();
        }

        private int clamp(int value, int min, int max) {
            return Math.max(min, Math.min(max, value));
        }

        private LinearLayout dialogPanel(Context context, String title) {
            LinearLayout dialog = panel(context);
            dialog.addView(title(context, title));
            return dialog;
        }

        private void showDialog(View dialog) {
            closeDialog();
            Context context = requireContext();

            FrameLayout overlay = new FrameLayout(context);
            overlay.setBackground(new ColorDrawable(0x99000000));
            overlay.setOnClickListener(v -> closeDialog());

            dialog.setOnClickListener(v -> {
            });
            FrameLayout.LayoutParams dialogParams = new FrameLayout.LayoutParams(330, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER);
            overlay.addView(dialog, dialogParams);
            root.addView(overlay, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            dialogOverlay = overlay;
        }

        private void closeDialog() {
            if (dialogOverlay != null && root != null) {
                root.removeView(dialogOverlay);
            }
            dialogOverlay = null;
        }

        private LinearLayout panel(Context context) {
            LinearLayout panel = new LinearLayout(context);
            panel.setOrientation(LinearLayout.VERTICAL);
            panel.setPadding(10, 10, 10, 10);
            panel.setBackground(new ColorDrawable(0xAA1C222B));
            return panel;
        }

        private View resizeBoundary(Context context) {
            ResizeBoundaryView view = new ResizeBoundaryView(context);
            view.setBackground(new ColorDrawable(0xFF8FD3FF));
            return view;
        }

        private FrameLayout framedPanel(Context context, View content) {
            FrameLayout frame = new FrameLayout(context);
            frame.setPadding(2, 2, 2, 2);
            frame.setBackground(new ColorDrawable(0xFF536278));
            frame.addView(content, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            return frame;
        }

        private TextView title(Context context, String text) {
            TextView view = new TextView(context);
            view.setText(text);
            view.setTextSize(22);
            view.setTextColor(0xFFFFFFFF);
            view.setPadding(0, 0, 0, 8);
            return view;
        }

        private TextView section(Context context, String text) {
            TextView view = new TextView(context);
            view.setText(text);
            view.setTextSize(16);
            view.setTextColor(0xFF8FD3FF);
            view.setPadding(0, 12, 0, 6);
            return view;
        }

        private TextView body(Context context, String text) {
            TextView view = new TextView(context);
            view.setText(text);
            view.setTextSize(14);
            view.setTextColor(0xFFECEFF4);
            view.setPadding(0, 4, 0, 4);
            return view;
        }

        private Button button(Context context, String text) {
            Button button = new Button(context);
            button.setText(text);
            button.setTextSize(14);
            button.setGravity(Gravity.CENTER);
            button.setPadding(8, 8, 8, 8);
            return button;
        }

        private LinearLayout.LayoutParams fullWidthWrap() {
            return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        private LinearLayout.LayoutParams equalWidthWrap() {
            return new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        }

        private LinearLayout.LayoutParams fullWidthFixed(int height) {
            return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height);
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

    private static final class ResizeBoundaryView extends View {
        private ResizeBoundaryView(Context context) {
            super(context);
        }

        @Override
        public PointerIcon onResolvePointerIcon(MotionEvent event) {
            return VERTICAL_RESIZE_CURSOR;
        }
    }

    private static final class HorizontalResizeView extends View {
        private HorizontalResizeView(Context context) {
            super(context);
        }

        @Override
        public PointerIcon onResolvePointerIcon(MotionEvent event) {
            return HORIZONTAL_RESIZE_CURSOR;
        }
    }
}
