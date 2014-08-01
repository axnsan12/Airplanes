package com.axnsan.airplanes;

import java.util.ArrayList;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;

public class Plane extends Actor {
	public enum Orientation { UP, LEFT, DOWN, RIGHT };
	
	public static class Location {
		public int x, y;
		public Plane.Model model;
		public Orientation orientation;
		public Location(int x, int y, Plane.Model model) {
			this.x = x; this.y = y; this.model = model; this.orientation = model.orientation;
		}
		
		public Location(int x, int y, Orientation orientation) {
			this.x = x; this.y = y; this.orientation = orientation;
			model = null;
		}
		
		@Override
		public String toString() {
			return x + ";" + y + ";" + orientation + ";";
		}
		
		private void fromString(String s) {
			String[] a = s.split(";");
			x = Integer.parseInt(a[0]);
			y = Integer.parseInt(a[1]);
			orientation = Orientation.valueOf(a[2]);
		}
		
		public Location(String s) { fromString(s); model = null; }
	}
	

	private static float vertices[];
	private static int vCell = 0, vBorder = 0;
	
	private Orientation orientation = Orientation.UP;
	private int cellSize, borderWidth;
	private Matrix4 transform = new Matrix4();
	private Polygon boundingShape;
	private String hexRGB;
	private short ID;
	private boolean hidden = false, dead = false;
	private Model _gridModel;
	
	public Plane(int cellSize, int borderWidth, Orientation orientation, String hexRGB) 
	{ 
		this.borderWidth = borderWidth;
		this.hexRGB = hexRGB;
		
		initializeShape(cellSize, borderWidth);
		boundingShape = new Polygon(vertices);
		this.cellSize = cellSize;
		this.orientation = orientation;
	}
	
	protected void refresh() {
		transform.idt();
		transform.translate(getX(), getY(), 0f);

		initializeShape(cellSize, borderWidth);
		boundingShape = new Polygon(vertices);
		boundingShape.setPosition(getX(), getY());
		float w = 3*cellSize + 2*borderWidth, h = 4*cellSize + 3*borderWidth; 
		switch (this.orientation)
		{
		case DOWN:
			transform.translate(w, h, 0f);
			transform.rotate(0, 0, 1.f, 180);
			boundingShape.translate(w, h);
			boundingShape.setRotation(180);
			setSize(w, h);
			break;
		case LEFT:
			transform.translate(h, 0f, 0f);
			transform.rotate(0, 0, 1.f, 90);
			boundingShape.translate(h, 0);
			boundingShape.setRotation(90);
			setSize(h, w);
			break;
		case RIGHT:
			transform.translate(0f, w, 0f);
			transform.rotate(0, 0, 1.f, -90);
			boundingShape.translate(0, w);
			boundingShape.setRotation(-90);
			setSize(h, w);
			break;
		case UP:
			boundingShape.setRotation(0);
			setSize(w, h);
		}
		_gridModel = calculateGridModel(orientation);
		
		Gdx.graphics.requestRendering();
	}
	
	protected void resize()
	{
		refresh();
	}
	
