package com.axnsan.airplanes;

import java.util.ArrayList;
import java.util.Iterator;

import com.axnsan.airplanes.util.Point2D;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL10;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;

public class Grid extends BaseGrid {
	protected int cell, border;
	protected ArrayList<Plane> planes = new ArrayList<Plane>();
	protected ShapeRenderer shape;
	private boolean hidePlanes = false;
	protected boolean hidden = false;
	private Matrix4 transform = new Matrix4();
	private String label;
	private static int gridID = 0;
	
	/**The smallest size, in pixels, a cell can have while still being discernable*/
	public static final int MIN_RESOLUTION = 10;
	
	/**A dp is defined as the width of a pixel on an 160 dpi screen, 0.15mm */
	public static final int MIN_CELLSIZE_DP = 20;
	
	public static int MIN_CELLSIZE_PX() {
		int ret = (int) (Grid.MIN_CELLSIZE_DP * Gdx.graphics.getDensity());
		ret = Math.max(ret, Grid.MIN_RESOLUTION);
		return ret;
	}
	
	public Grid(int cellSize, int borderWidth, int gridSize)
	{
		super(gridSize);
		border = borderWidth;
		setOrigin(0, 0);
		resize();
		setLabel("grid_" + gridID);
		++gridID;
	}
	
	protected boolean addPlane(Plane plane, int gridX, int gridY) {
		short id;
		if ((id = super.addPlane(gridX, gridY, plane.model())) < 0) {
			return false;
		}

		plane.setID(id);
		planes.add(plane);
		plane.setHidden(hidePlanes);
		Vector2 pos = gridToSceneCoords(gridX, gridY);
		plane.setPosition(pos.x, pos.y);
		return true;
	}
	
	/**Only intended to be called once, on an empty grid.
	 * Will empty the grid when called.
	 * @return success value*/
	
	public boolean addPlanes(ArrayList<Plane.Location> locations) {
		this.clear();
		Iterator<String> colors = Airplanes.colorsList.iterator();
		for (Plane.Location loc : locations) {
			if (!colors.hasNext())
				colors = Airplanes.colorsList.iterator();
			Plane p = new Plane(cell, border, loc.orientation, colors.next());
			if (!addPlane(p, loc.x, loc.y))
				return false;
		}
		return true;
	}
	
	public void randomize() {
		this.clear();
		System.gc();
		randomizeLocations();
		buildPlanes(planeLocations);
	}
	
	private void buildPlanes(ArrayList<Plane.Location> locations) {
		super.planeLocations = locations;
		Iterator<String> colors = Airplanes.colorsList.iterator();
		short id = 0;
		for (Plane.Location loc : planeLocations) {
			if (!colors.hasNext())
				colors = Airplanes.colorsList.iterator();
			Plane p = new Plane(cell, border, loc.model.orientation, colors.next());
			p.setID(++id);
			p.setHidden(hidePlanes);
			planes.add(p);
			Vector2 pos = gridToSceneCoords(loc.x, loc.y);
			p.setPosition(pos.x, pos.y);
		}
	}
	
	private void randomizeLocations() {
		this.clear();

		Plane.Model[] p = new Plane.Model[4];
		p[0] = Plane.calculateGridModel(Plane.Orientation.UP);
		p[1] = Plane.calculateGridModel(Plane.Orientation.LEFT);
		p[2] = Plane.calculateGridModel(Plane.Orientation.DOWN);
		p[3] = Plane.calculateGridModel(Plane.Orientation.RIGHT);
		try_placement: while (planeLocations.size() < Airplanes.game.config.numPlanes) 
		{
			int x, y, o;
			
			//Try 500 random positions for another plane
			int attempts = 0;
			do {
				x = (int) (Math.random() * size);
				y = (int) (Math.random() * size);
				o = (int) (Math.random() * 4);
				
				attempts++;
				if (super.addPlane(x, y, p[o]) > 0) {
					continue try_placement;
				}
			} while (attempts <= 500);
			
			/*If random placement failed while we still need to place planes, 
			 * iterate through all positions and place a plane in the first available one*/
			if (planeLocations.size() < Airplanes.game.config.numPlanes) {
				for (o = 0;o < 4;++o) {
					for (x = 0;x < size;++x)
						for (y = 0;y < size;++y)
							{
								if (super.addPlane(x, y, p[o]) > 0) {
									continue try_placement;
								}
							}
				}
				/*If there is no available position, try the placement again from scratch*/
				randomizeLocations();
			}
		}
	}
	
