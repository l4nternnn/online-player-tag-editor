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
import icyllis.modernui.view.View;
import icyllis.modernui.view.ViewGroup;
import icyllis.modernui.widget.Button;
import icyllis.modernui.widget.EditText;
import icyllis.modernui.widget.LinearLayout;
import icyllis.modernui.widget.ScrollView;
import icyllis.modernui.widget.TextView;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Locale;
import java.util.UUID;

public final class ModernTagEditorScreen {
    private static EditorFragment currentFragment;
    private static String activeTab = TagCategory.CHARACTER.id();
    private static String searchText = "";

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
        private LinearLayout root;
        private LinearLayout playerList;
        private LinearLayout detailPanel;
        private LinearLayout editorPanel;
        private TextView noticeView;
        private boolean noticeIsError;
        private String noticeMessage = "";

        private EditorFragment(EditorSnapshot snapshot) {
            this.snapshot = snapshot;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 icyllis.modernui.util.DataSet savedInstanceState) {
            Context context = requireContext();

            root = new LinearLayout(context);
            root.setOrientation(LinearLayout.HORIZONTAL);
            root.setPadding(14, 14, 14, 14);
            root.setBackground(new ColorDrawable(0xCC101318));
            root.setLayoutParams(match());

            root.addView(buildPlayersColumn(context), new LinearLayout.LayoutParams(250, ViewGroup.LayoutParams.MATCH_PARENT));
            root.addView(buildDetailColumn(context), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1.0f));
            root.addView(buildEditorColumn(context), new LinearLayout.LayoutParams(430, ViewGroup.LayoutParams.MATCH_PARENT));

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
            playerList = null;
            detailPanel = null;
            editorPanel = null;
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
            return column;
        }

        private View buildDetailColumn(Context context) {
            detailPanel = panel(context);
            return detailPanel;
        }

        private View buildEditorColumn(Context context) {
            LinearLayout column = panel(context);
            noticeView = body(context, noticeMessage);
            noticeView.setTextColor(noticeIsError ? 0xFFFF6B6B : 0xFF69D28D);
            column.addView(noticeView, fullWidthWrap());

            LinearLayout tabs = new LinearLayout(context);
            tabs.setOrientation(LinearLayout.HORIZONTAL);
            tabs.addView(tabButton(context, TagCategory.CHARACTER.id(), "角色"));
            tabs.addView(tabButton(context, TagCategory.MAGIC.id(), "魔法"));
            tabs.addView(tabButton(context, TagCategory.IDENTITY.id(), "身份"));
            tabs.addView(tabButton(context, "monvhua", "monvhua"));
            column.addView(tabs, fullWidthWrap());

            ScrollView scroll = new ScrollView(context);
            editorPanel = new LinearLayout(context);
            editorPanel.setOrientation(LinearLayout.VERTICAL);
            scroll.addView(editorPanel, scrollChild());
            column.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f));
            return column;
        }

        private View tabButton(Context context, String tab, String label) {
            Button button = button(context, activeTab.equals(tab) ? "● " + label : label);
            button.setOnClickListener(v -> {
                activeTab = tab;
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
            Context context = requireContext();
            if ("monvhua".equals(activeTab)) {
                rebuildScoreEditor(context);
                return;
            }
            rebuildTagEditor(context, activeTab);
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
                editorPanel.addView(tagButton, fullWidthWrap());
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

        private LinearLayout panel(Context context) {
            LinearLayout panel = new LinearLayout(context);
            panel.setOrientation(LinearLayout.VERTICAL);
            panel.setPadding(10, 10, 10, 10);
            panel.setBackground(new ColorDrawable(0xAA1C222B));
            return panel;
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
}
