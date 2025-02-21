package dev.xkmc.modulargolems.content.entity.humanoid;

import dev.xkmc.l2library.serial.SerialClass;
import dev.xkmc.modulargolems.content.entity.common.SweepGolemEntity;
import dev.xkmc.modulargolems.content.entity.common.goals.FollowOwnerGoal;
import dev.xkmc.modulargolems.content.entity.common.goals.GolemFloatGoal;
import dev.xkmc.modulargolems.content.entity.humanoid.ranged.GolemBowAttackGoal;
import dev.xkmc.modulargolems.content.entity.humanoid.ranged.GolemCrossbowAttackGoal;
import dev.xkmc.modulargolems.content.entity.humanoid.ranged.GolemShooterHelper;
import dev.xkmc.modulargolems.content.entity.humanoid.ranged.GolemTridentAttackGoal;
import dev.xkmc.modulargolems.content.item.WandItem;
import dev.xkmc.modulargolems.content.item.golem.GolemHolder;
import dev.xkmc.modulargolems.events.event.GolemBowAttackEvent;
import dev.xkmc.modulargolems.events.event.GolemDisableShieldEvent;
import dev.xkmc.modulargolems.events.event.GolemEquipEvent;
import dev.xkmc.modulargolems.init.advancement.GolemTriggers;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.monster.CrossbowAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.ToolActions;

import java.util.Arrays;
import java.util.function.Predicate;

@SerialClass
public class HumanoidGolemEntity extends SweepGolemEntity<HumanoidGolemEntity, HumaniodGolemPartType> implements CrossbowAttackMob {

	private static final EntityDataAccessor<Boolean> IS_CHARGING_CROSSBOW = SynchedEntityData.defineId(HumanoidGolemEntity.class, EntityDataSerializers.BOOLEAN);

	private final GolemBowAttackGoal bowGoal = new GolemBowAttackGoal(this, 1.0D, 20, 15.0F);
	private final GolemCrossbowAttackGoal crossbowGoal = new GolemCrossbowAttackGoal(this, 1.0D, 15.0F);
	private final MeleeAttackGoal meleeGoal = new MeleeAttackGoal(this, 1.0D, true);
	private final GolemTridentAttackGoal tridentGoal = new GolemTridentAttackGoal(this, 1, 40, 15);

	@SerialClass.SerialField(toClient = true)
	public int shieldCooldown = 0;

	public HumanoidGolemEntity(EntityType<HumanoidGolemEntity> type, Level level) {
		super(type, level);
		if (!this.level.isClientSide) {
			reassessWeaponGoal();
		}
	}

	public void reassessWeaponGoal() {
		if (!this.level.isClientSide) {
			this.goalSelector.removeGoal(this.meleeGoal);
			this.goalSelector.removeGoal(this.bowGoal);
			this.goalSelector.removeGoal(this.crossbowGoal);
			this.goalSelector.removeGoal(this.tridentGoal);
			ItemStack weapon = this.getMainHandItem();
			if (!weapon.isEmpty() && GolemShooterHelper.isValidThrowableWeapon(this, weapon).isThrowable()) {
				this.goalSelector.addGoal(1, this.tridentGoal);
				return;
			}
			if (!weapon.isEmpty() && weapon.getItem() instanceof BowItem) {
				this.bowGoal.setMinAttackInterval(20);
				this.goalSelector.addGoal(1, this.bowGoal);
				return;
			}
			if (!weapon.isEmpty() && weapon.getItem() instanceof CrossbowItem) {
				this.goalSelector.addGoal(1, this.crossbowGoal);
				return;
			}
			this.goalSelector.addGoal(1, this.meleeGoal);
		}
	}

