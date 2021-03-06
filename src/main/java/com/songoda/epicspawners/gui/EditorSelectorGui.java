package com.songoda.epicspawners.gui;

import com.songoda.core.compatibility.CompatibleMaterial;
import com.songoda.core.gui.Gui;
import com.songoda.core.gui.GuiUtils;
import com.songoda.core.utils.TextUtils;
import com.songoda.epicspawners.EpicSpawners;
import com.songoda.epicspawners.settings.Settings;
import com.songoda.epicspawners.spawners.spawner.SpawnerData;
import com.songoda.epicspawners.utils.HeadUtils;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class EditorSelectorGui extends Gui {

    private final EpicSpawners plugin;
    private final Player player;
    private Type shownType = Type.BOTH;
    private final List<SpawnerData> entities = new ArrayList<>();

    public EditorSelectorGui(EpicSpawners plugin, Player player) {
        super(6);
        this.plugin = plugin;
        this.player = player;

        entities.addAll(plugin.getSpawnerManager().getAllEnabledSpawnerData());
        setTitle("Spawner Selector");

        showPage();
    }

    public void showPage() {
        reset();

        // decorate the edges
        ItemStack glass2 = GuiUtils.getBorderItem(Settings.GLASS_TYPE_2.getMaterial(CompatibleMaterial.BLUE_STAINED_GLASS_PANE));
        ItemStack glass3 = GuiUtils.getBorderItem(Settings.GLASS_TYPE_3.getMaterial(CompatibleMaterial.LIGHT_BLUE_STAINED_GLASS_PANE));

        // edges will be type 3
        mirrorFill(0, 2, true, true, glass3);
        mirrorFill(1, 1, true, true, glass3);

        // decorate corners with type 2
        mirrorFill(0, 0, true, true, glass2);
        mirrorFill(1, 0, true, true, glass2);
        mirrorFill(0, 1, true, true, glass2);

        pages = (int) Math.max(1, Math.ceil(entities.size() / ((double) 28)));

        // enable page event
        setNextPage(5, 7, GuiUtils.createButtonItem(CompatibleMaterial.ARROW, plugin.getLocale().getMessage("general.nametag.next").getMessage()));
        setPrevPage(5, 1, GuiUtils.createButtonItem(CompatibleMaterial.ARROW, plugin.getLocale().getMessage("general.nametag.back").getMessage()));
        setOnPage((event) -> showPage());

        List<SpawnerData> data = entities.stream()
                .filter(s -> shownType == Type.BOTH
                        || shownType == Type.DEFAULT && !s.isCustom()
                        || shownType == Type.CUSTOM && s.isCustom()).skip((page - 1) * 28).limit(28).collect(Collectors.toList());

        setButton(8, GuiUtils.createButtonItem(CompatibleMaterial.OAK_DOOR,
                plugin.getLocale().getMessage("general.nametag.exit").getMessage()), (event) -> close());

        int num = 10;
        for (int i = 0; i < 28; i++) {
            num++;
            SpawnerData spawnerData = i < data.size() ? data.get(i) : null;
            if (num == 16 || num == 36)
                num = num + 2;

            if (spawnerData == null) {
                setItem(num, null);
                continue;
            }
            CompatibleMaterial mat = spawnerData.getDisplayItem();
            setButton(num, GuiUtils.createButtonItem(mat != null && !mat.isAir() ? spawnerData.getDisplayItem().getItem() : HeadUtils.getTexturedSkull(spawnerData),
                    TextUtils.formatText("&6&l" + spawnerData.getIdentifyingName()), TextUtils.formatText("&7Click to &a&lEdit&7.")),
                    (event) -> EditorTiersGui.openTiers(plugin, player, spawnerData));
        }

        setButton(5, 5, GuiUtils.createButtonItem(CompatibleMaterial.COMPASS, TextUtils.formatText("&5&lShow: &7" + shownType.name())),
                (event) -> {
                    shownType = shownType.next();
                    showPage();
                });
        setButton(5, 6, GuiUtils.createButtonItem(CompatibleMaterial.PAPER, TextUtils.formatText("&9&lNew Spawner")),
                (event) -> EditorTiersGui.openTiers(plugin, player, null));
    }

    private enum Type {

        BOTH, CUSTOM, DEFAULT;

        private static Type[] vals = values();

        public Type next() {
            return vals[(this.ordinal() != vals.length - 1 ? this.ordinal() + 1 : 0)];
        }

    }
}
