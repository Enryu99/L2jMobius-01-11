/*
 * This file is part of the L2J Mobius project.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.l2jmobius.gameserver.geoengine;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.List;
import java.util.logging.Logger;

import com.l2jmobius.Config;
import com.l2jmobius.gameserver.datatables.csv.DoorTable;
import com.l2jmobius.gameserver.datatables.xml.FenceData;
import com.l2jmobius.gameserver.geoengine.geodata.ABlock;
import com.l2jmobius.gameserver.geoengine.geodata.BlockComplex;
import com.l2jmobius.gameserver.geoengine.geodata.BlockFlat;
import com.l2jmobius.gameserver.geoengine.geodata.BlockMultilayer;
import com.l2jmobius.gameserver.geoengine.geodata.BlockNull;
import com.l2jmobius.gameserver.geoengine.geodata.GeoFormat;
import com.l2jmobius.gameserver.geoengine.geodata.GeoLocation;
import com.l2jmobius.gameserver.geoengine.geodata.GeoStructure;
import com.l2jmobius.gameserver.model.L2Object;
import com.l2jmobius.gameserver.model.L2World;
import com.l2jmobius.gameserver.model.actor.L2Character;
import com.l2jmobius.gameserver.model.actor.position.Location;
import com.l2jmobius.gameserver.util.MathUtil;

/**
 * @author Hasha
 */
public class GeoEngine
{
	protected static final Logger LOGGER = Logger.getLogger(GeoEngine.class.getName());
	
	private final ABlock[][] _blocks;
	private final BlockNull _nullBlock;
	
	/**
	 * Returns the instance of the {@link GeoEngine}.
	 * @return {@link GeoEngine} : The instance.
	 */
	public static GeoEngine getInstance()
	{
		return SingletonHolder._instance;
	}
	
	/**
	 * GeoEngine constructor. Loads all geodata files of chosen geodata format.
	 */
	public GeoEngine()
	{
		LOGGER.info("GeoEngine: Initializing...");
		
		// initialize block container
		_blocks = new ABlock[GeoStructure.GEO_BLOCKS_X][GeoStructure.GEO_BLOCKS_Y];
		
		// load null block
		_nullBlock = new BlockNull();
		
		// initialize multilayer temporarily buffer
		BlockMultilayer.initialize();
		
		// load geo files according to geoengine config setup
		int loaded = 0;
		for (int rx = L2World.TILE_X_MIN; rx <= L2World.TILE_X_MAX; rx++)
		{
			for (int ry = L2World.TILE_Y_MIN; ry <= L2World.TILE_Y_MAX; ry++)
			{
				final File f = new File(Config.GEODATA_PATH + String.format(GeoFormat.L2D.getFilename(), rx, ry));
				if (f.exists() && !f.isDirectory())
				{
					// region file is load-able, try to load it
					if (loadGeoBlocks(rx, ry))
					{
						loaded++;
					}
				}
				else
				{
					// region file is not load-able, load null blocks
					loadNullBlocks(rx, ry);
				}
			}
		}
		
		LOGGER.info("GeoEngine: Loaded " + loaded + " geodata files.");
		
		// avoid wrong configs when no files are loaded
		if (loaded == 0)
		{
			if (Config.PATHFINDING)
			{
				Config.PATHFINDING = false;
				LOGGER.info("GeoEngine: Forcing PathFinding setting to false.");
			}
			if (Config.COORD_SYNCHRONIZE == 2)
			{
				Config.COORD_SYNCHRONIZE = -1;
				LOGGER.info("GeoEngine: Forcing CoordSynchronize setting to -1.");
			}
		}
		
		// release multilayer block temporarily buffer
		BlockMultilayer.release();
	}
	
