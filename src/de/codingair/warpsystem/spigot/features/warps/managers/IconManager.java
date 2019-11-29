package de.codingair.warpsystem.spigot.features.warps.managers;

import de.codingair.codingapi.files.ConfigFile;
import de.codingair.codingapi.serializable.SerializableLocation;
import de.codingair.codingapi.server.Color;
import de.codingair.codingapi.tools.Location;
import de.codingair.codingapi.tools.items.ItemBuilder;
import de.codingair.codingapi.tools.items.XMaterial;
import de.codingair.warpsystem.spigot.base.WarpSystem;
import de.codingair.warpsystem.spigot.base.language.Lang;
import de.codingair.warpsystem.spigot.base.utils.teleport.destinations.Destination;
import de.codingair.warpsystem.spigot.base.utils.teleport.destinations.DestinationType;
import de.codingair.warpsystem.spigot.features.FeatureType;
import de.codingair.warpsystem.spigot.features.globalwarps.guis.affiliations.GlobalWarp;
import de.codingair.warpsystem.spigot.features.simplewarps.commands.CWarp;
import de.codingair.warpsystem.spigot.features.warps.commands.CWarps;
import de.codingair.warpsystem.spigot.features.warps.guis.affiliations.Category;
import de.codingair.warpsystem.spigot.features.warps.guis.affiliations.DecoIcon;
import de.codingair.warpsystem.spigot.features.warps.guis.affiliations.Warp;
import de.codingair.warpsystem.spigot.features.warps.guis.affiliations.utils.Action;
import de.codingair.warpsystem.spigot.features.warps.guis.affiliations.utils.ActionIconHelper;
import de.codingair.warpsystem.spigot.features.warps.guis.affiliations.utils.ActionObject;
import de.codingair.warpsystem.spigot.features.warps.importfilter.PageData;
import de.codingair.warpsystem.spigot.features.warps.importfilter.WarpData;
import de.codingair.warpsystem.spigot.features.warps.nextlevel.exceptions.IconReadException;
import de.codingair.warpsystem.spigot.features.warps.nextlevel.utils.Icon;
import de.codingair.warpsystem.spigot.base.utils.featureobjects.actions.types.BoundAction;
import de.codingair.warpsystem.spigot.base.utils.featureobjects.actions.types.CommandAction;
import de.codingair.warpsystem.spigot.base.utils.featureobjects.actions.types.CostsAction;
import de.codingair.warpsystem.spigot.base.utils.featureobjects.actions.types.WarpAction;
import de.codingair.warpsystem.spigot.features.simplewarps.SimpleWarp;
import de.codingair.warpsystem.spigot.features.simplewarps.managers.SimpleWarpManager;
import de.codingair.warpsystem.utils.Manager;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import de.codingair.codingapi.tools.JSON.JSONObject;
import de.codingair.codingapi.tools.JSON.JSONParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class IconManager implements Manager {
    private static ItemBuilder STANDARD_ITEM() {
        return new ItemBuilder(Material.GRASS);
    }

    private List<Icon> icons = new ArrayList<>();

    private ItemStack background = null;

    public static IconManager getInstance() {
        return ((IconManager) WarpSystem.getInstance().getDataManager().getManager(FeatureType.WARPS));
    }

    public boolean load() {
        if(WarpSystem.getInstance().getFileManager().getFile("ActionIcons") == null) WarpSystem.getInstance().getFileManager().loadFile("ActionIcons", "/Memory/");

        //Load
        boolean success = true;

        WarpSystem.log("  > Loading Icons");
        ActionIconHelper.load = true;

        ConfigFile file = WarpSystem.getInstance().getFileManager().getFile("ActionIcons");
        FileConfiguration config = file.getConfig();

        WarpSystem.log("    > Loading background");
        String data = config.getString("Background_Item", null);
        this.background = data == null ? null : ItemBuilder.getFromJSON(data).getItem();
        if(this.background == null) {
            WarpSystem.log("      ...no background available > create standard");
            this.background = new ItemBuilder(XMaterial.BLACK_STAINED_GLASS_PANE).setHideName(true).getItem();
        } else WarpSystem.log("      ...got 1 background");

        WarpSystem.log("    > Loading Icons");
        icons.clear();
        List<String> iconList = config.getStringList("Icons");
        for(String s : iconList) {
            try {
                JSONObject json = (JSONObject) new JSONParser().parse(s);
                Icon icon = new Icon();
                try {
                    icon.read(json);
                    icons.add(icon);
                } catch(IconReadException e) {
                    e.printStackTrace();
                    success = false;
                }
            } catch(Exception e) {
                e.printStackTrace();
            }
        }

//        WarpSystem.log("    > Loading Categories");
        List<Category> categories = new ArrayList<>();
        List<String> categoryList = config.getStringList("Categories");
        for(String s : categoryList) {
            Category category = ActionIconHelper.fromString(s);

            if(category != null) {
                if(category.getName().contains("@")) category.setName(category.getName().replace("@", "(at)"));
                categories.add(category);
            } else success = false;
        }

//        WarpSystem.log("      ...got " + categoryList.size() + " " + (categoryList.size() == 1 ? "Category" : "Categories"));

//        WarpSystem.log("    > Loading Warps");
        List<Warp> warps = new ArrayList<>();
        List<String> warpList = config.getStringList("Warps");
        for(String s : warpList) {
            Warp warp = ActionIconHelper.fromString(s);

            if(warp != null) {
                if(warp.getName().contains("@")) warp.setName(warp.getName().replace("@", "(at)"));
                warps.add(warp);
            } else success = false;
        }

//        WarpSystem.log("      ...got " + warpList.size() + " Warp(s)");

//        WarpSystem.log("      > Check each Category of all Warps");
        for(Warp warp : warps) {
            if(warp.getCategory() == null) continue;
            if(!existsPage(warp.getCategory().getName())) {
                categories.add(warp.getCategory());
            }
        }

//        WarpSystem.log("    > Loading GlobalWarps");
        List<GlobalWarp> globalWarps = new ArrayList<>();
        List<String> gWarps = config.getStringList("GlobalWarps");
        for(String s : gWarps) {
            GlobalWarp warp = ActionIconHelper.fromString(s);

            if(warp != null) {
                if(warp.getName().contains("@")) warp.setName(warp.getName().replace("@", "(at)"));
                globalWarps.add(warp);
            } else success = false;
        }

//        WarpSystem.log("      ...got " + globalWarps.size() + " GlobalWarp(s)");

//        WarpSystem.log("    > Loading Deco");
        List<DecoIcon> decoIcons = new ArrayList<>();
        List<String> decoIconList = config.getStringList("DecoIcons");
        for(String s : decoIconList) {
            DecoIcon deco = ActionIconHelper.fromString(s);

            if(deco != null) decoIcons.add(deco);
            else success = false;
        }
//        WarpSystem.log("      ...got " + decoIconList.size() + " DecoIcon(s)");

        for(Warp icon : warps)
            if(icon.getName() != null && icon.getName().contains("_")) {
                icon.setName(icon.getName().replace("_", " "));
            }
        for(Category icon : categories)
            if(icon.getName() != null && icon.getName().contains("_")) {
                icon.setName(icon.getName().replace("_", " "));
            }
        for(GlobalWarp icon : globalWarps)
            if(icon.getName() != null && icon.getName().contains("_")) {
                icon.setName(icon.getName().replace("_", " "));
            }
        for(DecoIcon icon : decoIcons)
            if(icon.getName() != null && icon.getName().contains("_")) {
                icon.setName(icon.getName().replace("_", " "));
            }

        //translate
        for(Category c : categories) {
            ActionObject command = c.getAction(Action.RUN_COMMAND);
            String s = command == null ? null : command.getValue();

            ActionObject bound = c.getAction(Action.BOUND_TO_WORLD);
            String world = bound == null ? null : bound.getValue();

            List<de.codingair.warpsystem.spigot.base.utils.featureobjects.actions.ActionObject> actions = new ArrayList<>();
            if(s != null) actions.add(new CommandAction(s));
            if(world != null) actions.add(new BoundAction(world));

            Icon icon;
            this.icons.add(icon = new Icon(c.getName(), c.getItem(), null, c.getSlot(), c.getPermission(), actions));
            icon.setPage(true);
        }

        for(Warp c : warps) {
            ActionObject warpAction = c.getAction(Action.TELEPORT_TO_WARP);
            SerializableLocation loc = warpAction.getValue();
            boolean created = false;
            if(SimpleWarpManager.getInstance().getWarp(c.getIdentifier()) == null) {
                Location dest = (Location) loc.getLocation();
                SimpleWarpManager.getInstance().addWarp(
                        new SimpleWarp(new WarpData(c.getIdentifier().replace(" ", "_"), null, c.getPermission(), dest.getWorldName(), dest.getX(), dest.getY(), dest.getZ(), dest.getYaw(), dest.getPitch()))
                );
                created = true;
            }

            ActionObject command = c.getAction(Action.RUN_COMMAND);
            String s = command == null ? null : command.getValue();

            ActionObject costs = c.getAction(Action.PAY_MONEY);
            double amount = costs == null ? 0 : costs.getValue();

            ActionObject bound = c.getAction(Action.BOUND_TO_WORLD);
            String world = bound == null ? null : bound.getValue();

            List<de.codingair.warpsystem.spigot.base.utils.featureobjects.actions.ActionObject> actions = new ArrayList<>();
            if(created) actions.add(new WarpAction(new Destination(c.getIdentifier().replace(" ", "_"), DestinationType.SimpleWarp)));
            if(s != null) actions.add(new CommandAction(s));
            if(world != null) actions.add(new BoundAction(world));
            if(amount > 0) actions.add(new CostsAction(amount));

            this.icons.add(new Icon(c.getName(), c.getItem(), c.getCategory() == null ? null : getPage(c.getCategory().getName()), c.getSlot(), c.getPermission(), actions));
        }

        for(GlobalWarp c : globalWarps) {
            ActionObject switchServer = c.getAction(Action.SWITCH_SERVER);
            String gWarp = switchServer.getValue();

            ActionObject command = c.getAction(Action.RUN_COMMAND);
            String s = command == null ? null : command.getValue();

            ActionObject costs = c.getAction(Action.PAY_MONEY);
            double amount = costs == null ? 0 : costs.getValue();

            ActionObject bound = c.getAction(Action.BOUND_TO_WORLD);
            String world = bound == null ? null : bound.getValue();

            List<de.codingair.warpsystem.spigot.base.utils.featureobjects.actions.ActionObject> actions = new ArrayList<>();
            actions.add(new WarpAction(new Destination(gWarp, DestinationType.GlobalWarp)));
            if(s != null) actions.add(new CommandAction(s));
            if(world != null) actions.add(new BoundAction(world));
            if(amount > 0) actions.add(new CostsAction(amount));

            this.icons.add(new Icon(c.getName(), c.getItem(), c.getCategory() == null ? null : getPage(c.getCategory().getName()), c.getSlot(), c.getPermission(), actions));
        }

        for(DecoIcon c : decoIcons) {
            ActionObject command = c.getAction(Action.RUN_COMMAND);
            String s = command == null ? null : command.getValue();

            ActionObject costs = c.getAction(Action.PAY_MONEY);
            double amount = costs == null ? 0 : costs.getValue();

            ActionObject bound = c.getAction(Action.BOUND_TO_WORLD);
            String world = bound == null ? null : bound.getValue();

            List<de.codingair.warpsystem.spigot.base.utils.featureobjects.actions.ActionObject> actions = new ArrayList<>();
            if(s != null) actions.add(new CommandAction(s));
            if(world != null) actions.add(new BoundAction(world));
            if(amount > 0) actions.add(new CostsAction(amount));

            this.icons.add(new Icon(c.getName(), c.getItem(), c.getCategory() == null ? null : getPage(c.getCategory().getName()), c.getSlot(), c.getPermission(), actions));
        }

        ActionIconHelper.load = false;
        new CWarps().register(WarpSystem.getInstance());
        if(WarpSystem.getInstance().getFileManager().getFile("Config").getConfig().getBoolean("WarpSystem.Commands.Warp.GUI", false) && !FeatureType.SIMPLE_WARPS.isActive()) {
            new CWarp().register(WarpSystem.getInstance());
        }

        if(!success) {
            TextComponent base = new TextComponent(Lang.getPrefix() + "§cTry to use WarpSystem ");
            TextComponent link = new TextComponent("§c§nv3.0.1");
            TextComponent end = new TextComponent("§c to convert your icons!");

            link.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://www.spigotmc.org/resources/warps-portals-and-warpsigns-warp-system-only-gui.29595/history"));
            link.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new BaseComponent[] {new TextComponent("§8» Click «")}));

            base.addExtra(link);
            base.addExtra(end);

            Bukkit.getPluginManager().registerEvents(new Listener() {
                @EventHandler
                public void onJoin(PlayerJoinEvent e) {
                    if(e.getPlayer().hasPermission(WarpSystem.PERMISSION_ADMIN)) {
                        Bukkit.getScheduler().runTaskLater(WarpSystem.getInstance(), () -> {
                            e.getPlayer().sendMessage(" ");
                            e.getPlayer().sendMessage(Lang.getPrefix() + "§4Warning! §cCouldn't load all icons successfully.");
                            e.getPlayer().spigot().sendMessage(base);
                            e.getPlayer().sendMessage(" ");
                        }, 20);
                    }
                }
            }, WarpSystem.getInstance());

            for(Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                if(onlinePlayer.hasPermission(WarpSystem.PERMISSION_ADMIN)) {
                    Bukkit.getScheduler().runTaskLater(WarpSystem.getInstance(), () -> {
                        onlinePlayer.sendMessage(" ");
                        onlinePlayer.sendMessage(Lang.getPrefix() + "§4Warning! §cCouldn't load all icons successfully.");
                        onlinePlayer.spigot().sendMessage(base);
                        onlinePlayer.sendMessage(" ");
                    }, 20);
                }
            }
        }

        int icons = this.icons.size();
        clean(null);
        if(icons > this.icons.size()) {
            WarpSystem.log("      ...cleaned a total of " + (icons - this.icons.size()) + " icon(s)");
        }

        WarpSystem.log("      ...got " + this.icons.size() + " " + (this.icons.size() == 1 ? "Icon" : "Icons"));
        return success;
    }

    private void clean(Icon page) {
        if(page != null && !page.isPage()) throw new IllegalArgumentException("Given icon is not a category!");
        Icon[] iconList = new Icon[54];

        for(Icon icon : getIcons(page)) {
            Icon other = iconList[icon.getSlot()];

            if(other == null) iconList[icon.getSlot()] = icon;
            else if(other.isPage() && icon.isPage()) {
                List<Icon> l0 = getIcons(icon);
                List<Icon> l1 = getIcons(other);

                if(l0.size() >= l1.size()) {
                    remove(other);
                    iconList[icon.getSlot()] = icon;
                } else {
                    remove(icon);
                }

                l0.clear();
                l1.clear();
            } else if(other.isPage() && !icon.isPage()) {
                remove(icon);
            } else if(!other.isPage() && icon.isPage()) {
                remove(other);
                iconList[icon.getSlot()] = icon;
            } else {
                remove(icon);
            }
        }

        List<Icon> pages = getPages(page);

        for(Icon icon : pages) {
            clean(icon);
        }

        pages.clear();
    }

    public void save(boolean saver) {
        //Save
        if(!saver) WarpSystem.log("  > Saving Icons");

        ConfigFile file = WarpSystem.getInstance().getFileManager().getFile("ActionIcons");
        FileConfiguration config = file.getConfig();

        if(!saver) WarpSystem.log("    > Saving background");
        config.set("Background_Item", new ItemBuilder(this.background).toJSONString());
        if(!saver) WarpSystem.log("      ...saved 1 background");

        if(!saver) WarpSystem.log("    > Saving Icons");
        List<String> icons = new ArrayList<>();
        for(Icon icon : this.icons) {
            JSONObject json = new JSONObject();
            icon.write(json);
            icons.add(json.toJSONString());
        }
        config.set("Icons", icons);
        if(!saver) WarpSystem.log("      ...saved " + icons.size() + " Icon(s)");

        config.set("DecoIcons", null);
        config.set("GlobalWarps", null);
        config.set("Warps", null);
        config.set("Categories", null);

        file.saveConfig();
    }

    @Override
    public void destroy() {
        this.icons.clear();
    }

    public List<Icon> getPages() {
        List<Icon> icons = new ArrayList<>();

        for(Icon icon : this.icons) {
            if(icon.isPage()) icons.add(icon);
        }

        return icons;
    }

    public List<Icon> getPages(Icon page) {
        if(page != null && !page.isPage()) throw new IllegalArgumentException("Given icon is not a category!");
        List<Icon> icons = new ArrayList<>();

        for(Icon icon : this.icons) {
            if(icon.isPage() && Objects.equals(page, icon.getPage())) icons.add(icon);
        }

        return icons;
    }

    public boolean boundToWorld() {
        return WarpSystem.getInstance().getFileManager().getFile("Config").getConfig().getBoolean("WarpSystem.GUI.Bound_to_world", false);
    }

    private int getNextFreeSlot(Icon page) {
        int slot = 0;

        boolean available;

        do {
            available = true;

            if(slot > 53) {
                available = false;
                break;
            }

            if(page == null) {
                for(Icon c : getPages()) {
                    if(c.getSlot() == slot) {
                        slot++;
                        available = false;
                        break;
                    }
                }
            }

            for(Icon warp : getIcons(page)) {
                if(warp.getSlot() == slot) {
                    slot++;
                    available = false;
                    break;
                }
            }
        } while(!available);

        if(available) return slot;
        else return -999;
    }

    public boolean importPageData(PageData pageData) {
        if(this.existsPage(pageData.getName())) return false;

        int slot = getNextFreeSlot(null);
        if(slot == -999) return false;

        Icon icon = new Icon(pageData.getName(), STANDARD_ITEM().setName(pageData.getName()).getItem(), null, slot, pageData.getPermission());
        icon.setPage(true);
        this.icons.add(icon);

        boolean result = true;

        for(WarpData warpData : pageData.getWarps()) {
            if(!importWarpData(warpData)) result = false;
        }

        return result;
    }

    public boolean importWarpData(WarpData warpData) {
        if(SimpleWarpManager.getInstance().existsWarp(warpData.getName())) return false;
        if(warpData.getPage() != null && !existsPage(warpData.getPage())) return false;
        if(this.existsIcon(warpData.getName())) return false;

        Icon page = warpData.getPage() == null ? null : getPage(warpData.getPage());

        int slot = getNextFreeSlot(page);
        if(slot == -999) return false;

        SimpleWarpManager.getInstance().addWarp(new SimpleWarp(warpData));

        Icon icon = new Icon(warpData.getName(), STANDARD_ITEM().setName(warpData.getName()).getItem(), page, slot, warpData.getPermission(), new WarpAction(new Destination(warpData.getName(), DestinationType.SimpleWarp)));
        this.icons.add(icon);
        return true;
    }

    public boolean existsIcon(String name) {
        if(name == null) return false;
        return getIcon(name) != null;
    }

    public boolean existsPage(String name) {
        if(name == null) return false;
        return getPage(name) != null;
    }

    public Icon getPage(String name) {
        if(name == null) return null;
        name = Color.removeColor(Color.translateAlternateColorCodes('&', name));

        for(Icon icon : this.icons) {
            if(!icon.isPage()) continue;
            if(icon.getNameWithoutColor().equalsIgnoreCase(name)) return icon;
        }

        return null;
    }

    public Icon getIcon(String name) {
        if(name == null) return null;
        name = Color.removeColor(Color.translateAlternateColorCodes('&', name));

        for(Icon icon : this.icons) {
            if(icon.isPage() || icon.getName() == null) continue;
            if(icon.getNameWithoutColor().equalsIgnoreCase(name)) return icon;
        }

        return null;
    }

    public List<Icon> getIcons(Icon page) {
        if(page != null && !page.isPage()) throw new IllegalArgumentException("Given icon is not a page!");
        List<Icon> icons = new ArrayList<>();

        for(Icon icon : this.icons) {
            if(Objects.equals(page, icon.getPage())) icons.add(icon);
        }

        return icons;
    }

    public void remove(Icon icon) {
        if(icon.isPage()) {
            List<Icon> warps = getIcons(icon);

            for(Icon warp : warps) {
                remove(warp);
            }
        }

        this.icons.remove(icon);
    }

    public List<Icon> getIcons() {
        return icons;
    }

    public ItemStack getBackground() {
        return background;
    }

    public void setBackground(ItemStack background) {
        if(background == null) background = new ItemStack(Material.AIR);
        new ItemBuilder(background).removeLore().setName(null).setHideName(true).setHideStandardLore(true).setHideEnchantments(true);
        this.background = background;
    }
}
