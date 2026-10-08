package de.jan.tennis.entity;

import de.jan.tennis.court.Court;
import de.jan.tennis.logic.BallPhysics;
import de.jan.tennis.logic.CourtGeometry;
import de.jan.tennis.logic.Surface;
import de.jan.tennis.logic.V3;
import de.jan.tennis.registry.ModEntities;
import de.jan.tennis.registry.ModItems;
import de.jan.tennis.registry.ModSounds;
import java.util.UUID;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

/**
 * Der Tennisball. Fliegt mit echter Ballphysik (Schwerkraft, Luftwiderstand, Magnus-Effekt) und springt
 * je nach Belag unterschiedlich ab. Die Physik läuft auf Server und Client, damit der Ball flüssig fliegt;
 * die Regeln entscheidet nur der Server.
 */
public class TennisBallEntity extends Entity implements ItemSupplier {
	private static final EntityDataAccessor<Vector3fc> DATA_SPIN = SynchedEntityData.defineId(TennisBallEntity.class, EntityDataSerializers.VECTOR3);
	private static final int MAX_AGE = 20 * 60;
	private static final ItemStack RENDER_STACK = new ItemStack(ModItems.TENNIS_BALL);

	private @Nullable BallListener listener;
	private @Nullable Court court;
	private @Nullable UUID lastHitter;
	private int lastHitTick = -100;
	private int restingTicks;
	private boolean dead;
	private boolean serveToss;

	public TennisBallEntity(EntityType<? extends TennisBallEntity> type, Level level) {
		super(type, level);
	}

	public static TennisBallEntity spawn(ServerLevel level, Vec3 pos, Vec3 velocity, @Nullable Court court) {
		TennisBallEntity ball = new TennisBallEntity(ModEntities.TENNIS_BALL, level);
		ball.setPos(pos);
		ball.setDeltaMovement(velocity);
		ball.court = court;
		level.addFreshEntity(ball);
		return ball;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		builder.define(DATA_SPIN, new Vector3f());
	}

	public V3 getSpin() {
		Vector3fc s = entityData.get(DATA_SPIN);
		return new V3(s.x(), s.y(), s.z());
	}

	public void setSpin(V3 spin) {
		entityData.set(DATA_SPIN, new Vector3f((float) spin.x(), (float) spin.y(), (float) spin.z()));
	}

	@Override
	public void tick() {
		super.tick();
		if (dead) {
			return;
		}
		V3 spin = getSpin();
		V3 vel = BallPhysics.stepVelocity(toV3(getDeltaMovement()), spin);
		if (isInWater()) {
			vel = vel.scale(0.7);
		}
		Vec3 before = toVec(vel);
		Vec3 start = position();
		move(MoverType.SELF, before);
		Vec3 moved = position().subtract(start);
		boolean server = !level().isClientSide();

		boolean xCollision = Math.abs(moved.x - before.x) > 1.0e-5;
		boolean zCollision = Math.abs(moved.z - before.z) > 1.0e-5;
		boolean groundHit = verticalCollision && before.y < 0;
		boolean ceilingHit = verticalCollision && before.y > 0;

		if (groundHit && -before.y < 0.05) {
			// rollt oder liegt: nur Rollreibung
			vel = new V3(vel.x() * 0.95, 0, vel.z() * 0.95);
		} else if (groundHit) {
			Surface surface = surfaceBelow();
			V3 bounced = BallPhysics.bounce(vel, spin, surface);
			vel = bounced;
			if (server) {
				setSpin(BallPhysics.spinAfterBounce(spin));
				playSound(ModSounds.BALL_BOUNCE, (float) Math.min(1.0, -before.y * 2.5), 0.9F + random.nextFloat() * 0.2F);
				if (listener != null) {
					listener.onBounce(this, position());
				}
			}
		} else if (ceilingHit) {
			vel = new V3(vel.x(), -vel.y() * 0.4, vel.z());
		}
		if (xCollision || zCollision) {
			boolean net = isAtNet();
			double factor = net ? -0.08 : -0.45;
			vel = new V3(xCollision ? vel.x() * factor : vel.x(), vel.y() * (net ? 0.5 : 0.9), zCollision ? vel.z() * factor : vel.z());
			if (server) {
				setSpin(getSpin().scale(0.3));
				playSound(net ? ModSounds.NET_HIT : ModSounds.BALL_BOUNCE, 0.6F, net ? 1.0F : 0.7F);
				if (listener != null) {
					if (net) {
						listener.onNetTouch(this);
					} else {
						listener.onObstacle(this);
					}
				}
			}
		}
		if (server && !groundHit && court != null && Math.abs(court.u(start)) > 0.05 && Math.signum(court.u(start)) != Math.signum(court.u(position()))) {
			// Ball überquert das Netz knapp: Netzkante berührt?
			double netTop = court.groundY() + 1.0;
			if (getY() - netTop < 0.12 && getY() > netTop - 0.05 && listener != null) {
				playSound(ModSounds.NET_HIT, 0.4F, 1.4F);
				listener.onNetTouch(this);
			}
		}
		setDeltaMovement(toVec(vel));
		if (server) {
			setSpin(getSpin().scale(BallPhysics.SPIN_DECAY));
			checkDeath(vel);
		}
	}

