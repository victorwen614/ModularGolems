package dev.xkmc.modulargolems.compat.materials.l2complements;

import dev.xkmc.l2complements.init.registrate.LCEnchantments;
import dev.xkmc.l2complements.init.registrate.LCItems;
import dev.xkmc.l2library.base.ingredients.EnchantmentIngredient;
import dev.xkmc.l2library.base.recipe.ConditionalRecipeWrapper;
import dev.xkmc.l2library.repack.registrate.providers.RegistrateLangProvider;
import dev.xkmc.l2library.repack.registrate.providers.RegistrateRecipeProvider;
import dev.xkmc.l2library.serial.network.ConfigDataProvider;
import dev.xkmc.modulargolems.compat.materials.common.ModDispatch;
import dev.xkmc.modulargolems.init.registrate.GolemItems;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.world.item.Items;
import net.minecraftforge.eventbus.api.IEventBus;

public class LCDispatch extends ModDispatch {

	public static final String MODID = "l2complements";

	public LCDispatch() {
		LCCompatRegistry.register();
	}

	public void genLang(RegistrateLangProvider pvd) {
		pvd.add("golem_material." + MODID + ".totemic_gold", "Totemic Gold");
		pvd.add("golem_material." + MODID + ".poseidite", "Poseidite");
		pvd.add("golem_material." + MODID + ".shulkerate", "Shulkerate");
		pvd.add("golem_material." + MODID + ".eternium", "Eternium");
	}

	@Override
	public void genRecipe(RegistrateRecipeProvider pvd) {

		safeUpgrade(pvd, new ShapelessRecipeBuilder(LCCompatRegistry.FORCE_FIELD.get(), 1)::unlockedBy, LCItems.FORCE_FIELD.get())
				.requires(GolemItems.EMPTY_UPGRADE.get())
				.requires(LCItems.FORCE_FIELD.get())
				.save(ConditionalRecipeWrapper.mod(pvd, MODID));

		safeUpgrade(pvd, ShapedRecipeBuilder.shaped(LCCompatRegistry.FREEZE_UP.get())::unlockedBy, LCItems.HARD_ICE.get())
				.pattern("CAC").pattern("1B2").pattern("CAC")
				.define('C', Items.GOLD_INGOT)
				.define('A', LCItems.HARD_ICE.get())
				.define('B', GolemItems.EMPTY_UPGRADE.get())
				.define('1', new EnchantmentIngredient(LCEnchantments.ICE_THORN.get(), 1))
				.define('2', new EnchantmentIngredient(LCEnchantments.ICE_BLADE.get(), 1))
				.save(ConditionalRecipeWrapper.mod(pvd, MODID));

		safeUpgrade(pvd, ShapedRecipeBuilder.shaped(LCCompatRegistry.FLAME_UP.get())::unlockedBy, LCItems.SOUL_FLAME.get())
				.pattern("CAC").pattern("1B2").pattern("CAC")
				.define('C', Items.GOLD_INGOT)
				.define('A', LCItems.SOUL_FLAME.get())
				.define('B', GolemItems.EMPTY_UPGRADE.get())
				.define('1', new EnchantmentIngredient(LCEnchantments.FLAME_THORN.get(), 1))
				.define('2', new EnchantmentIngredient(LCEnchantments.FLAME_BLADE.get(), 1))
				.save(ConditionalRecipeWrapper.mod(pvd, MODID));

		safeUpgrade(pvd, new ShapelessRecipeBuilder(LCCompatRegistry.TELEPORT_UP.get(), 1)::unlockedBy, LCItems.VOID_EYE.get())
				.requires(GolemItems.EMPTY_UPGRADE.get())
				.requires(LCItems.VOID_EYE.get())
				.save(ConditionalRecipeWrapper.mod(pvd, MODID));
	}

	@Override
	protected ConfigDataProvider getDataGen(DataGenerator gen) {
		return new LCConfigGen(gen);
	}

	@Override
	public void dispatchClientSetup(IEventBus bus) {
		ForceFieldLayer.registerLayer();
	}
}
