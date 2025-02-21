package dev.xkmc.modulargolems.content.entity.dog;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;
import dev.xkmc.modulargolems.content.entity.common.AbstractGolemRenderer;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class DogGolemRenderer extends AbstractGolemRenderer<DogGolemEntity, DogGolemPartType, DogGolemModel> {

	protected static void transform(PoseStack stack, ItemTransforms.TransformType transform, @Nullable DogGolemPartType part) {
		switch (transform) {
			case GUI:
			case FIRST_PERSON_LEFT_HAND:
			case FIRST_PERSON_RIGHT_HAND:
				break;
			case THIRD_PERSON_LEFT_HAND:
			case THIRD_PERSON_RIGHT_HAND: {
				stack.translate(0.25, 0.4, 0.5);
				float size = 1F;
				stack.scale(size, size, size);
				break;
			}
			case GROUND: {
				stack.translate(0.25, 0, 0.5);
				float size = 1F;
				stack.scale(size, size, size);
				break;
			}
			case NONE:
			case HEAD:
			case FIXED: {
				stack.translate(0.5, 0.5, 0.5);
				float size = 1f;
				stack.scale(size, -size, size);
				stack.translate(0, -0.5, 0);
				return;
			}
		}
		stack.mulPose(Vector3f.ZP.rotationDegrees(135));
		stack.mulPose(Vector3f.YP.rotationDegrees(-155));
		if (part == null) {
			float size = 0.8f;
			stack.scale(size, size, size);
			stack.translate(0, -1.9, 0);
		} else if (part == DogGolemPartType.BODY) {
			float size = 0.9f;
			stack.scale(size, size, size);
			stack.translate(0, -1.6, 0);
		} else if (part == DogGolemPartType.LEGS) {
			float size = 1f;
			stack.scale(size, size, size);
			stack.translate(0, -1.9, 0);
		}
	}

	public DogGolemRenderer(EntityRendererProvider.Context ctx) {
		super(ctx, new DogGolemModel(ctx.bakeLayer(ModelLayers.WOLF)), 0.5F, DogGolemPartType::values);
	}

	protected float getBob(DogGolemEntity dog, float pPartialTicks) {
		return dog.getTailAngle();
	}

}