	public ItemStack getProjectile(ItemStack pShootable) {
		if (pShootable.getItem() instanceof ProjectileWeaponItem) {
			Predicate<ItemStack> predicate = ((ProjectileWeaponItem) pShootable.getItem()).getSupportedHeldProjectiles();
			ItemStack itemstack = ProjectileWeaponItem.getHeldProjectile(this, predicate);
			return net.minecraftforge.common.ForgeHooks.getProjectile(this, pShootable, itemstack);
		} else {
			return net.minecraftforge.common.ForgeHooks.getProjectile(this, pShootable, ItemStack.EMPTY);
		}
	}

	public void readAdditionalSaveData(CompoundTag pCompound) {
		super.readAdditionalSaveData(pCompound);
		this.reassessWeaponGoal();
	}

	public void setItemSlot(EquipmentSlot pSlot, ItemStack pStack) {
		super.setItemSlot(pSlot, pStack);
		if (!this.level.isClientSide) {
			this.reassessWeaponGoal();
		}
	}

	protected void defineSynchedData() {
		super.defineSynchedData();
		this.entityData.define(IS_CHARGING_CROSSBOW, false);
	}

	protected AbstractArrow getArrow(ItemStack pArrowStack, float pVelocity) {
		return ProjectileUtil.getMobArrow(this, pArrowStack, pVelocity);
	}

	public boolean canFireProjectileWeapon(ProjectileWeaponItem pProjectileWeapon) {
		return true;
	}

	public boolean isChargingCrossbow() {
		return this.entityData.get(IS_CHARGING_CROSSBOW);
	}

	public void setChargingCrossbow(boolean pIsCharging) {
		this.entityData.set(IS_CHARGING_CROSSBOW, pIsCharging);
	}

	@Override
	public void shootCrossbowProjectile(LivingEntity pTarget, ItemStack pCrossbowStack, Projectile pProjectile, float pProjectileAngle) {
		shootCrossbowProjectile(this, pTarget, pProjectile, pProjectileAngle, 1.6F);
	}

	public void shootCrossbowProjectile(LivingEntity pUser, LivingEntity pTarget, Projectile pProjectile, float pProjectileAngle, float pVelocity) {
		GolemShooterHelper.shootAimHelper(pTarget, pProjectile);
		pUser.playSound(SoundEvents.CROSSBOW_SHOOT, 1.0F, 1.0F / (pUser.getRandom().nextFloat() * 0.4F + 0.8F));
	}

	@Override
	public void onCrossbowAttackPerformed() {
		noActionTime = 0;
	}

	public void performCrossbowAttack(LivingEntity pUser, float pVelocity) {
		InteractionHand interactionhand = ProjectileUtil.getWeaponHoldingHand(pUser, item -> item instanceof CrossbowItem);
		ItemStack itemstack = pUser.getItemInHand(interactionhand);
		if (pUser.isHolding(is -> is.getItem() instanceof CrossbowItem)) {
			CrossbowItem.performShooting(pUser.level, pUser, interactionhand, itemstack, pVelocity, 0);
		}
		this.onCrossbowAttackPerformed();
	}

