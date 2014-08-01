package com.axnsan.airplanes;

import java.util.ArrayList;

import com.axnsan.airplanes.util.Point2D;
import com.badlogic.gdx.scenes.scene2d.Actor;

public class BaseGrid extends Actor {
	protected final static byte MISS = -1, BODY = -2, HEAD = -3, SILENT_HIT = -4, NONE = 0; 
	private short[][] grid;
	protected int size;
	protected ArrayList<Plane.Location> planeLocations = new ArrayList<Plane.Location>();
	private short detachedPlane;
	
	public BaseGrid(int size)
	{
		grid = new short[size][size];
		for (int i = 0;i < size;++i)
			for (int j = 0;j < size;++j)
				grid[i][j] = 0;
		
		this.size = size;
		detachedPlane = -1;
	}
	
	/**
	 * Add a plane to the grid.
	 * @param x, y location to add the plane at.
	 * @param model the plane's grid model
	 * @return the ID of the plane that was added, or -1 if the add was not successful
	 */
	public short addPlane(int x, int y, Plane.Model model)
	{
		if (detachedPlane >= 0)
			throw new RuntimeException("Attempting to add new plane while a plane is detached. Forgot to reattach?");
		
		if (!fits(model, x, y))
			return -1;
		
		planeLocations.add(new Plane.Location(x, y, model));
		for (int i = 0; i < model.width; ++i)
			for (int j = 0;j < model.height; ++j)
				if (model.body[i][j])
					grid[x+i][y-j] = (short) planeLocations.size();
		
		return (short) planeLocations.size();
	}
	
	/**Removes the space occupied by the given plane from the grid.<br>
	 * Useful for relocating the plane.
	 * Only one plane can be detached at a time.**/
	public void detachPlane(short ID) {
		if (ID < 0)
			throw new IllegalArgumentException();
		
		if (detachedPlane >= 0) {
			throw new RuntimeException("Attempting to detach plane while another plane is already detached. Forgot to reattach?");
		}
		
		for (int i = 0;i < grid.length;++i)
			for (int j = 0;j < grid.length;++j)
				if (grid[i][j] == ID)
					grid[i][j] = 0;

		detachedPlane = ID;
	}
	
	/**Re-attach the detached plane at the given location.
	 * @return success**/
	public boolean attachPlane(int x, int y) {
		if (detachedPlane < 0) {
			throw new RuntimeException("Attempting to attach plane while no plane is detached. Perhaps trying to add a new plane?");
		}
		
		Plane.Model model = planeLocations.get(detachedPlane-1).model;
		if (!fits(model, x, y))
			return false;

		planeLocations.get(detachedPlane-1).x = x;
		planeLocations.get(detachedPlane-1).y = y;
		for (int i = 0; i < model.width; ++i)
			for (int j = 0;j < model.height; ++j)
				if (model.body[i][j])
					grid[x+i][y-j] = detachedPlane;
		
		detachedPlane = -1;
		return true;
	}
	
	/**Attempt to re-attach the detached plane at the given location and change its model.
	 * @return success**/
	public boolean attachPlane(int x, int y, Plane.Model model) {
		if (detachedPlane < 0) {
			throw new RuntimeException("Attempting to attach plane while no plane is detached. Perhaps trying to add a new plane?");
		}
		
		//Plane.GridModel model = planes.get(detachedPlane-1).model;
		if (!fits(model, x, y))
			return false;

		planeLocations.get(detachedPlane-1).x = x;
		planeLocations.get(detachedPlane-1).y = y;
		planeLocations.get(detachedPlane-1).model = model;
		planeLocations.get(detachedPlane-1).orientation = model.orientation;
		for (int i = 0; i < model.width; ++i)
			for (int j = 0;j < model.height; ++j)
				if (model.body[i][j])
					grid[x+i][y-j] = detachedPlane;
		
		
		detachedPlane = -1;
		return true;
	}
	
	/**Attempt to re-attach the detached plane at the location it was, but with a different model
	 * @return success**/
	public boolean reattachPlane(Plane.Model model) {
		if (detachedPlane < 0)
			throw new RuntimeException("Attempting to reattach while no plane is detached.");
		
		if (!attachPlane(planeLocations.get(detachedPlane-1).x, planeLocations.get(detachedPlane-1).y, model))
			return false;
		
		return true;
	}
	
	/**Put the detached plane back. Should never fail. Big problem if it does**/
	public void reattachPlane() {
		if (detachedPlane < 0)
			throw new RuntimeException("Attempting to reattach while no plane is detached.");
		
		if (!attachPlane(planeLocations.get(detachedPlane-1).x, planeLocations.get(detachedPlane-1).y))
			throw new RuntimeException("Failed to reattach. How the fuck did this happen?");
	}
	
	public void clear() {
		planeLocations.clear();
		detachedPlane = -1;
		for (int i = 0;i < size;++i)
			for (int j = 0;j < size;++j)
				grid[i][j] = 0;
	}
	
	protected byte cellType(int x, int y) {
		if (grid[x][y] > 0) {
			Plane.Location loc = planeLocations.get(grid[x][y]-1);
			Plane.Model model = loc.model;
			if (loc.x + model.headX == x && loc.y - model.headY == y) {
				return HEAD;
			}
			else return BODY;
		}
		return MISS;
	}
	
	protected short planeAt(int x, int y) {
		return grid[x][y];
	}
	
	protected void removePlaneAt(int x, int y) {
		detachPlane(grid[x][y]);
		detachedPlane = -1;
	}
	
	public Point2D planeLocation(short ID) {
		if (ID < 0)
			throw new IllegalArgumentException();
		
		return new Point2D(planeLocations.get(ID-1).x, planeLocations.get(ID-1).y);
	}
	
	public ArrayList<Plane.Location> dumpPlaneLocations() {
		ArrayList<Plane.Location> ret = new ArrayList<Plane.Location>();
		for (Plane.Location loc : planeLocations)
			ret.add(new Plane.Location(loc.x, loc.y, loc.orientation));
		
		return ret;
	}
	
	protected boolean fits(Plane.Model model, int x, int y)
	{
		if (x + model.width - 1 >= grid.length || y - model.height + 1 < 0 || x < 0 || y >= grid.length
			|| model.headX < 0 || model.headX >= model.width || model.headY < 0 || model.headY >= model.height)
			return false;
		
		for (int i = 0; i < model.width; ++i)
			for (int j = 0;j < model.height; ++j)
				if (model.body[i][j])
					if (grid[x+i][y-j] != 0)
						return false;
		
		return true;
	}
}
