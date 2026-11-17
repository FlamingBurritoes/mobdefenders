package net.amemah.mobdefenders.block;

import com.mojang.serialization.MapCodec;
import net.amemah.mobdefenders.menu.AllySpawnerMenu;
import net.amemah.mobdefenders.spawner.AllySpawnerData;
import net.amemah.mobdefenders.spawner.SpawnerKind;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;

public final class AllySpawnerBlock extends Block {
	public static final MapCodec<AllySpawnerBlock> CODEC = simpleCodec(AllySpawnerBlock::new);
	public AllySpawnerBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH));
	}
	@Override protected MapCodec<? extends Block> codec() { return CODEC; }
	@Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(BlockStateProperties.HORIZONTAL_FACING);
	}
	@Override public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, context.getHorizontalDirection());
	}
	@Override protected BlockState rotate(BlockState state, Rotation rotation) {
		return state.setValue(BlockStateProperties.HORIZONTAL_FACING, rotation.rotate(state.getValue(BlockStateProperties.HORIZONTAL_FACING)));
	}
	@Override protected BlockState mirror(BlockState state, Mirror mirror) {
		return state.rotate(mirror.getRotation(state.getValue(BlockStateProperties.HORIZONTAL_FACING)));
	}
	@Override protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moved) {
		super.onPlace(state, level, pos, oldState, moved);
		if (level instanceof ServerLevel server && !oldState.is(this)) {
			var data = AllySpawnerData.get(server);
			var previous = data.source(pos);
			SpawnerKind kind = SpawnerKind.forBlock(state);
			if (previous != null && previous.kind != kind) data.unregister(server, pos);
			data.register(pos, server.getGameTime(), kind);
		}
	}
	@Override protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean moved) {
		super.affectNeighborsAfterRemoval(state, level, pos, moved);
		var data = AllySpawnerData.get(level);
		var source = data.source(pos);
		if (!level.getBlockState(pos).is(this) && source != null && source.kind.block() == this)
			data.unregister(level, pos);
	}
	@Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (level instanceof ServerLevel) {
			player.openMenu(new SimpleMenuProvider((id, inventory, user) ->
				new AllySpawnerMenu(id, inventory, ContainerLevelAccess.create(level, pos)),
				getName()));
		}
		return InteractionResult.SUCCESS;
	}
}