	@Override
	public void performRangedAttack(LivingEntity pTarget, float dist) {
		ItemStack stack = this.getMainHandItem();
		var throwable = GolemShooterHelper.isValidThrowableWeapon(this, stack);
		if (throwable.isThrowable()) {
			Projectile projectile = throwable.createProjectile(level);
			GolemShooterHelper.shootAimHelper(pTarget, projectile);
			this.playSound(SoundEvents.TRIDENT_THROW, 1.0F, 1.0F / (this.getRandom().nextFloat() * 0.4F + 0.8F));
			this.level.addFreshEntity(projectile);
			stack.hurtAndBreak(1, this, e -> e.broadcastBreakEvent(InteractionHand.MAIN_HAND));
		} else if (stack.getItem() instanceof CrossbowItem) {
			performCrossbowAttack(this, 3);
		} else if (stack.getItem() instanceof BowItem bow) {
			ItemStack arrowStack = this.getProjectile(stack);
			if (arrowStack.isEmpty()) return;
			AbstractArrow arrowEntity = bow.customArrow(getArrow(arrowStack, dist));
			boolean infinite = GolemShooterHelper.arrowIsInfinite(arrowStack, stack);
			GolemBowAttackEvent event = new GolemBowAttackEvent(this, stack, arrowEntity, infinite);
			MinecraftForge.EVENT_BUS.post(event);
			arrowEntity = event.getArrow();
			if (event.isNoPickup()) {
				arrowEntity.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
			} else {
				arrowEntity.pickup = AbstractArrow.Pickup.ALLOWED;
			}
			if (!event.isNoConsume()) {
				arrowStack.shrink(1);
			}
			GolemShooterHelper.shootAimHelper(pTarget, arrowEntity, event.speed(), event.gravity());
			this.playSound(SoundEvents.ARROW_SHOOT, 1.0F, 1.0F / (this.getRandom().nextFloat() * 0.4F + 0.8F));
			this.level.addFreshEntity(arrowEntity);
			stack.hurtAndBreak(1, this, e -> e.broadcastBreakEvent(InteractionHand.MAIN_HAND));
		}
	}

	// ------ common golem behavior

