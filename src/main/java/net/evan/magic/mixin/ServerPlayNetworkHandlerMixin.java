package net.evan.magic.mixin;

import net.evan.magic.magic.ability.MagicAbilityManager;
import net.evan.magic.magic.ability.GreedAbilityRuntime;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInputC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayNetworkHandler.class)
public abstract class ServerPlayNetworkHandlerMixin {
	@Shadow
	public ServerPlayerEntity player;

	private String magic$debugName() {
		return player.getName().getString() + "(" + player.getUuid() + ")";
	}

	private boolean magic$shouldLogPacketDebug() {
		if (player.getEntityWorld().getServer() == null || !player.getEntityWorld().getServer().isOnThread()) {
			return false;
		}

		return MagicAbilityManager.isManipulatingCaster(player) || MagicAbilityManager.isManipulationControlledTarget(player);
	}

	private void magic$packetDebug(String message, Object... args) {
		if (!magic$shouldLogPacketDebug()) {
			return;
		}

		MagicAbilityManager.debugManipulationPacket(message, args);
	}

	private boolean magic$isGreedIntroFrozen() {
		return MagicAbilityManager.isGreedDomainIntroFrozen(player);
	}

	private boolean magic$shouldCancelGreedHandUse(Hand hand) {
		if (
			hand == Hand.OFF_HAND
			&& GreedAbilityRuntime.isOffhandBlocked(player)
			&& !GreedAbilityRuntime.canUseEnderPearlWhileRooted(player, player.getStackInHand(hand))
		) {
			GreedAbilityRuntime.onBlockedShieldUse(player);
			return true;
		}
		if (player.getStackInHand(hand).isOf(Items.SHIELD) && GreedAbilityRuntime.isShieldLocked(player)) {
			GreedAbilityRuntime.onBlockedShieldUse(player);
			return true;
		}
		return false;
	}

	private Hand magic$interactionHand(PlayerInteractEntityC2SPacket packet) {
		final Hand[] hand = { null };
		packet.handle(new PlayerInteractEntityC2SPacket.Handler() {
			@Override
			public void interact(Hand packetHand) {
				hand[0] = packetHand;
			}

			@Override
			public void interactAt(Hand packetHand, Vec3d pos) {
				hand[0] = packetHand;
			}

			@Override
			public void attack() {
			}
		});
		return hand[0];
	}

	@Inject(method = "onPlayerMove", at = @At("HEAD"), cancellable = true)
	private void magic$onPlayerMoveHead(PlayerMoveC2SPacket packet, CallbackInfo ci) {
		if (MagicAbilityManager.handleCelestialPlayerMovePacket(player, packet)) {
			ci.cancel();
			return;
		}
		MagicAbilityManager.onManipulationLookPacket(player, packet.getYaw(player.getYaw()), packet.getPitch(player.getPitch()));
		if (magic$isGreedIntroFrozen()) {
			ci.cancel();
			return;
		}
	}

	@Inject(method = "onPlayerMove", at = @At("RETURN"))
	private void magic$onPlayerMoveReturn(PlayerMoveC2SPacket packet, CallbackInfo ci) {
	}

	@Inject(method = "onPlayerInput", at = @At("HEAD"), cancellable = true)
	private void magic$onPlayerInputHead(PlayerInputC2SPacket packet, CallbackInfo ci) {
		if (MagicAbilityManager.handleCelestialPlayerInputPacket(player, packet)) {
			ci.cancel();
			return;
		}
		MagicAbilityManager.onManipulationInputPacket(player, packet.input());
		if (magic$isGreedIntroFrozen()) {
			ci.cancel();
			return;
		}

		magic$packetDebug(
			"{} packet onPlayerInput HEAD: forward={}, backward={}, left={}, right={}, jump={}, sneak={}, sprint={}",
			magic$debugName(),
			packet.input().forward(),
			packet.input().backward(),
			packet.input().left(),
			packet.input().right(),
			packet.input().jump(),
			packet.input().sneak(),
			packet.input().sprint()
		);
	}

	@Inject(method = "onClientCommand", at = @At("HEAD"), cancellable = true)
	private void magic$onClientCommandHead(ClientCommandC2SPacket packet, CallbackInfo ci) {
		if (MagicAbilityManager.handleCelestialClientCommandPacket(player, packet)) {
			ci.cancel();
			return;
		}
		magic$packetDebug(
			"{} packet onClientCommand HEAD: mode={}",
			magic$debugName(),
			packet.getMode()
		);
		if (magic$isGreedIntroFrozen()) {
			ci.cancel();
			return;
		}
		if (packet.getMode() == ClientCommandC2SPacket.Mode.START_SPRINTING && GreedAbilityRuntime.isSprintBlocked(player)) {
			player.setSprinting(false);
			GreedAbilityRuntime.onBlockedSprintAttempt(player);
			ci.cancel();
			return;
		}
		if (packet.getMode() == ClientCommandC2SPacket.Mode.START_SPRINTING) {
			GreedAbilityRuntime.onStartSprinting(player);
		}
	}

	@Inject(method = "onClientCommand", at = @At("RETURN"))
	private void magic$onClientCommandReturn(ClientCommandC2SPacket packet, CallbackInfo ci) {
		magic$packetDebug(
			"{} packet onClientCommand RETURN: mode={}, sneaking={}, sprinting={}",
			magic$debugName(),
			packet.getMode(),
			player.isSneaking(),
			player.isSprinting()
		);
	}