	private void checkDeath(V3 vel) {
		if (vel.length() < 0.02 && onGround()) {
			restingTicks++;
		} else {
			restingTicks = 0;
		}
		boolean outside = court != null && !court.contains(level(), position());
		if (restingTicks > 30 || tickCount > MAX_AGE || isInWater() || getY() < level().getMinY() || outside) {
			markDead();
		}
	}

	/** Ball ist aus dem Spiel: Schiedsrichter informieren und nach kurzer Zeit entfernen. */
	public void markDead() {
		if (dead) {
			return;
		}
		dead = true;
		if (listener != null) {
			BallListener l = listener;
			listener = null;
			l.onDead(this);
		}
		if (level() instanceof ServerLevel) {
			discard();
		}
	}

	private boolean isAtNet() {
		if (court != null) {
			return Math.abs(court.u(position())) < 1.0 && getY() < court.groundY() + 1.3;
		}
		BlockState state = level().getBlockState(blockPosition());
		return state.is(Blocks.IRON_BARS);
	}

	private Surface surfaceBelow() {
		if (court != null && CourtGeometry.inArea(court.u(position()), court.v(position()))) {
			return court.surface();
		}
		BlockState below = level().getBlockState(blockPosition().below());
		if (below.is(BlockTags.SAND) || below.is(Blocks.TERRACOTTA)) {
			return Surface.SAND;
		}
		if (below.is(Blocks.GRASS_BLOCK) || below.is(Blocks.MOSS_BLOCK)) {
			return Surface.RASEN;
		}
		return Surface.HARTPLATZ;
	}

	/** Schlag ausführen: neue Geschwindigkeit und Spin setzen und sofort an alle Spieler senden. */
	public void hit(ServerPlayer player, Vec3 velocity, V3 spin) {
		setDeltaMovement(velocity);
		setSpin(spin);
		hurtMarked = true;
		needsSync = true;
		lastHitter = player.getUUID();
		lastHitTick = tickCount;
		restingTicks = 0;
		if (listener != null) {
			listener.onHit(this, player);
		}
		serveToss = false;
	}

	public boolean recentlyHitBy(ServerPlayer player, int ticks) {
		return player.getUUID().equals(lastHitter) && tickCount - lastHitTick < ticks;
	}

	@Override
	public boolean skipAttackInteraction(Entity source) {
		// Linksklick mit dem Schläger: schneller Schlag ohne Ausholen
		if (source instanceof ServerPlayer player && level() instanceof ServerLevel) {
			de.jan.tennis.item.ShotMaker.quickHit(player, this);
		}
		return true;
	}

	@Override
	public boolean isPickable() {
		return !dead;
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		return false;
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
	}

	@Override
	public ItemStack getItem() {
		return RENDER_STACK;
	}

	public void playSound(net.minecraft.sounds.SoundEvent sound, float volume, float pitch) {
		level().playSound(null, getX(), getY(), getZ(), sound, SoundSource.PLAYERS, volume, pitch);
	}

	public @Nullable BallListener getListener() {
		return listener;
	}

	public void setListener(@Nullable BallListener listener) {
		this.listener = listener;
	}

	public @Nullable Court getCourt() {
		return court;
	}

	public void setCourt(@Nullable Court court) {
		this.court = court;
	}

	public @Nullable UUID getLastHitter() {
		return lastHitter;
	}

	public boolean isServeToss() {
		return serveToss;
	}

	public void setServeToss(boolean serveToss) {
		this.serveToss = serveToss;
	}

	public boolean isDead() {
		return dead;
	}

	public static V3 toV3(Vec3 v) {
		return new V3(v.x, v.y, v.z);
	}

	public static Vec3 toVec(V3 v) {
		return new Vec3(v.x(), v.y(), v.z());
	}
}