	protected void registerGoals() {
		this.goalSelector.addGoal(0, new GolemFloatGoal(this));
		this.goalSelector.addGoal(6, new FollowOwnerGoal(this));
		this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 6.0F));
		this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
		registerTargetGoals();
	}

	@Override
	public void broadcastBreakEvent(EquipmentSlot pSlot) {
		super.broadcastBreakEvent(pSlot);
		Player player = getOwner();
		if (player != null) {
			GolemTriggers.BREAK.trigger((ServerPlayer) player);
		}
	}

	public boolean doHurtTarget(Entity target) {
		boolean can_sweep = getMainHandItem().canPerformAction(ToolActions.SWORD_SWEEP);
		if (!can_sweep) {
			return super.doHurtTarget(target);
		} else {
			if (performRangedDamage(target, 0, 0)) {
				ItemStack stack = getItemBySlot(EquipmentSlot.MAINHAND);
				stack.hurtAndBreak(1, this, self -> self.broadcastBreakEvent(EquipmentSlot.MAINHAND));
				return true;
			}
			return false;
		}
	}

	protected AABB getTargetBoundingBox(Entity target) {
		return target.getBoundingBox();
	}

	@Override
	protected boolean performDamageTarget(Entity target, float damage, double kb) {
		return super.doHurtTarget(target);
	}

	@Override
	protected InteractionResult mobInteract(Player player, InteractionHand hand) {
		if (player.getItemInHand(hand).getItem() instanceof WandItem) return InteractionResult.PASS;
		ItemStack itemstack = player.getItemInHand(hand);
		if (player.isShiftKeyDown()) {
			for (EquipmentSlot slot : EquipmentSlot.values()) {
				dropSlot(slot, false);
			}
			if (itemstack.isEmpty()) {
				super.mobInteract(player, hand);
			}
			return InteractionResult.SUCCESS;
		}
		if (itemstack.isEmpty()) {
			return super.mobInteract(player, hand);
		}
		if ((itemstack.getItem() instanceof GolemHolder) || !itemstack.getItem().canFitInsideContainerItems()) {
			return InteractionResult.FAIL;
		}
		GolemEquipEvent event = new GolemEquipEvent(this, itemstack);
		MinecraftForge.EVENT_BUS.post(event);
		if (event.canEquip()) {
			if (level.isClientSide()) {
				return InteractionResult.SUCCESS;
			}
			if (hasItemInSlot(event.getSlot())) {
				dropSlot(event.getSlot(), false);
			}
			if (hasItemInSlot(event.getSlot())) {
				return InteractionResult.FAIL;
			}
			setItemSlot(event.getSlot(), itemstack.split(event.getAmount()));
			int count = (int) Arrays.stream(EquipmentSlot.values()).filter(e -> !getItemBySlot(e).isEmpty()).count();
			GolemTriggers.EQUIP.trigger((ServerPlayer) player, count);
			return InteractionResult.CONSUME;
		}
		return InteractionResult.FAIL;
	}

	protected void dropCustomDeathLoot(DamageSource source, int i, boolean b) {
		for (EquipmentSlot slot : EquipmentSlot.values()) {
			dropSlot(slot, true);
		}
		super.dropCustomDeathLoot(source, i, b);
	}

	private void dropSlot(EquipmentSlot slot, boolean isDeath) {
		ItemStack itemstack = this.getItemBySlot(slot);
		if (itemstack.isEmpty()) return;
		if (!isDeath && EnchantmentHelper.hasBindingCurse(itemstack)) return;
		if (isDeath && EnchantmentHelper.hasVanishingCurse(itemstack)) return;
		this.spawnAtLocation(itemstack);
		this.setItemSlot(slot, ItemStack.EMPTY);
	}

	// ------ player equipment hurt

	@Override
	protected void hurtArmor(DamageSource source, float damage) {
		if (damage <= 0.0F) return;
		damage /= 4.0F;
		if (damage < 1.0F) {
			damage = 1.0F;
		}
		for (EquipmentSlot slot : EquipmentSlot.values()) {
			if (slot.getType() != EquipmentSlot.Type.ARMOR) continue;
			ItemStack itemstack = this.getItemBySlot(slot);
			if ((!source.isFire() || !itemstack.getItem().isFireResistant()) && itemstack.getItem() instanceof ArmorItem) {
				itemstack.hurtAndBreak((int) damage, this, (entity) -> entity.broadcastBreakEvent(slot));
			}
		}
	}

	@Override
	public boolean isBlocking() {
		return shieldCooldown == 0 && getItemBySlot(EquipmentSlot.OFFHAND).canPerformAction(ToolActions.SHIELD_BLOCK);
	}

	protected void hurtCurrentlyUsedShield(float damage) {
		ItemStack stack = getItemBySlot(EquipmentSlot.OFFHAND);
		if (!stack.canPerformAction(ToolActions.SHIELD_BLOCK)) return;
		if (damage < 3.0F) return;
		int i = 1 + Mth.floor(damage);
		stack.hurtAndBreak(i, this, (self) -> self.broadcastBreakEvent(EquipmentSlot.OFFHAND));
		if (stack.isEmpty()) {
			this.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
		}
		this.playSound(SoundEvents.SHIELD_BREAK, 0.8F, 0.8F + this.level.random.nextFloat() * 0.4F);
	}

	protected void blockUsingShield(LivingEntity source) {
		super.blockUsingShield(source);
		boolean canDisable = source.canDisableShield() || source.getMainHandItem().canDisableShield(this.getOffhandItem(), this, source);
		GolemDisableShieldEvent event = new GolemDisableShieldEvent(this, source, canDisable);
		MinecraftForge.EVENT_BUS.post(event);
		if (event.shouldDisable()) {
			this.shieldCooldown = 100;
			this.level.broadcastEntityEvent(this, EntityEvent.SHIELD_DISABLED);
		}
	}

	@Override
	public void handleEntityEvent(byte event) {
		if (event == EntityEvent.SHIELD_DISABLED) {
			shieldCooldown = 100;
		}
		super.handleEntityEvent(event);
	}

	@Override
	public void tick() {
		super.tick();
		shieldCooldown = Mth.clamp(shieldCooldown - 1, 0, 100);
	}

	@Override
	public void aiStep() {
		super.aiStep();
		for (EquipmentSlot slot : EquipmentSlot.values()) {
			ItemStack stack = getItemBySlot(slot);
			if (!stack.isEmpty()) {
				stack.inventoryTick(level, this, slot.ordinal(), slot == EquipmentSlot.MAINHAND);
			}
		}
	}

}
