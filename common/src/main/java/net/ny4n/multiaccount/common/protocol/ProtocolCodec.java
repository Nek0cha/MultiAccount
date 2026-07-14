package net.ny4n.multiaccount.common.protocol;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * multiaccount:main チャンネルで送受信するペイロードの符号化。
 * Velocity / Paper 双方がバンドルする Guava の ByteArrayDataOutput/Input を用いる。
 */
public final class ProtocolCodec {

    private ProtocolCodec() {
    }

    public static byte[] encodeRequest(CommandRequest request) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF(MessageType.COMMAND_REQUEST.name());
        writeUuid(out, request.correlationId());
        out.writeUTF(request.subcommand());
        out.writeInt(request.args().size());
        for (String arg : request.args()) {
            out.writeUTF(arg);
        }
        return out.toByteArray();
    }

    public static byte[] encodeResponse(CommandResponse response) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF(MessageType.COMMAND_RESPONSE.name());
        writeUuid(out, response.correlationId());
        out.writeBoolean(response.success());
        out.writeInt(response.lines().size());
        for (String line : response.lines()) {
            out.writeUTF(line);
        }
        return out.toByteArray();
    }

    public static MessageType peekType(byte[] data) {
        ByteArrayDataInput in = ByteStreams.newDataInput(data);
        return MessageType.valueOf(in.readUTF());
    }

    public static CommandRequest decodeRequest(byte[] data) {
        ByteArrayDataInput in = ByteStreams.newDataInput(data);
        MessageType type = MessageType.valueOf(in.readUTF());
        if (type != MessageType.COMMAND_REQUEST) {
            throw new IllegalArgumentException("Not a COMMAND_REQUEST payload: " + type);
        }
        UUID correlationId = readUuid(in);
        String subcommand = in.readUTF();
        int argCount = in.readInt();
        List<String> args = new ArrayList<>(argCount);
        for (int i = 0; i < argCount; i++) {
            args.add(in.readUTF());
        }
        return new CommandRequest(correlationId, subcommand, args);
    }

    public static CommandResponse decodeResponse(byte[] data) {
        ByteArrayDataInput in = ByteStreams.newDataInput(data);
        MessageType type = MessageType.valueOf(in.readUTF());
        if (type != MessageType.COMMAND_RESPONSE) {
            throw new IllegalArgumentException("Not a COMMAND_RESPONSE payload: " + type);
        }
        UUID correlationId = readUuid(in);
        boolean success = in.readBoolean();
        int lineCount = in.readInt();
        List<String> lines = new ArrayList<>(lineCount);
        for (int i = 0; i < lineCount; i++) {
            lines.add(in.readUTF());
        }
        return new CommandResponse(correlationId, success, lines);
    }

    private static void writeUuid(ByteArrayDataOutput out, UUID uuid) {
        out.writeLong(uuid.getMostSignificantBits());
        out.writeLong(uuid.getLeastSignificantBits());
    }

    private static UUID readUuid(ByteArrayDataInput in) {
        long most = in.readLong();
        long least = in.readLong();
        return new UUID(most, least);
    }
}
