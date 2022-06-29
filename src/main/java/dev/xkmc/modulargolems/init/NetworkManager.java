package dev.xkmc.modulargolems.init;

import dev.xkmc.l2library.serial.network.BaseConfig;
import dev.xkmc.l2library.serial.network.PacketHandlerWithConfig;
import dev.xkmc.modulargolems.content.config.GolemMaterialConfig;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public enum NetworkManager {
	MATERIAL;

	public String getID() {
		return name().toLowerCase(Locale.ROOT);
	}

	public static final PacketHandlerWithConfig HANDLER = new PacketHandlerWithConfig(
			new ResourceLocation(ModularGolems.MODID, "main"), 1, "artifact_config"
	);

	public <T extends BaseConfig> T getMerged() {
		return HANDLER.getCachedConfig(getID());
	}

	public static void register() {
		HANDLER.addCachedConfig(MATERIAL.getID(), s -> {
			List<GolemMaterialConfig> list = s.map(e -> (GolemMaterialConfig) e.getValue()).toList();
			var stats = BaseConfig.collectMap(list, e -> e.stats, HashMap::new, HashMap::putAll);
			var modifiers = BaseConfig.collectMap(list, e -> e.modifiers, ArrayList::new, ArrayList::addAll);
			var ingredient = BaseConfig.overrideMap(list, e -> e.ingredient);
			GolemMaterialConfig ans = new GolemMaterialConfig();
			ans.stats = stats;
			ans.modifiers = modifiers;
			ans.ingredient = ingredient;
			ans.validate();
			return ans;
		});
	}

}