	protected Vector2 gridToSceneCoords(int x, int y) {
		return gridToSceneCoords(new Point2D((int)x, (int)y));
	}
	protected Vector2 gridToSceneCoords(Point2D gridCoords)
	{
		Vector2 ret = new Vector2();
		ret.x = getX() + border + gridCoords.x*(cell+border);
		ret.y = getY() + getHeight() - (cell+border) - gridCoords.y*(cell+border);
		
		return ret;
	}
	
	private int clamp(int x, int min, int max)
	{
		if (x < min)
			return min;
		if (x > max)
			return max;
		return x;
	}
	protected Point2D screenToGridCoords(float x, float y) {
		return screenToGridCoords(new Vector2(x, y));
	}
	
	protected Point2D screenToGridCoords(Vector2 screenCoords)
	{
		float x = screenCoords.x - getX(), y = screenCoords.y - (Gdx.graphics.getHeight() - getHeight() - getY());
		x = (int) Math.floor(x / (cell+border));
		y = (int) Math.floor(y / (cell+border));
		return new Point2D(clamp((int)x, 0, size-1), clamp((int)y, 0, size-1));
	}
	
	protected Point2D sceneToGridCoords(float x, float y) {
		return sceneToGridCoords(new Vector2(x, y));
	}
	
	protected Point2D sceneToGridCoords(Vector2 sceneCoords) {
		return screenToGridCoords(sceneCoords.x, Gdx.graphics.getHeight() - sceneCoords.y);
	}
	
	public void setCellSize(int cellSize)
	{
		cell = cellSize;
		this.setWidth(size*(cell + border) + border);
		this.setHeight(size*(cell + border) + border);
		for (Plane p : planes)
		{
			p.setCellSize(cellSize);
			Vector2 pos = gridToSceneCoords(super.planeLocation(p.getID()));
			p.setPosition(pos.x, pos.y);
		}
		resize();
	}
	
	/**Notify the grid that the window was resized, in order to refresh drawing surfaces**/
	public void resize()
	{
		if (shape != null)
			shape.dispose();
		shape = new ShapeRenderer();
		
		Gdx.graphics.requestRendering();
	}
	
	@Override
	public void clear() {
		for (Plane p : planes)
			p.dispose();
		planes.clear();
		super.clear();
	}
	
	@Override
	public void setPosition(float x, float y)
	{
		transform.idt();
		transform.translate(x, y, 0);
		for (Plane p : planes)
			p.setPosition(p.getX() + x - getX(), p.getY() + y - getY());
		
		super.setPosition(x, y);
	}
	
	public boolean contains(int screenX, int screenY) {
		if (screenX < getX() + border || screenX >= getX() + getWidth() - border)
			return false;
		
		if (screenY < Gdx.graphics.getHeight() - getY() - getHeight() + border 
				|| screenY >= Gdx.graphics.getHeight() - getY() - border)
			return false;
		
		return true;
	}
	
	public int getCellSize() {
		return cell;
	}
	
	@Override
	public void draw(Batch batch, float parentAlpha)
	{
		if (hidden)
			return;
		
		shape.begin(ShapeType.Filled);
		{
			Gdx.gl.glEnable(GL10.GL_BLEND);
		    Gdx.gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
		    shape.setTransformMatrix(transform);
		    shape.setColor(0.33f, 0.3f, 0.27f, 0.7f);
			for (int i = 0;i <= size;++i)
			{
				/*ShapeRenderer.line crashes on gingerbread 2.3.6, no idea why.
				Use filled rectangles with border width or height instead.*/
				shape.rect(i*(cell + border), 0, border, size*(cell + border));
				shape.rect(0, i*(cell + border), size*(cell + border), border);
			}
			
			for (Plane p : planes)
				p.draw(shape, !hidePlanes);
		}
		shape.end();
		shape.setTransformMatrix(transform);
	}
	
	public String getLabel()
	{
		return label;
	}
	
	public void setLabel(String label)
	{
		this.label = label;
	}
	
	public void hidePlanes() {
		hidePlanes = true;
		for (Plane p : planes)
			p.hide();
		Gdx.graphics.requestRendering();
	}
	
	public void showPlanes() {
		hidePlanes = false;
		for (Plane p : planes)
			p.show();
		Gdx.graphics.requestRendering();
	}
	
	public void show() {
		hidden = false;
		Gdx.graphics.requestRendering();
	}
	
	public void hide() {
		hidden = true;
		Gdx.graphics.requestRendering();
	}

	public void dispose() {
		for (Plane p : planes)
			p.dispose();
		
		if (shape != null)
			shape.dispose();
		
		shape = null;
	}
}
