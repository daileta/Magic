package net.evan.magic.client.render;

import com.zigythebird.playeranim.api.PlayerAnimationFactory;
import com.zigythebird.playeranimcore.animation.layered.ModifierLayer;
import net.evan.magic.Magic;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animatable.manager.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

public final class ClientLibraryValidation {
	private static final Identifier PAL_VALIDATION_LAYER_ID = Identifier.of(Magic.MOD_ID, "pal_validation");
	private static final ValidationGeoAnimatable GECKOLIB_VALIDATION = new ValidationGeoAnimatable();
	private static boolean palRegistered;

	private ClientLibraryValidation() {
	}

	public static void initialize() {
		initializeGeckoLib();
		initializePal();
	}

	private static void initializeGeckoLib() {
		AnimatableManager<?> manager = GECKOLIB_VALIDATION.getAnimatableInstanceCache().getManagerForId(0L);
		Magic.LOGGER.info("GeckoLib validation manager initialized with controllers {}", manager.getAnimationControllers().keySet());
	}

	private static void initializePal() {
		if (palRegistered) {
			return;
		}

		PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(
			PAL_VALIDATION_LAYER_ID,
			0,
			player -> new ModifierLayer<>()
		);
		palRegistered = true;
		Magic.LOGGER.info("Player Animation Library validation layer registered as {}", PAL_VALIDATION_LAYER_ID);
	}

	private static final class ValidationGeoAnimatable implements GeoAnimatable {
		private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

		@Override
		public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
			controllers.add(new AnimationController<>("magic_validation", animationState -> PlayState.STOP));
		}

		@Override
		public AnimatableInstanceCache getAnimatableInstanceCache() {
			return this.cache;
		}
	}
}

