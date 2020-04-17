package de.codingair.warpsystem.bungee.base.listeners;

import de.codingair.codingapi.tools.Callback;
import de.codingair.codingapi.utils.Value;
import de.codingair.warpsystem.bungee.api.ServerSwitchAttemptEvent;
import de.codingair.warpsystem.bungee.base.WarpSystem;
import de.codingair.warpsystem.transfer.packets.bungee.PrepareLoginMessagePacket;
import de.codingair.warpsystem.transfer.packets.general.BooleanPacket;
import de.codingair.warpsystem.transfer.packets.general.IntegerPacket;
import de.codingair.warpsystem.transfer.packets.general.PrepareCoordinationTeleportPacket;
import de.codingair.warpsystem.transfer.packets.spigot.*;
import de.codingair.warpsystem.transfer.packets.utils.Packet;
import de.codingair.warpsystem.transfer.packets.utils.PacketType;
import de.codingair.warpsystem.transfer.utils.PacketListener;
import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ServerConnectedEvent;
import net.md_5.bungee.api.event.ServerDisconnectEvent;
import net.md_5.bungee.api.event.ServerSwitchEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.util.concurrent.TimeUnit;

public class MainListener implements Listener, PacketListener {

    @EventHandler
    public void onConnect(ServerConnectedEvent e) {
        if(e.getServer().getInfo().getPlayers().size() == 0) {
            //Update it
            WarpSystem.getInstance().getServerManager().sendInitialPacket(e.getServer().getInfo());
        }
    }

    @EventHandler
    public void onSwitch(ServerSwitchEvent e) {
        WarpSystem.getInstance().getDataManager().getOped().remove(e.getPlayer().getName());
    }

    @EventHandler
    public void onSwitch(ServerDisconnectEvent e) {
        WarpSystem.getInstance().getDataManager().getOped().remove(e.getPlayer().getName());
    }