	public void draw(ShapeRenderer shape, boolean forced) {
		if (forced) {
			boolean tmp = hidden;
			hidden = false;
			draw(shape);
			hidden = tmp;
		}
		else draw(shape);
	}
	public void draw(ShapeRenderer shape) {
		if (!hidden)
		{
			shape.setTransformMatrix(this.transform);
		    shape.setColor(Color.valueOf(hexRGB + "CF"));
		    for (int i = 0;i < vertices.length/2 - 1;++i)
		    {
		    	/*ShapeRenderer.line crashes on gingerbread 2.3.6, no idea why.
				Use filled rectangles instead.*/
		    	float lw, lh;
		    	float p1x = vertices[2*i], p1y = vertices[2*i + 1];
		    	float p2x = vertices[2*(i+1)], p2y = vertices[2*(i+1) + 1];
		    	if (p1x == p2x)
		    	{
		    		lw = borderWidth;
		    		lh = p2y - p1y;
		    	}
		    	else
		    	{
		    		lh = borderWidth;
		    		lw = p2x - p1x;
		    	}
		    	shape.rect(p1x, p1y, lw, lh);
		    }
		    
		    
		    shape.setColor(Color.valueOf(hexRGB + "7F"));
		    shape.rect(cellSize + borderWidth, 0, cellSize, 4*cellSize + 3*borderWidth);
		    shape.rect(0, 0, cellSize + borderWidth, cellSize);
		    shape.rect(0, 2*cellSize + 2*borderWidth, cellSize + borderWidth, cellSize);
		    shape.rect(2*cellSize + borderWidth, 0, cellSize + borderWidth, cellSize);
		    shape.rect(2*cellSize + borderWidth, 2*cellSize + 2*borderWidth, cellSize + borderWidth, cellSize);
		}
	}
	@Override
	public void draw(Batch batch, float parentAlpha)
	{
		throw new RuntimeException("Must use draw(ShapeRenderer)");
	}
	
	@Override
	public void setPosition(float x, float y)
	{
		super.setPosition(x, y);
		refresh();
	}
	
	/**Does the plane contain the given point?
	 * @param x x component in screen coordinates
	 * @param y y component in screen coordinates
	 */
	public boolean containsScreenCoords(float x, float y)
	{
		return boundingShape.contains(x, Gdx.graphics.getHeight() - y);
	}
	
	/**Does the plane contain the given point?
	 * @param x x component in scene coordinates
	 * @param y y component in scene coordinates
	 */
	public boolean containsSceneCoords(float x, float y)
	{
		return boundingShape.contains(x, y);
	}
	
	public void hide() {
		hidden = true;
	}
	
	public void show() {
		hidden = false;
	}
	
	public void setHidden(boolean hidden) {
		this.hidden = hidden;
	}
	
	public void setVisible(boolean visible) {
		this.hidden = !visible;
	}
	
	/**The plane is referred by the grid coordinates of the bottom left corner of its bounding box.<br>
	 * The grid model defines the spaces occupied by the plane, relative to this origin.<br>
	 * If point (i, j) is in the model, then (originX + i, originY - j) is a square 
	 * occupied by the plane on the grid.<br>
	 * (Grid coordinates have the origin on the top-left)<br>**/
	public static class Model {
		public int width, height;
		public boolean[][] body;
		public int headX, headY;
		public Orientation orientation;
		public int cellCount;
		
		private void rotateLeft()
		{
			boolean[][] tmp = new boolean[height][width];
			int tmp2;
			for (int i = 0;i < width;++i)
				for (int j = 0;j < height;++j)
					tmp[height-j-1][i] = body[i][j];
			
			tmp2 = headX;
			headX = height - headY - 1;
			headY = tmp2;
			
			body = tmp;
			tmp2 = width;
			width = height;
			height = tmp2;
		}
	};
	
	
	public Model model() { 
		if (_gridModel == null)
			_gridModel = calculateGridModel(orientation);
		return _gridModel; 
	}
	
	public Orientation getOrientation() {
		return orientation;
	}

	public void setOrientation(Orientation orientation) {
		this.orientation = orientation;
		refresh();
	}
	
	public void setCellSize(int cellSize) {
		this.cellSize = cellSize;
		resize();
	}
	
	public void dispose() {
	}
	
	public void kill()
	{
		dead = true;
		setVisible(Airplanes.game.config.revealDeadPlanes);
	}
	
	public boolean isDead()
	{
		return dead;
	}
	
	public void setID(short id) {
		ID = id;
	}
	public short getID() {
		return ID;
	}
	
	public static Model templateModel() {
		return calculateGridModel(Orientation.UP);
	}
	