	@Inject(method = "onPlayerAction", at = @At("HEAD"), cancellable = true)
	private void magic$onPlayerActionHead(PlayerActionC2SPacket packet, CallbackInfo ci) {
		if (MagicAbilityManager.handleCelestialPlayerActionPacket(player, packet)) {
			ci.cancel();
			return;
		}
		PlayerActionC2SPacket.Action action = packet.getAction();
		magic$packetDebug("{} packet onPlayerAction HEAD: action={}", magic$debugName(), action);
		if (magic$isGreedIntroFrozen()) {
			ci.cancel();
			return;
		}
		if (action == PlayerActionC2SPacket.Action.SWAP_ITEM_WITH_OFFHAND) {
			if (GreedAbilityRuntime.isOffhandBlocked(player)) {
				GreedAbilityRuntime.onBlockedShieldUse(player);
				ci.cancel();
				return;
			}
			GreedAbilityRuntime.recordExternalAction(player, "attribute_swapping");
		}
		if (
			(action == PlayerActionC2SPacket.Action.DROP_ITEM || action == PlayerActionC2SPacket.Action.DROP_ALL_ITEMS) &&
			MagicAbilityManager.isPlayerControlLocked(player)
		) {
			magic$packetDebug("{} packet onPlayerAction canceled: drop blocked due to control lock", magic$debugName());
			ci.cancel();
			return;
		}

	}

	@Inject(method = "onPlayerAction", at = @At("RETURN"))
	private void magic$onPlayerActionReturn(PlayerActionC2SPacket packet, CallbackInfo ci) {
		magic$packetDebug("{} packet onPlayerAction RETURN: action={}", magic$debugName(), packet.getAction());
	}

	@Inject(method = "onPlayerInteractBlock", at = @At("HEAD"), cancellable = true)
	private void magic$onPlayerInteractBlockHead(PlayerInteractBlockC2SPacket packet, CallbackInfo ci) {
		if (MagicAbilityManager.handleCelestialInteractBlockPacket(player, packet)) {
			ci.cancel();
			return;
		}
		if (magic$isGreedIntroFrozen()) {
			ci.cancel();
			return;
		}
		if (magic$shouldCancelGreedHandUse(packet.getHand())) {
			ci.cancel();
			return;
		}
		magic$packetDebug(
			"{} packet onPlayerInteractBlock HEAD: hand={}, hitPos={}",
			magic$debugName(),
			packet.getHand(),
			packet.getBlockHitResult().getPos()
		);
	}

	@Inject(method = "onPlayerInteractBlock", at = @At("RETURN"))
	private void magic$onPlayerInteractBlockReturn(PlayerInteractBlockC2SPacket packet, CallbackInfo ci) {
		magic$packetDebug("{} packet onPlayerInteractBlock RETURN: hand={}", magic$debugName(), packet.getHand());
	}

	@Inject(method = "onPlayerInteractItem", at = @At("HEAD"), cancellable = true)
	private void magic$onPlayerInteractItemHead(PlayerInteractItemC2SPacket packet, CallbackInfo ci) {
		if (MagicAbilityManager.handleCelestialInteractItemPacket(player, packet)) {
			ci.cancel();
			return;
		}
		if (magic$isGreedIntroFrozen()) {
			ci.cancel();
			return;
		}
		if (magic$shouldCancelGreedHandUse(packet.getHand())) {
			ci.cancel();
			return;
		}
		magic$packetDebug("{} packet onPlayerInteractItem HEAD: hand={}", magic$debugName(), packet.getHand());
	}

	@Inject(method = "onPlayerInteractItem", at = @At("RETURN"))
	private void magic$onPlayerInteractItemReturn(PlayerInteractItemC2SPacket packet, CallbackInfo ci) {
		magic$packetDebug("{} packet onPlayerInteractItem RETURN: hand={}", magic$debugName(), packet.getHand());
	}

	@Inject(method = "onPlayerInteractEntity", at = @At("HEAD"), cancellable = true)
	private void magic$onPlayerInteractEntityHead(PlayerInteractEntityC2SPacket packet, CallbackInfo ci) {
		if (MagicAbilityManager.handleCelestialInteractEntityPacket(player, packet)) {
			ci.cancel();
			return;
		}
		magic$packetDebug("{} packet onPlayerInteractEntity HEAD", magic$debugName());
		if (magic$isGreedIntroFrozen()) {
			ci.cancel();
			return;
		}
		boolean cancelManipulationAttack = MagicAbilityManager.shouldCancelManipulationEntityAttack(player, packet);
		if (!cancelManipulationAttack) {
			Hand hand = magic$interactionHand(packet);
			if (hand != null && magic$shouldCancelGreedHandUse(hand)) {
				ci.cancel();
				return;
			}
		}
		if (cancelManipulationAttack) {
			magic$packetDebug("{} packet onPlayerInteractEntity canceled by manipulation attack guard", magic$debugName());
			ci.cancel();
			return;
		}
		MagicAbilityManager.recordBurningPassionMeleeImpact(player, packet);
	}

	@Inject(method = "onPlayerInteractEntity", at = @At("RETURN"))
	private void magic$onPlayerInteractEntityReturn(PlayerInteractEntityC2SPacket packet, CallbackInfo ci) {
		magic$packetDebug("{} packet onPlayerInteractEntity RETURN", magic$debugName());
	}
}

