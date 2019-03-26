package de.codingair.warpsystem.transfer.packets.bungee;

import de.codingair.warpsystem.transfer.packets.utils.Packet;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class TeleportPlayerToCoordsPacket implements Packet {
    private String gate, player;
    private double x, y, z;
    private boolean relativeX, relativeY, relativeZ;

    public TeleportPlayerToCoordsPacket() {
    }

    public TeleportPlayerToCoordsPacket(String gate, String player, double x, double y, double z, boolean relativeX, boolean relativeY, boolean relativeZ) {
        this.gate = gate;
        this.player = player;
        this.x = x;
        this.y = y;
        this.z = z;
        this.relativeX = relativeX;
        this.relativeY = relativeY;
        this.relativeZ = relativeZ;
    }

    @Override
    public void write(DataOutputStream out) throws IOException {
        out.writeUTF(this.gate);
        out.writeUTF(this.player);
        out.writeDouble(this.x);
        out.writeDouble(this.y);
        out.writeDouble(this.z);
        out.writeBoolean(this.relativeX);
        out.writeBoolean(this.relativeY);
        out.writeBoolean(this.relativeZ);
    }

    @Override
    public void read(DataInputStream in) throws IOException {
        this.gate = in.readUTF();
        this.player = in.readUTF();
        this.x = in.readDouble();
        this.y = in.readDouble();
        this.z = in.readDouble();
        this.relativeX = in.readBoolean();
        this.relativeY = in.readBoolean();
        this.relativeZ = in.readBoolean();
    }

    public String getGate() {
        return gate;
    }

    public String getPlayer() {
        return player;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public boolean isRelativeX() {
        return relativeX;
    }

    public boolean isRelativeY() {
        return relativeY;
    }

    public boolean isRelativeZ() {
        return relativeZ;
    }
}
