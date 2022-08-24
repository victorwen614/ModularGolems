package dev.xkmc.modulargolems.content.entity.common;

import com.google.common.base.Suppliers;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.xkmc.modulargolems.content.config.GolemMaterial;
import dev.xkmc.modulargolems.content.core.GolemType;
import dev.xkmc.modulargolems.content.item.GolemHolder;
import dev.xkmc.modulargolems.content.item.GolemPart;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import java.util.ArrayList;
import java.util.Optional;
import java.util.function.Supplier;

public class GolemBEWLR extends BlockEntityWithoutLevelRenderer {

	public static final Supplier<BlockEntityWithoutLevelRenderer> INSTANCE = Suppliers.memoize(() ->
			new GolemBEWLR(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels()));

	public static final IClientItemExtensions EXTENSIONS = new IClientItemExtensions() {

		@Override
		public BlockEntityWithoutLevelRenderer getCustomRenderer() {
			return INSTANCE.get();
		}

	};

	public GolemBEWLR(BlockEntityRenderDispatcher dispatcher, EntityModelSet set) {
		super(dispatcher, set);
	}

	@Override
	public void renderByItem(ItemStack stack, ItemTransforms.TransformType type, PoseStack poseStack,
							 MultiBufferSource bufferSource, int light, int overlay) {
		if (stack.getItem() instanceof GolemPart<?, ?> part) {
			render(part, stack);
		}
		if (stack.getItem() instanceof GolemHolder<?, ?> holder) {
			render(holder, stack);
		}
	}

	private <T extends AbstractGolemEntity<T, P>, P> void render(GolemHolder<T, P> item, ItemStack stack) {
		ArrayList<GolemMaterial> list = GolemHolder.getMaterial(stack);
		P[] parts = item.getEntityType().values();
		for (int i = 0; i < Math.min(list.size(), parts.length); i++) {
			renderPart(list.get(i).id(), item.getEntityType(), parts[i]);
		}
	}

	private <T extends AbstractGolemEntity<T, P>, P> void render(GolemPart<T, P> item, ItemStack stack) {
		Optional<ResourceLocation> id = GolemPart.getMaterial(stack);
		id.ifPresent(rl -> renderPart(rl, item.getEntityType(), item.getPart()));
	}

	private <T extends AbstractGolemEntity<T, P>, P> void renderPart(ResourceLocation id, GolemType<T, P> type, P part) {
		//TODO
	}

}
