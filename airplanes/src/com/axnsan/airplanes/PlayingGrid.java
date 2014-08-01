package com.axnsan.airplanes;

import java.util.ArrayList;

import com.axnsan.airplanes.util.Point2D;
import com.axnsan.airplanes.util.StringManager;
import com.axnsan.airplanes.util.TapDetector;
import com.axnsan.airplanes.util.TapListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL10;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.Vector2;

public class PlayingGrid extends Grid implements TapListener, InputProcessor {
	private TextureSprite spriteX, spriteXRed, spriteDot;
	private TapDetector tdet = new TapDetector(this);
	
	/**Remember squares that were attacked on this grid<br>
	 * 0 = not attacked<br>
	 * 1 = miss<br>
	 * 2 = hit<br>
	 * 3 = head shot*/
	private byte[][] hits = null;
	private int deadPlanes = 0;
	private boolean targetable = false;
	private byte[] planeNumHits;
	private Point2D selectedCell = new Point2D(-1, -1), immuneCell = new Point2D(-1, -1);
	private boolean dragging;
	private MatchHandler handler;
	private int playerID;
	
	public int getPlayerID() { return playerID; }
	
	public PlayingGrid(int cellSize, int borderWidth, int gridSize, MatchHandler handler, int playerID) {
		super(cellSize, borderWidth, gridSize);
		
		this.handler = handler;
		this.playerID = playerID;
		if (playerID >= 0 && playerID < GameConfiguration.maxPlayers)
			setLabel(StringManager.getString("%splayer_grid")
					.replace("%s", Airplanes.game.config.playerNames[playerID]));
		
		/*Sprite used to display hits*/
		if (spriteX == null)
			spriteX = new TextureSprite("data/x.png", 0, 0, 64, 64, Color.BLACK);
		
		/*Sprite used to display head shots*/
		if (spriteXRed == null)
			spriteXRed = new TextureSprite("data/x.png", 0, 0, 64, 64, Color.RED);
		
		/*Sprite used to display misses*/
		if (spriteDot == null)
			spriteDot = new TextureSprite("data/dot.png", 0, 0, 64, 64, Color.BLACK);
		
		hits = new byte[gridSize][gridSize];
		planeNumHits = new byte[Airplanes.game.config.numPlanes + 1];
	}

	@Override
	public void draw(Batch batch, float parentAlpha) {
		if (hidden)
			return;
		
		batch.end();
		if (dragging && Gdx.input.isTouched(0))
			selectCell(screenToGridCoords(Gdx.input.getX(0), Gdx.input.getY(0)));
		
		super.draw(batch, parentAlpha);
		if (selectedCell.x >= 0 && selectedCell.y >= 0 && this.targetable) {
			/*Draw a rectangle on the cell that is selected*/
			Vector2 pos = gridToSceneCoords(selectedCell);
			pos.x -= getX();
			pos.y -= getY();
			shape.begin(ShapeType.Filled);
			{
				Gdx.gl.glEnable(GL10.GL_BLEND);
			    Gdx.gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
			    shape.setColor(0.8f, 0f, 0f, 1f);
				shape.rect(pos.x + cell/2, 0, 1, pos.y);
				shape.rect(pos.x + cell/2, pos.y + cell, 1, getHeight() - pos.y - cell);
				shape.rect(0, pos.y + cell/2, pos.x, 1);
				shape.rect(pos.x + cell, pos.y + cell/2, getWidth() - pos.x - cell, 1);
				
			    shape.setColor(0f, 0f, 0f, 1f);
				/*ShapeRenderer.line crashes on gingerbread 2.3.6, no idea why.
				Use filled rectangles with border width or height instead.*/
				shape.rect(pos.x, pos.y, cell, 3);
				shape.rect(pos.x + cell, pos.y, -3, cell);
				shape.rect(pos.x + cell, pos.y + cell, -cell, -3);
				shape.rect(pos.x, pos.y + cell, 3, -cell);
				
				
			}
			shape.end();
		}
		
		batch.begin();
		if (tdet.waiting()) {
			tdet.elapsed(Gdx.graphics.getRawDeltaTime());
			Gdx.graphics.requestRendering();
		}
		
		{
			for (int i = 0;i < size;++i) {
				for (int j = 0;j < size;++j) {
					if (hits[i][j] == 0)
						continue;
					
					Vector2 pos = super.gridToSceneCoords(i, j);
	
					switch (hits[i][j]) {
					case MISS:
						spriteDot.draw(pos.x, pos.y, batch);
						break;
					case BODY:
						spriteX.draw(pos.x, pos.y, batch);
						break;
					case HEAD:
						spriteXRed.draw(pos.x, pos.y, batch);
						break;
					}
				}
			}
		}
	}
	
	public int planesLeft() {
		return planes.size() - deadPlanes;
	}
	
	public boolean canAttackCell(int x, int y) {
		return !(hits[x][y] != NONE || planesLeft() <= 0);
	}
	
