package net.amemah.mobdefenders;

import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

final class GameTestPlayers {
	static FakePlayer spawnSurvival(ServerLevel level, Vec3 position) {
		var player = new FakePlayer(level, new GameProfile(UUID.randomUUID(), "GameTestPlayer")) {
			// Fabric's default fake player is immune to damage, which would mask blast regressions.
			@Override public boolean isInvulnerableTo(ServerLevel level, DamageSource damage) { return false; }
		};
		player.setGameMode(GameType.SURVIVAL);
		player.snapTo(position, 0, 0);
		level.addNewPlayer(player);
		return player;
	}

	private GameTestPlayers() { }
}
