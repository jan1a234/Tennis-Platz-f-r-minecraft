package de.jan.tennis.court;

import de.jan.tennis.logic.Surface;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Baut einen Tennisplatz in Originalproportionen: Spielfeld 25 × 13 Blöcke mit Einzel- und Doppellinien,
 * Netz, Auslauf, Zaun und Flutlicht.
 */
public final class CourtBuilder {
	private static final int FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_SUPPRESS_DROPS;
	private static final int HALF_LENGTH = 12;
	private static final int HALF_WIDTH_DOUBLES = 6;
	private static final int HALF_WIDTH_SINGLES = 4;
	private static final int SERVICE = 7;
	private static final int AREA_LENGTH = 18;
	private static final int AREA_WIDTH = 10;
	private static final int CLEAR_HEIGHT = 12;

	private CourtBuilder() {
	}

	public static Court build(ServerLevel level, BlockPos center, boolean alongX, Surface surface, String name) {
		int groundY = center.getY();
		List<BlockPos> shaped = new ArrayList<>();
		BlockState line = Blocks.CONCRETE.white().defaultBlockState();
		BlockState court = switch (surface) {
			case RASEN -> Blocks.GRASS_BLOCK.defaultBlockState();
			case SAND -> Blocks.TERRACOTTA.defaultBlockState();
			case HARTPLATZ -> Blocks.CONCRETE.blue().defaultBlockState();
		};
		BlockState runoff = switch (surface) {
			case RASEN -> Blocks.MOSS_BLOCK.defaultBlockState();
			case SAND -> Blocks.TERRACOTTA.defaultBlockState();
			case HARTPLATZ -> Blocks.CONCRETE.green().defaultBlockState();
		};
		BlockState fence = Blocks.IRON_BARS.defaultBlockState();

		for (int a = -AREA_LENGTH - 1; a <= AREA_LENGTH + 1; a++) {
			for (int b = -AREA_WIDTH - 1; b <= AREA_WIDTH + 1; b++) {
				boolean border = Math.abs(a) == AREA_LENGTH + 1 || Math.abs(b) == AREA_WIDTH + 1;
				BlockPos floor = offset(center, a, b, alongX).below();
				BlockState floorState;
				if (isLine(a, b)) {
					floorState = line;
				} else if (Math.abs(a) <= HALF_LENGTH && Math.abs(b) <= HALF_WIDTH_DOUBLES) {
					floorState = court;
				} else {
					floorState = runoff;
				}
				level.setBlock(floor, floorState, FLAGS);
				for (int h = 0; h < CLEAR_HEIGHT; h++) {
					BlockPos pos = floor.above(1 + h);
					BlockState state = Blocks.AIR.defaultBlockState();
					if (border) {
						boolean gate = Math.abs(b) == AREA_WIDTH + 1 && Math.abs(a) >= 1 && Math.abs(a) <= 2;
						boolean corner = Math.abs(a) == AREA_LENGTH + 1 && Math.abs(b) == AREA_WIDTH + 1;
						if (h < 3 && !gate) {
							state = fence;
						} else if (h == 3 && corner) {
							state = Blocks.SEA_LANTERN.defaultBlockState();
						}
					} else if (a == 0 && h == 0) {
						if (Math.abs(b) <= HALF_WIDTH_DOUBLES + 1) {
							state = fence;
						} else if (Math.abs(b) == HALF_WIDTH_DOUBLES + 2) {
							state = Blocks.DARK_OAK_FENCE.defaultBlockState();
						}
					}
					level.setBlock(pos, state, FLAGS);
					if (!state.isAir()) {
						shaped.add(pos);
					}
				}
			}
		}
		// Gitter und Pfosten miteinander verbinden
		for (BlockPos pos : shaped) {
			BlockState state = level.getBlockState(pos);
			level.setBlock(pos, Block.updateFromNeighbourShapes(state, level, pos), FLAGS);
		}
		String dimension = level.dimension().identifier().toString();
		return new Court(name, dimension, center.getX() + 0.5, groundY, center.getZ() + 0.5, alongX, surface);
	}

	private static boolean isLine(int a, int b) {
		int ua = Math.abs(a);
		int ub = Math.abs(b);
		if (ua == HALF_LENGTH && ub <= HALF_WIDTH_DOUBLES) {
			return true; // Grundlinie
		}
		if ((ub == HALF_WIDTH_DOUBLES || ub == HALF_WIDTH_SINGLES) && ua <= HALF_LENGTH) {
			return true; // Seitenlinien
		}
		if (ua == SERVICE && ub <= HALF_WIDTH_SINGLES) {
			return true; // T-Linie
		}
		if (b == 0 && ua >= 1 && ua <= SERVICE) {
			return true; // Mittellinie
		}
		return b == 0 && ua == HALF_LENGTH - 1; // Mittelmarkierung
	}

	private static BlockPos offset(BlockPos center, int a, int b, boolean alongX) {
		return alongX ? center.offset(a, 0, b) : center.offset(-b, 0, a);
	}
}