    @Override
    public void onReceive(Packet packet, String extra) {
        ServerInfo server = BungeeCord.getInstance().getServerInfo(extra);

        switch(PacketType.getByObject(packet)) {
            case RequestInitialPacket: {
                WarpSystem.getInstance().getServerManager().sendInitialPacket(server);
                break;
            }

            case IsOnlinePacket: {
                IsOnlinePacket p = (IsOnlinePacket) packet;
                BooleanPacket answer = new BooleanPacket(BungeeCord.getInstance().getPlayer(p.getName()) != null);
                p.applyAsAnswer(answer);
                WarpSystem.getInstance().getDataHandler().send(answer, server);
                break;
            }

            case IsOperatorPacket: {
                IsOperatorPacket p = (IsOperatorPacket) packet;

                if(p.isOperator()) {
                    WarpSystem.getInstance().getDataManager().getOped().add(p.getPlayer());
                } else {
                    WarpSystem.getInstance().getDataManager().getOped().remove(p.getPlayer());
                }

                break;
            }

            case MessagePacket: {
                MessagePacket p = (MessagePacket) packet;
                ProxiedPlayer player = BungeeCord.getInstance().getPlayer(p.getPlayer());

                if(player != null) player.sendMessage(new TextComponent(p.getMessage()));
                break;
            }

            case RequestServerStatusPacket: {
                RequestServerStatusPacket p = (RequestServerStatusPacket) packet;
                BooleanPacket answer = new BooleanPacket();
                p.applyAsAnswer(answer);

                ServerInfo info = BungeeCord.getInstance().getServerInfo(p.getServer());

                if(info == null) {
                    answer.setValue(false);
                    WarpSystem.getInstance().getDataHandler().send(answer, server);
                } else {
                    info.ping((serverPing, throwable) -> {
                        WarpSystem.getInstance().getServerManager().setStatus(info, throwable == null);
                        answer.setValue(throwable == null);
                        WarpSystem.getInstance().getDataHandler().send(answer, server);
                    });
                }
                break;
            }

            case PrepareServerSwitchPacket: {
                PrepareServerSwitchPacket p = (PrepareServerSwitchPacket) packet;
                IntegerPacket answer = new IntegerPacket();
                p.applyAsAnswer(answer);

                ProxiedPlayer pp = BungeeCord.getInstance().getPlayer(p.getPlayer());
                ServerInfo info = BungeeCord.getInstance().getServerInfo(p.getServer());

                if(pp == null || info == null) {
                    answer.setValue(1);
                    WarpSystem.getInstance().getDataHandler().send(answer, server);
                } else {
                    if(pp.getServer().getInfo() == info) {
                        answer.setValue(2);
                        WarpSystem.getInstance().getDataHandler().send(answer, server);
                        return;
                    }

                    Value<Boolean> modifiedEvent = new Value<>(false);
                    ServerSwitchAttemptEvent e = new ServerSwitchAttemptEvent(pp, info, new Callback() {
                        private boolean used = false;

                        @Override
                        public void accept(Object object) {
                            if(used) return;
                            used = true;

                            Callback<Boolean> pingCallback = new Callback<Boolean>() {
                                @Override
                                public void accept(Boolean online) {
                                    if(online) {
                                        pp.connect(info, (connected, throwable) -> {
                                            if(connected) {
                                                if(p.getMessage() != null) {
                                                    BungeeCord.getInstance().getScheduler().schedule(WarpSystem.getInstance(), () -> WarpSystem.getInstance().getDataHandler().send(new PrepareLoginMessagePacket(pp.getName(), p.getMessage()), info), 500, TimeUnit.MILLISECONDS);
                                                }
                                            }

                                            answer.setValue(connected ? 0 : 4);
                                            WarpSystem.getInstance().getDataHandler().send(answer, server);
                                        });
                                    } else {
                                        answer.setValue(3);
                                        WarpSystem.getInstance().getDataHandler().send(answer, server);
                                    }
                                }
                            };

                            if(modifiedEvent.getValue()) {
                                WarpSystem.getInstance().getServerManager().ping(info, pingCallback);
                            } else pingCallback.accept(WarpSystem.getInstance().getServerManager().isOnline(info));
                        }
                    });

                    BungeeCord.getInstance().getPluginManager().callEvent(e);
                    if(!e.isWaitForCallback()) e.getTeleportFinisher().accept(null);
                    else modifiedEvent.setValue(true);
                }
                break;
            }

            case PrepareCoordinationTeleportPacket: {
                PrepareCoordinationTeleportPacket p = (PrepareCoordinationTeleportPacket) packet;
                IntegerPacket answer = new IntegerPacket();
                p.applyAsAnswer(answer);

                ProxiedPlayer pp = BungeeCord.getInstance().getPlayer(p.getPlayer());
                ServerInfo target = BungeeCord.getInstance().getServerInfo(p.getServer());

                if(WarpSystem.getInstance().getServerManager().isOnline(target)) {
                    if(target.getPlayers().isEmpty()) {
                        //switch and teleport
                        pp.connect(target, (connected, throwable) -> {
                            if(connected) {
                                answer.setValue(0);
                                PrepareCoordinationTeleportPacket finalCall = p.clone(null);
                                finalCall.setServer(null);
                                WarpSystem.getInstance().getDataHandler().send(finalCall, target);
                            } else {
                                answer.setValue(1);
                            }

                            WarpSystem.getInstance().getDataHandler().send(answer, server);
                        });
                    } else {
                        //prepare and switch
                        PrepareCoordinationTeleportPacket finalCall = p.clone(new Callback<Integer>() {
                            @Override
                            public void accept(Integer object) {
                                answer.setValue(object);
                                WarpSystem.getInstance().getDataHandler().send(answer, server);
                            }
                        });
                        finalCall.setServer(null);
                        WarpSystem.getInstance().getDataHandler().send(finalCall, target);
                        pp.connect(target);
                    }
                } else {
                    answer.setValue(1);
                    WarpSystem.getInstance().getDataHandler().send(answer, server);
                }
                break;
            }
        }
    }

    @Override
    public boolean onSend(Packet packet) {
        return false;
    }
}