	private static Model[] models = new Model[4];
	public static Model calculateGridModel(Orientation orientation) {
		int in = 0;
		switch (orientation) {
		case UP:
			in = 0;break;
		case LEFT:
			in = 1; break;
		case DOWN:
			in = 2; break;
		case RIGHT:
			in = 3;break;
		}
		
		if (models[in] != null)
			return models[in];
		
		Model ret = new Model();
		
		/*Set the model orientated upwards as default*/
		ret.headX = 1;
		ret.headY = 3;
		ret.body = new boolean[3][4];
		for (int i = 0;i < 3;++i)
			for (int j = 0;j < 4;++j)
				ret.body[i][j] = false;
		
							  ret.body[1][3]
		= ret.body[0][2] 	= ret.body[1][2] 	= ret.body[2][2]
							= ret.body[1][1]
		= ret.body[0][0] 	= ret.body[1][0] 	= ret.body[2][0] = true;
							  
		ret.width = 3;
		ret.height = 4;
		ret.orientation = orientation;
		ret.cellCount = 8;
		
		switch (orientation)
		{
		case RIGHT: /*To get it to face right, rotate left 3 times*/
			ret.rotateLeft();
		case DOWN: /*twice for down*/
			ret.rotateLeft();
		case LEFT: /*once for left*/
			ret.rotateLeft();
			break;
		case UP:
			break;
		}
		return models[in] = ret;
	}
	
	protected static void initializeShape(int cellSize, int borderWidth) {
		if (vCell == cellSize && vBorder == borderWidth)
			return;

		//TODO automatically generate vertices from grid model
		float w = 3*cellSize + 2*borderWidth - 1, h = 4*cellSize + 3*borderWidth - 1; 
		ArrayList<Vector2> corners = new ArrayList<Vector2>();
		corners.add(new Vector2(0, 0));
		corners.add(new Vector2(w, 0));
		corners.add(new Vector2(w, cellSize - 1));
		corners.add(new Vector2(2*cellSize + borderWidth - 1, cellSize - 1));
		corners.add(new Vector2(2*cellSize + borderWidth - 1, 2*cellSize + 2*borderWidth));
		corners.add(new Vector2(w, 2*cellSize + 2*borderWidth));
		corners.add(new Vector2(w, 3*cellSize + 2*borderWidth - 1));
		corners.add(new Vector2(2*cellSize + borderWidth - 1, 3*cellSize + 2*borderWidth - 1));
		corners.add(new Vector2(2*cellSize + borderWidth - 1, h));
		corners.add(new Vector2(cellSize + borderWidth, h));
		corners.add(new Vector2(cellSize + borderWidth, 3*cellSize + 2*borderWidth - 1));
		corners.add(new Vector2(0, 3*cellSize + 2*borderWidth - 1));
		corners.add(new Vector2(0, 2*cellSize + 2*borderWidth));
		corners.add(new Vector2(cellSize + borderWidth, 2*cellSize + 2*borderWidth));
		corners.add(new Vector2(cellSize + borderWidth, cellSize - 1));
		corners.add(new Vector2(0, cellSize - 1));
		corners.add(new Vector2(0, 0));
		vertices = new float[corners.size() * 2];
		int i = 0;
		for (Vector2 v : corners)
		{
			vertices[2*i] = v.x;
			vertices[2*i + 1] = v.y;
			++i;
		}
		vCell = cellSize;
		vBorder = borderWidth;
	}
	
	public static int maxPlanesOnGridSize(int gridSize) {
		int g = gridSize;
		/* Average number of planes with this model that can be randomly added to a 
		 * grid of size gridSize to fill it, using Grid.randomize().
		 * Determined from experimental data, accurate in the range [4, 50].
		 * Returning too much can cause Grid.randomize() to crash or enter an infinite loop.*/
		return (int) Math.round(0.0831055d*Math.pow(g, 2.d) - 0.0825607d*g + 0.139184d);
	}
}
