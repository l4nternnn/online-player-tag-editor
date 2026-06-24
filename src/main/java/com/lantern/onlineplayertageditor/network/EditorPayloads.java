package com.lantern.onlineplayertageditor.network;

import com.lantern.onlineplayertageditor.OnlinePlayerTagEditor;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.UUID;

public final class EditorPayloads {
    private EditorPayloads() {
    }

    private static Identifier id(String path) {
        return Identifier.of(OnlinePlayerTagEditor.MOD_ID, path);
    }

    public record OpenEditorS2C(String json) implements CustomPayload {
        public static final Id<OpenEditorS2C> ID = new Id<>(id("open_editor"));
        public static final PacketCodec<RegistryByteBuf, OpenEditorS2C> CODEC =
                PacketCodecs.STRING.xmap(OpenEditorS2C::new, OpenEditorS2C::json).cast();

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record EditorNoticeS2C(String message, boolean error) implements CustomPayload {
        public static final Id<EditorNoticeS2C> ID = new Id<>(id("editor_notice"));
        public static final PacketCodec<RegistryByteBuf, EditorNoticeS2C> CODEC = PacketCodec.ofStatic(
                (buf, payload) -> {
                    buf.writeString(payload.message);
                    buf.writeBoolean(payload.error);
                },
                buf -> new EditorNoticeS2C(buf.readString(), buf.readBoolean())
        );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record RefreshEditorC2S(UUID selectedPlayerUuid) implements CustomPayload {
        public static final Id<RefreshEditorC2S> ID = new Id<>(id("refresh_editor"));
        public static final PacketCodec<RegistryByteBuf, RefreshEditorC2S> CODEC = PacketCodec.ofStatic(
                (buf, payload) -> buf.writeUuid(payload.selectedPlayerUuid),
                buf -> new RefreshEditorC2S(buf.readUuid())
        );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record ToggleTagC2S(UUID targetPlayerUuid, String tag) implements CustomPayload {
        public static final Id<ToggleTagC2S> ID = new Id<>(id("toggle_tag"));
        public static final PacketCodec<RegistryByteBuf, ToggleTagC2S> CODEC = uuidStringCodec(
                ToggleTagC2S::new,
                ToggleTagC2S::targetPlayerUuid,
                ToggleTagC2S::tag
        );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record ClearPresetTagsC2S(UUID targetPlayerUuid, String categoryId) implements CustomPayload {
        public static final Id<ClearPresetTagsC2S> ID = new Id<>(id("clear_preset_tags"));
        public static final PacketCodec<RegistryByteBuf, ClearPresetTagsC2S> CODEC = uuidStringCodec(
                ClearPresetTagsC2S::new,
                ClearPresetTagsC2S::targetPlayerUuid,
                ClearPresetTagsC2S::categoryId
        );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record SetMonvhuaScoreC2S(UUID targetPlayerUuid, int value) implements CustomPayload {
        public static final Id<SetMonvhuaScoreC2S> ID = new Id<>(id("set_monvhua_score"));
        public static final PacketCodec<RegistryByteBuf, SetMonvhuaScoreC2S> CODEC = PacketCodec.ofStatic(
                (buf, payload) -> {
                    buf.writeUuid(payload.targetPlayerUuid);
                    buf.writeVarInt(payload.value);
                },
                buf -> new SetMonvhuaScoreC2S(buf.readUuid(), buf.readVarInt())
        );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    private static <T> PacketCodec<RegistryByteBuf, T> uuidStringCodec(
            UuidStringFactory<T> factory,
            java.util.function.Function<T, UUID> uuidGetter,
            java.util.function.Function<T, String> stringGetter
    ) {
        return PacketCodec.ofStatic(
                (buf, payload) -> {
                    buf.writeUuid(uuidGetter.apply(payload));
                    buf.writeString(stringGetter.apply(payload));
                },
                buf -> factory.create(buf.readUuid(), buf.readString())
        );
    }

    @FunctionalInterface
    private interface UuidStringFactory<T> {
        T create(UUID uuid, String value);
    }
}