	/**
	 * Loads geodata from a file. When file does not exist, is corrupted or not consistent, loads none geodata.
	 * @param regionX : Geodata file region X coordinate.
	 * @param regionY : Geodata file region Y coordinate.
	 * @return boolean : True, when geodata file was loaded without problem.
	 */
	private final boolean loadGeoBlocks(int regionX, int regionY)
	{
		final String filename = String.format(GeoFormat.L2D.getFilename(), regionX, regionY);
		
		// standard load
		try (RandomAccessFile raf = new RandomAccessFile(Config.GEODATA_PATH + filename, "r");
			FileChannel fc = raf.getChannel())
		{
			// initialize file buffer
			MappedByteBuffer buffer = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size()).load();
			buffer.order(ByteOrder.LITTLE_ENDIAN);
			
			// get block indexes
			final int blockX = (regionX - L2World.TILE_X_MIN) * GeoStructure.REGION_BLOCKS_X;
			final int blockY = (regionY - L2World.TILE_Y_MIN) * GeoStructure.REGION_BLOCKS_Y;
			
			// loop over region blocks
			for (int ix = 0; ix < GeoStructure.REGION_BLOCKS_X; ix++)
			{
				for (int iy = 0; iy < GeoStructure.REGION_BLOCKS_Y; iy++)
				{
					// get block type
					final byte type = buffer.get();
					
					// load block according to block type
					switch (type)
					{
						case GeoStructure.TYPE_FLAT_L2D:
						{
							_blocks[blockX + ix][blockY + iy] = new BlockFlat(buffer, GeoFormat.L2D);
							break;
						}
						case GeoStructure.TYPE_COMPLEX_L2D:
						{
							_blocks[blockX + ix][blockY + iy] = new BlockComplex(buffer, GeoFormat.L2D);
							break;
						}
						case GeoStructure.TYPE_MULTILAYER_L2D:
						{
							_blocks[blockX + ix][blockY + iy] = new BlockMultilayer(buffer, GeoFormat.L2D);
							break;
						}
						default:
						{
							throw new IllegalArgumentException("Unknown block type: " + type);
						}
					}
				}
			}
			
			// check data consistency
			if (buffer.remaining() > 0)
			{
				LOGGER.warning("GeoEngine: Region file " + filename + " can be corrupted, remaining " + buffer.remaining() + " bytes to read.");
			}
			
			// loading was successful
			return true;
		}
		catch (Exception e)
		{
			// an error occured while loading, load null blocks
			LOGGER.warning("GeoEngine: Error while loading " + filename + " region file.");
			LOGGER.warning(e.getMessage());
			e.printStackTrace();
			
			// replace whole region file with null blocks
			loadNullBlocks(regionX, regionY);
			
			// loading was not successful
			return false;
		}
	}
	
	/**
	 * Loads null blocks. Used when no region file is detected or an error occurs during loading.
	 * @param regionX : Geodata file region X coordinate.
	 * @param regionY : Geodata file region Y coordinate.
	 */
	private final void loadNullBlocks(int regionX, int regionY)
	{
		// get block indexes
		final int blockX = (regionX - L2World.TILE_X_MIN) * GeoStructure.REGION_BLOCKS_X;
		final int blockY = (regionY - L2World.TILE_Y_MIN) * GeoStructure.REGION_BLOCKS_Y;
		
		// load all null blocks
		for (int ix = 0; ix < GeoStructure.REGION_BLOCKS_X; ix++)
		{
			for (int iy = 0; iy < GeoStructure.REGION_BLOCKS_Y; iy++)
			{
				_blocks[blockX + ix][blockY + iy] = _nullBlock;
			}
		}
	}
	
	// GEODATA - GENERAL
	
	/**
	 * Converts world X to geodata X.
	 * @param worldX
	 * @return int : Geo X
	 */
	public static int getGeoX(int worldX)
	{
		return (MathUtil.limit(worldX, L2World.MAP_MIN_X, L2World.MAP_MAX_X) - L2World.MAP_MIN_X) >> 4;
	}
	
	/**
	 * Converts world Y to geodata Y.
	 * @param worldY
	 * @return int : Geo Y
	 */
	public static int getGeoY(int worldY)
	{
		return (MathUtil.limit(worldY, L2World.MAP_MIN_Y, L2World.MAP_MAX_Y) - L2World.MAP_MIN_Y) >> 4;
	}
	
	/**
	 * Converts geodata X to world X.
	 * @param geoX
	 * @return int : World X
	 */
	public static int getWorldX(int geoX)
	{
		return (MathUtil.limit(geoX, 0, GeoStructure.GEO_CELLS_X) << 4) + L2World.MAP_MIN_X + 8;
	}
	
	/**
	 * Converts geodata Y to world Y.
	 * @param geoY
	 * @return int : World Y
	 */
	public static int getWorldY(int geoY)
	{
		return (MathUtil.limit(geoY, 0, GeoStructure.GEO_CELLS_Y) << 4) + L2World.MAP_MIN_Y + 8;
	}
	
	/**
	 * Returns block of geodata on given coordinates.
	 * @param geoX : Geodata X
	 * @param geoY : Geodata Y
	 * @return {@link ABlock} : Block of geodata.
	 */
	private final ABlock getBlock(int geoX, int geoY)
	{
		final int x = geoX / GeoStructure.BLOCK_CELLS_X;
		final int y = geoY / GeoStructure.BLOCK_CELLS_Y;
		
		// if x or y is out of array return null
		if ((x < GeoStructure.GEO_BLOCKS_X) && (y < GeoStructure.GEO_BLOCKS_Y))
		{
			return _blocks[x][y];
		}
		return null;
	}
	
	/**
	 * Check if geo coordinates has geo.
	 * @param geoX : Geodata X
	 * @param geoY : Geodata Y
	 * @return boolean : True, if given geo coordinates have geodata
	 */
	public final boolean hasGeoPos(int geoX, int geoY)
	{
		final ABlock block = getBlock(geoX, geoY);
		if (block == null) // null block check
		{
			// TODO: Find when this can be null. (Bad geodata? Check L2World getRegion method.)
			// LOGGER.warning("Could not find geodata block at " + getWorldX(geoX) + ", " + getWorldY(geoY) + ".");
			return false;
		}
		return block.hasGeoPos();
	}
	
	/**
	 * Returns the height of cell, which is closest to given coordinates.
	 * @param geoX : Cell geodata X coordinate.
	 * @param geoY : Cell geodata Y coordinate.
	 * @param worldZ : Cell world Z coordinate.
	 * @return short : Cell geodata Z coordinate, closest to given coordinates.
	 */
	public final short getHeightNearest(int geoX, int geoY, int worldZ)
	{
		final ABlock block = getBlock(geoX, geoY);
		return block != null ? block.getHeightNearest(geoX, geoY, worldZ) : (short) worldZ;
	}
	
	/**
	 * Returns the NSWE flag byte of cell, which is closes to given coordinates.
	 * @param geoX : Cell geodata X coordinate.
	 * @param geoY : Cell geodata Y coordinate.
	 * @param worldZ : Cell world Z coordinate.
	 * @return short : Cell NSWE flag byte coordinate, closest to given coordinates.
	 */
	public final byte getNsweNearest(int geoX, int geoY, int worldZ)
	{
		final ABlock block = getBlock(geoX, geoY);
		return block != null ? block.getNsweNearest(geoX, geoY, worldZ) : (byte) 0xFF;
	}
	
	/**
	 * Check if world coordinates has geo.
	 * @param worldX : World X
	 * @param worldY : World Y
	 * @return boolean : True, if given world coordinates have geodata
	 */
	public final boolean hasGeo(int worldX, int worldY)
	{
		return hasGeoPos(getGeoX(worldX), getGeoY(worldY));
	}
	
	/**
	 * Returns closest Z coordinate according to geodata.
	 * @param worldX : world x
	 * @param worldY : world y
	 * @param worldZ : world z
	 * @return short : nearest Z coordinates according to geodata
	 */
	public final short getHeight(int worldX, int worldY, int worldZ)
	{
		return getHeightNearest(getGeoX(worldX), getGeoY(worldY), worldZ);
	}
	
	// PATHFINDING
	
	/**
	 * Check line of sight from {@link L2Object} to {@link L2Object}.
	 * @param origin : The origin object.
	 * @param target : The target object.
	 * @return {@code boolean} : True if origin can see target
	 */
	public final boolean canSeeTarget(L2Object origin, L2Object target)
	{
		if (target.isDoor())
		{
			return true;
		}
		
		// get origin and target world coordinates
		final int ox = origin.getX();
		final int oy = origin.getY();
		final int oz = origin.getZ();
		final int tx = target.getX();
		final int ty = target.getY();
		final int tz = target.getZ();
		
		if (DoorTable.getInstance().checkIfDoorsBetween(ox, oy, oz, tx, ty, tz))
		{
			return false;
		}
		if (FenceData.getInstance().checkIfFenceBetween(ox, oy, oz, tx, ty, tz))
		{
			return false;
		}
		
		// get origin and check existing geo coordinates
		final int gox = getGeoX(ox);
		final int goy = getGeoY(oy);
		if (!hasGeoPos(gox, goy))
		{
			return true;
		}
		
		final short goz = getHeightNearest(gox, goy, oz);
		
		// get target and check existing geo coordinates
		final int gtx = getGeoX(tx);
		final int gty = getGeoY(ty);
		if (!hasGeoPos(gtx, gty))
		{
			return true;
		}
		
		final short gtz = getHeightNearest(gtx, gty, tz);
		
		// origin and target coordinates are same
		if ((gox == gtx) && (goy == gty))
		{
			return goz == gtz;
		}
		
		// get origin and target height, real height = collision height * 2
		double oheight = 0;
		if (origin.isCharacter())
		{
			oheight = ((L2Character) origin).getTemplate().collisionHeight * 2;
		}
		
		double theight = 0;
		if (target.isCharacter())
		{
			theight = ((L2Character) target).getTemplate().collisionHeight * 2;
		}
		
		// perform geodata check
		return checkSee(gox, goy, goz, oheight, gtx, gty, gtz, theight, origin.getInstanceId());
	}
	
	/**
	 * Check line of sight from {@link L2Object} to {@link Location}.
	 * @param origin : The origin object.
	 * @param position : The target position.
	 * @return {@code boolean} : True if object can see position
	 */
	public final boolean canSeeTarget(L2Object origin, Location position)
	{
		// get origin and target world coordinates
		final int ox = origin.getX();
		final int oy = origin.getY();
		final int oz = origin.getZ();
		final int tx = position.getX();
		final int ty = position.getY();
		final int tz = position.getZ();
		
		if (DoorTable.getInstance().checkIfDoorsBetween(ox, oy, oz, tx, ty, tz))
		{
			return false;
		}
		if (FenceData.getInstance().checkIfFenceBetween(ox, oy, oz, tx, ty, tz))
		{
			return false;
		}
		
		// get origin and check existing geo coordinates
		final int gox = getGeoX(ox);
		final int goy = getGeoY(oy);
		if (!hasGeoPos(gox, goy))
		{
			return true;
		}
		
		final short goz = getHeightNearest(gox, goy, oz);
		
		// get target and check existing geo coordinates
		final int gtx = getGeoX(tx);
		final int gty = getGeoY(ty);
		if (!hasGeoPos(gtx, gty))
		{
			return true;
		}
		
		final short gtz = getHeightNearest(gtx, gty, tz);
		
		// origin and target coordinates are same
		if ((gox == gtx) && (goy == gty))
		{
			return goz == gtz;
		}
		
		// get origin and target height, real height = collision height * 2
		double oheight = 0;
		if (origin.isCharacter())
		{
			oheight = ((L2Character) origin).getTemplate().collisionHeight;
		}
		
		// perform geodata check
		return checkSee(gox, goy, goz, oheight, gtx, gty, gtz, 0, origin.getInstanceId());
	}
	
	/**
	 * Simple check for origin to target visibility.
	 * @param gox : origin X geodata coordinate
	 * @param goy : origin Y geodata coordinate
	 * @param goz : origin Z geodata coordinate
	 * @param oheight : origin height (if instance of {@link Character})
	 * @param gtx : target X geodata coordinate
	 * @param gty : target Y geodata coordinate
	 * @param gtz : target Z geodata coordinate
	 * @param theight : target height (if instance of {@link Character})
	 * @param instanceId
	 * @return {@code boolean} : True, when target can be seen.
	 */
	private final boolean checkSee(int gox, int goy, int goz, double oheight, int gtx, int gty, int gtz, double theight, int instanceId)
	{
		// get line of sight Z coordinates
		double losoz = goz + ((oheight * Config.PART_OF_CHARACTER_HEIGHT) / 100);
		double lostz = gtz + ((theight * Config.PART_OF_CHARACTER_HEIGHT) / 100);
		
		// get X delta and signum
		final int dx = Math.abs(gtx - gox);
		final int sx = gox < gtx ? 1 : -1;
		final byte dirox = sx > 0 ? GeoStructure.CELL_FLAG_E : GeoStructure.CELL_FLAG_W;
		final byte dirtx = sx > 0 ? GeoStructure.CELL_FLAG_W : GeoStructure.CELL_FLAG_E;
		
		// get Y delta and signum
		final int dy = Math.abs(gty - goy);
		final int sy = goy < gty ? 1 : -1;
		final byte diroy = sy > 0 ? GeoStructure.CELL_FLAG_S : GeoStructure.CELL_FLAG_N;
		final byte dirty = sy > 0 ? GeoStructure.CELL_FLAG_N : GeoStructure.CELL_FLAG_S;
		
		// get Z delta
		final int dm = Math.max(dx, dy);
		final double dz = (lostz - losoz) / dm;
		
		// get direction flag for diagonal movement
		final byte diroxy = getDirXY(dirox, diroy);
		final byte dirtxy = getDirXY(dirtx, dirty);
		
		// delta, determines axis to move on (+..X axis, -..Y axis)
		int d = dx - dy;
		
		// NSWE direction of movement
		byte diro;
		byte dirt;
		
		// clearDebugItems();
		// dropDebugItem(728, 0, new GeoLocation(gox, goy, goz)); // blue potion
		// dropDebugItem(728, 0, new GeoLocation(gtx, gty, gtz)); // blue potion
		
		// initialize node values
		int nox = gox;
		int noy = goy;
		int ntx = gtx;
		int nty = gty;
		byte nsweo = getNsweNearest(gox, goy, goz);
		byte nswet = getNsweNearest(gtx, gty, gtz);
		
		// loop
		ABlock block;
		int index;
		for (int i = 0; i < ((dm + 1) / 2); i++)
		{
			// dropDebugItem(57, 0, new GeoLocation(gox, goy, goz)); // antidote
			// dropDebugItem(1831, 0, new GeoLocation(gtx, gty, gtz)); // adena
			
			// reset direction flag
			diro = 0;
			dirt = 0;
			
			// calculate next point coordinates
			int e2 = 2 * d;
			if ((e2 > -dy) && (e2 < dx))
			{
				// calculate next point XY coordinates
				d -= dy;
				d += dx;
				nox += sx;
				ntx -= sx;
				noy += sy;
				nty -= sy;
				diro |= diroxy;
				dirt |= dirtxy;
			}
			else if (e2 > -dy)
			{
				// calculate next point X coordinate
				d -= dy;
				nox += sx;
				ntx -= sx;
				diro |= dirox;
				dirt |= dirtx;
			}
			else if (e2 < dx)
			{
				// calculate next point Y coordinate
				d += dx;
				noy += sy;
				nty -= sy;
				diro |= diroy;
				dirt |= dirty;
			}
			
			{
				// get block of the next cell
				block = getBlock(nox, noy);
				
				// get index of particular layer, based on movement conditions
				if ((nsweo & diro) == 0)
				{
					index = block.getIndexAbove(nox, noy, goz - GeoStructure.CELL_IGNORE_HEIGHT);
				}
				else
				{
					index = block.getIndexBelow(nox, noy, goz + GeoStructure.CELL_IGNORE_HEIGHT);
				}
				
				// layer does not exist, return
				if (index == -1)
				{
					return false;
				}
				
				// get layer and next line of sight Z coordinate
				goz = block.getHeight(index);
				losoz += dz;
				
				// perform line of sight check, return when fails
				if ((goz - losoz) > Config.MAX_OBSTACLE_HEIGHT)
				{
					return false;
				}
				
				// get layer nswe
				nsweo = block.getNswe(index);
			}
			{
				// get block of the next cell
				block = getBlock(ntx, nty);
				
				// get index of particular layer, based on movement conditions
				if ((nswet & dirt) == 0)
				{
					index = block.getIndexAbove(ntx, nty, gtz - GeoStructure.CELL_IGNORE_HEIGHT);
				}
				else
				{
					index = block.getIndexBelow(ntx, nty, gtz + GeoStructure.CELL_IGNORE_HEIGHT);
				}
				
				// layer does not exist, return
				if (index == -1)
				{
					return false;
				}
				
				// get layer and next line of sight Z coordinate
				gtz = block.getHeight(index);
				lostz -= dz;
				
				// perform line of sight check, return when fails
				if ((gtz - lostz) > Config.MAX_OBSTACLE_HEIGHT)
				{
					return false;
				}
				
				// get layer nswe
				nswet = block.getNswe(index);
			}
			
			// update coords
			gox = nox;
			goy = noy;
			gtx = ntx;
			gty = nty;
		}
		
		// when iteration is completed, compare final Z coordinates
		return Math.abs(goz - gtz) < (GeoStructure.CELL_HEIGHT * 4);
	}
	
	/**
	 * Check movement from coordinates to coordinates.
	 * @param ox : origin X coordinate
	 * @param oy : origin Y coordinate
	 * @param oz : origin Z coordinate
	 * @param tx : target X coordinate
	 * @param ty : target Y coordinate
	 * @param tz : target Z coordinate
	 * @param instanceId
	 * @return {code boolean} : True if target coordinates are reachable from origin coordinates
	 */
	public final boolean canMoveToTarget(int ox, int oy, int oz, int tx, int ty, int tz, int instanceId)
	{
		// get origin and check existing geo coordinates
		final int gox = getGeoX(ox);
		final int goy = getGeoY(oy);
		if (!hasGeoPos(gox, goy))
		{
			return true;
		}
		
		final short goz = getHeightNearest(gox, goy, oz);
		
		// get target and check existing geo coordinates
		final int gtx = getGeoX(tx);
		final int gty = getGeoY(ty);
		if (!hasGeoPos(gtx, gty))
		{
			return true;
		}
		
		final short gtz = getHeightNearest(gtx, gty, tz);
		
		// target coordinates reached
		if ((gox == gtx) && (goy == gty) && (goz == gtz))
		{
			return true;
		}
		
		// perform geodata check
		GeoLocation loc = checkMove(gox, goy, goz, gtx, gty, gtz, instanceId);
		return (loc.getGeoX() == gtx) && (loc.getGeoY() == gty);
	}
	
	/**
	 * Check movement from origin to target. Returns last available point in the checked path.
	 * @param ox : origin X coordinate
	 * @param oy : origin Y coordinate
	 * @param oz : origin Z coordinate
	 * @param tx : target X coordinate
	 * @param ty : target Y coordinate
	 * @param tz : target Z coordinate
	 * @param instanceId
	 * @return {@link Location} : Last point where object can walk (just before wall)
	 */
	public final Location canMoveToTargetLoc(int ox, int oy, int oz, int tx, int ty, int tz, int instanceId)
	{
		// Mobius: Double check for doors before normal checkMove to avoid exploiting key movement.
		if (DoorTable.getInstance().checkIfDoorsBetween(ox, oy, oz, tx, ty, tz))
		{
			return new Location(ox, oy, oz);
		}
		if (FenceData.getInstance().checkIfFenceBetween(ox, oy, oz, tx, ty, tz))
		{
			return new Location(ox, oy, oz);
		}
		
		// get origin and check existing geo coordinates
		final int gox = getGeoX(ox);
		final int goy = getGeoY(oy);
		if (!hasGeoPos(gox, goy))
		{
			return new Location(tx, ty, tz);
		}
		
		final short goz = getHeightNearest(gox, goy, oz);
		
		// get target and check existing geo coordinates
		final int gtx = getGeoX(tx);
		final int gty = getGeoY(ty);
		if (!hasGeoPos(gtx, gty))
		{
			return new Location(tx, ty, tz);
		}
		
		final short gtz = getHeightNearest(gtx, gty, tz);
		
		// target coordinates reached
		if ((gox == gtx) && (goy == gty) && (goz == gtz))
		{
			return new Location(tx, ty, tz);
		}
		
		// perform geodata check
		return checkMove(gox, goy, goz, gtx, gty, gtz, instanceId);
	}
	
	/**
	 * With this method you can check if a position is visible or can be reached by beeline movement.<br>
	 * Target X and Y reachable and Z is on same floor:
	 * <ul>
	 * <li>Location of the target with corrected Z value from geodata.</li>
	 * </ul>
	 * Target X and Y reachable but Z is on another floor:
	 * <ul>
	 * <li>Location of the origin with corrected Z value from geodata.</li>
	 * </ul>
	 * Target X and Y not reachable:
	 * <ul>
	 * <li>Last accessible location in destination to target.</li>
	 * </ul>
	 * @param gox : origin X geodata coordinate
	 * @param goy : origin Y geodata coordinate
	 * @param goz : origin Z geodata coordinate
	 * @param gtx : target X geodata coordinate
	 * @param gty : target Y geodata coordinate
	 * @param gtz : target Z geodata coordinate
	 * @param instanceId
	 * @return {@link GeoLocation} : The last allowed point of movement.
	 */
	protected final GeoLocation checkMove(int gox, int goy, int goz, int gtx, int gty, int gtz, int instanceId)
	{
		if (DoorTable.getInstance().checkIfDoorsBetween(gox, goy, goz, gtx, gty, gtz))
		{
			return new GeoLocation(gox, goy, goz);
		}
		if (FenceData.getInstance().checkIfFenceBetween(gox, goy, goz, gtx, gty, gtz))
		{
			return new GeoLocation(gox, goy, goz);
		}
		
		// get X delta, signum and direction flag
		final int dx = Math.abs(gtx - gox);
		final int sx = gox < gtx ? 1 : -1;
		final byte dirX = sx > 0 ? GeoStructure.CELL_FLAG_E : GeoStructure.CELL_FLAG_W;
		
		// get Y delta, signum and direction flag
		final int dy = Math.abs(gty - goy);
		final int sy = goy < gty ? 1 : -1;
		final byte dirY = sy > 0 ? GeoStructure.CELL_FLAG_S : GeoStructure.CELL_FLAG_N;
		
		// get direction flag for diagonal movement
		final byte dirXY = getDirXY(dirX, dirY);
		
		// delta, determines axis to move on (+..X axis, -..Y axis)
		int d = dx - dy;
		
		// NSWE direction of movement
		byte direction;
		
		// load pointer coordinates
		int gpx = gox;
		int gpy = goy;
		int gpz = goz;
		
		// load next pointer
		int nx = gpx;
		int ny = gpy;
		
		// loop
		do
		{
			direction = 0;
			
			// calculate next point coordinates
			int e2 = 2 * d;
			if ((e2 > -dy) && (e2 < dx))
			{
				d -= dy;
				d += dx;
				nx += sx;
				ny += sy;
				direction |= dirXY;
			}
			else if (e2 > -dy)
			{
				d -= dy;
				nx += sx;
				direction |= dirX;
			}
			else if (e2 < dx)
			{
				d += dx;
				ny += sy;
				direction |= dirY;
			}
			
			// obstacle found, return
			if ((getNsweNearest(gpx, gpy, gpz) & direction) == 0)
			{
				return new GeoLocation(gpx, gpy, gpz);
			}
			
			// update pointer coordinates
			gpx = nx;
			gpy = ny;
			gpz = getHeightNearest(nx, ny, gpz);
			
			// target coordinates reached
			if ((gpx == gtx) && (gpy == gty))
			{
				if (gpz == gtz)
				{
					// path found, Z coordinates are okay, return target point
					return new GeoLocation(gtx, gty, gtz);
				}
				
				// path found, Z coordinates are not okay, return origin point
				return new GeoLocation(gox, goy, goz);
			}
		}
		while (true);
	}
	
	/**
	 * Returns diagonal NSWE flag format of combined two NSWE flags.
	 * @param dirX : X direction NSWE flag
	 * @param dirY : Y direction NSWE flag
	 * @return byte : NSWE flag of combined direction
	 */
	private static byte getDirXY(byte dirX, byte dirY)
	{
		// check axis directions
		if (dirY == GeoStructure.CELL_FLAG_N)
		{
			if (dirX == GeoStructure.CELL_FLAG_W)
			{
				return GeoStructure.CELL_FLAG_NW;
			}
			
			return GeoStructure.CELL_FLAG_NE;
		}
		
		if (dirX == GeoStructure.CELL_FLAG_W)
		{
			return GeoStructure.CELL_FLAG_SW;
		}
		
		return GeoStructure.CELL_FLAG_SE;
	}
	
	/**
	 * Returns the list of location objects as a result of complete path calculation.
	 * @param ox : origin x
	 * @param oy : origin y
	 * @param oz : origin z
	 * @param tx : target x
	 * @param ty : target y
	 * @param tz : target z
	 * @param instanceId
	 * @return {@code List<Location>} : complete path from nodes
	 */
	public List<Location> findPath(int ox, int oy, int oz, int tx, int ty, int tz, int instanceId)
	{
		return null;
	}
	
	private static class SingletonHolder
	{
		protected static final GeoEngine _instance = Config.PATHFINDING ? new GeoEnginePathfinding() : new GeoEngine();
	}
}