	public void attackCell(int x, int y) {
		if (!canAttackCell(x, y))
			return;

		byte result = cellType(x, y);
		System.out.println("player " + playerID + "'s grid hit at " + x + " " + y + ": " + result);
		
		short planeID = planeAt(x, y);
		Plane hitPlane = null;
		if (planeID > 0)
			hitPlane = planes.get(planeID-1);
		
		GameConfiguration.KillMode killMode = Airplanes.game.config.killMode;
		if (killMode == GameConfiguration.KillMode.Headshot && result == HEAD) {
			if (Airplanes.game.config.revealDeadPlanes) {
				Plane.Model model = hitPlane.model();
				Plane.Location loc = super.planeLocations.get(planeID-1);
				for (int i = 0; i < model.width; ++i)
					for (int j = 0;j < model.height; ++j)
						if (model.body[i][j] && hits[loc.x+i][loc.y-j] == NONE)
							hits[loc.x+i][loc.y-j] = SILENT_HIT;
			}
			removePlaneAt(x, y);
			++deadPlanes;
			hitPlane.kill();
		}
		if (killMode != GameConfiguration.KillMode.Headshot) {
			if (result == HEAD)
				result = BODY;
			
			if (result == BODY) {
				++planeNumHits[hitPlane.getID()];
				if (planeNumHits[hitPlane.getID()] >= hitPlane.model().cellCount) {
					++deadPlanes;
					removePlaneAt(x, y);
					hitPlane.kill();
				}
			}
		}
		
		if (deadPlanes == this.planes.size()) {
			setTargetable(false);
			showPlanes();
			handler.playerDied(playerID);
		}
		
		hits[x][y] = result;
		selectedCell.x = selectedCell.y = immuneCell.x = immuneCell.y = -1;
	}
	
	public void selectCell(Point2D gridCoords) {
		selectCell(gridCoords.x, gridCoords.y);
	}
	
	public void selectCell(int x, int y) {
		if (x < 0 || x >= size || y < 0 || y >= size 
				|| !this.targetable)
			return;
		
		if (planesLeft() <= 0)
			return;
		
		selectedCell.x = x;
		selectedCell.y = y;
		handler.cellSelected(selectedCell);
	}
	
	@Override
	public void hidePlanes()
	{
		super.hidePlanes();
		if (Airplanes.game.config.revealDeadPlanes)
		{
			for (Plane p : planes)
			{
				if (p.isDead())
					p.setVisible(true);
			}
		}
	}
	
	public void addMoves(ArrayList<Point2D> moves) {
		for (Point2D m : moves)
			attackCell(m.x, m.y);
	}
	
	@Override
	public void tap(int x, int y) {
		if (hidden || !this.contains(x, y))
			return;
		
		Point2D pos = screenToGridCoords(x, y);
		if (selectedCell.x == pos.x && selectedCell.y == pos.y) {
			if (immuneCell.x == pos.x && immuneCell.y == pos.y)
				{ immuneCell.x = -1; immuneCell.y = -1; }
			else if (canAttackCell(pos.x, pos.y)) {
				handler.playerWasAttacked(playerID, pos);
				attackCell(pos.x, pos.y);
			}
		}
		else selectCell(pos.x, pos.y);
	}
	
	public void setHandler(MatchHandler handler) {
		this.handler = handler;
	}

	public void setCellSize(int cellSize) {
		super.setCellSize(cellSize);
		spriteX.setSize(cell, cell);
		spriteXRed.setSize(cell, cell);
		spriteDot.setSize(cell, cell);
	}
	
	public void dispose() {
		if (spriteX != null)
			spriteX.dispose();
		if (spriteXRed != null)
			spriteXRed.dispose();
		if (spriteDot != null)
			spriteDot.dispose();

		spriteX = spriteXRed = spriteDot = null;
	}
	
	private static class TextureSprite extends Sprite {
		private Texture tex;
		
		public TextureSprite(String texture_path, int x, int y, int width, int height, Color tint) {
			super();
			tex = new Texture(Gdx.files.internal(texture_path));
			tex.setFilter(TextureFilter.Linear, TextureFilter.Linear);
			super.setRegion(new TextureRegion(tex, x, y, width, height));
			super.setOrigin(0, 0);
			super.setColor(tint);
		}
		
		public void draw(float x, float y, Batch batch)
		{
			super.setPosition(x, y);
			super.draw(batch);
		}
		
		public void dispose()
		{
			tex.dispose();
		}
	}
	
	@Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
		if ((!hidden) && targetable && pointer == 0 && this.contains(screenX, screenY))
		{
        	tdet.down(screenX, screenY);
        	dragging = true;
        	
        	Point2D pos = screenToGridCoords(screenX, screenY);
        	if (!(pos.x == selectedCell.x && pos.y == selectedCell.y))
        	{
	        	selectCell(pos);
	        	immuneCell.x = selectedCell.x;
	        	immuneCell.y = selectedCell.y;
        	}
			return true;
		}
		return false;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        if ((!hidden) && targetable && pointer == 0)
        {
        	tdet.up(screenX, screenY);
        	dragging = false;
        	if (this.contains(screenX, screenY))
        		return true;
        	else return false;
        }
        return false;
    }

    public void setTargetable(boolean targetable) {
    	this.targetable = targetable;
    	this.selectedCell.x = this.selectedCell.y = this.immuneCell.x = this.immuneCell.y = -1;
    }
    
	@Override
	public boolean keyDown(int keycode) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean keyUp(int keycode) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean keyTyped(char character) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean touchDragged(int screenX, int screenY, int pointer) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean mouseMoved(int screenX, int screenY) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean scrolled(int amount) {
		// TODO Auto-generated method stub
		return false;
	}

}
