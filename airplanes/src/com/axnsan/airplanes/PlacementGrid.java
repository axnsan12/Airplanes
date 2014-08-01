package com.axnsan.airplanes;

import java.util.ArrayList;

import com.axnsan.airplanes.util.Detector;
import com.axnsan.airplanes.util.DoubleTapDetector;
import com.axnsan.airplanes.util.DoubleTapListener;
import com.axnsan.airplanes.util.Point2D;
import com.axnsan.airplanes.util.TapDetector;
import com.axnsan.airplanes.util.TapListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.g2d.Batch;


public final class PlacementGrid extends Grid implements InputProcessor, DoubleTapListener, TapListener {
	private ArrayList<Detector> detectors = new ArrayList<Detector>();
	private DoubleTapDetector dtdet = new DoubleTapDetector(this);
	private TapDetector tdet = new TapDetector(this);

	/**Hold info about a touch/left click, to be processed in the next draw call**/
	class TouchInfo {
		/**Touch/left click coordinates, in screen space**/
        public float X = 0, Y = 0;
        public float duration;
        
        /**True while the finger is on screen/left mouse button is down**/
        public boolean touched = false;
    };
    
    /**Hold info about a plane being dragged*/
    class DragInfo extends TouchInfo {
    	/**The plane currently being dragged**/
    	public Plane draggingPlane = null;
    	
    	/**Coordinates the plane would be at if it wasn't snapped to grid**/
    	public float planeX, planeY;
    	public float initialX, initialY;
    	
    	public DragInfo(Plane plane) { this.draggingPlane = plane; }
    };
    private TouchInfo touch = new TouchInfo();
    private DragInfo drag = null;
    
	public PlacementGrid(int cellSize, int borderWidth, int gridSize) {
		super(cellSize, borderWidth, gridSize);
		
		detectors.add(dtdet);
		detectors.add(tdet);
	}
	
	@Override
	public void draw(Batch batch, float parentAlpha)
	{
		for (Detector det : detectors){
			if (det.waiting()) {
				det.elapsed(Gdx.graphics.getRawDeltaTime());
				Gdx.graphics.requestRendering();
			}
		}

		super.draw(batch, parentAlpha);
		
		if (drag != null)
		{
			/*Calculate the amount we need to move.
			The y axis increases downwards in screen coordinates, while it 
			increases upwards in scene coordinates. Thus we have to negate the 
			difference when calculating it.*/
			float deltaX = Gdx.input.getX() - drag.X, deltaY = -Gdx.input.getY() + drag.Y;
			float x = drag.planeX + deltaX;
			float y = drag.planeY + deltaY;
			
			/*If the drag would bring the plane out of the grid, clamp it back inside*/
			x = Math.max(x, getX());
			x = Math.min(x, getX() + getWidth() - drag.draggingPlane.getWidth());
			y = Math.max(y, getY());
			y = Math.min(y, getY() + getHeight() - drag.draggingPlane.getHeight());
			
			/*Snap the plane to the grid*/
			x = x - getX();
			x = (cell+border) * Math.round(x/(cell+border));
			x += getX() + border;
			y = y - getY();
			y = (cell+border) * Math.round(y/(cell+border));
			y += getY() + border;
			
			/*Commit the coordinates, save drag info for next frame and proceed to draw the grid*/
			drag.draggingPlane.setPosition(x, y);
			drag.X = Gdx.input.getX();
			drag.Y = Gdx.input.getY();
			drag.planeX += deltaX;
			drag.planeY += deltaY;
		}
		
	}
    
    /**Start dragging the given plane using the coordinates given as pivot**/
    private void startDrag(Plane plane, int screenX, int screenY)
    {
    	drag = new DragInfo(plane);
    	drag.X = screenX;
    	drag.Y = screenY;
    	drag.planeX = drag.initialX = plane.getX();
    	drag.planeY = drag.initialY = plane.getY();
    	super.detachPlane(plane.getID());
    }
    
    private void stopDrag()
    {
    	if (drag != null) {
	    	Point2D pos = sceneToGridCoords(drag.draggingPlane.getX() + cell/2, drag.draggingPlane.getY() + cell/2);
	    	if (!super.attachPlane(pos.x, pos.y)) {
	    		super.reattachPlane();
	    		drag.draggingPlane.setPosition(drag.initialX, drag.initialY);
	    	}
    	}
    	drag = null;
    }
    
    @Override
    public void clear() {
    	stopDrag();
    	super.clear();
    }
    
	@Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
		if (pointer == 0 && this.contains(screenX, screenY))
		{
			touch.touched = true;
        	touch.X = screenX;
        	touch.Y = screenY;
        	tdet.down(screenX, screenY);
        	for (Plane p : planes) {
				if (drag == null && p.containsScreenCoords(touch.X, touch.Y))
					startDrag(p, (int)touch.X, (int)touch.Y);
			}
			return true;
		}
		return false;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        if (pointer == 0)
        {
        	stopDrag();
        	touch.touched = false;
        	tdet.up(screenX, screenY);
        	return false;
        }
        return false;
    }
	
	@Override
	public void tap(int x, int y) {
		
		dtdet.tap((int)touch.X, (int)touch.Y);
	}
	
	@Override
	public void doubleTap(int x, int y) {
    	for (Plane p : planes) {
			if (p.containsScreenCoords(touch.X, touch.Y)) {
				stopDrag();
				/*If a plane was double tapped, attempt to rotate it*/
				Plane.Orientation initial = p.getOrientation(), target;
				switch (initial) {
				case UP:
					target = Plane.Orientation.LEFT; break;
				case LEFT:
					target = Plane.Orientation.DOWN; break;
				case DOWN:
					target = Plane.Orientation.RIGHT; break;
				case RIGHT:
					target = Plane.Orientation.UP; break;
				default:
					target = initial;
				}
				super.detachPlane(p.getID());
				p.setOrientation(target);
				if (!super.reattachPlane(p.model())) {
					super.reattachPlane();
					p.setOrientation(initial);
				}
				
				break;
			}
    	}
	}

	
	@Override
	public void dispose() {
		
	}
	
	@Override
	public boolean keyDown(int keycode) {
		return false;
	}

	@Override
	public boolean keyUp(int keycode) {
		return false;
	}

	@Override
	public boolean keyTyped(char character) {
		return false;
	}

	@Override
	public boolean touchDragged(int screenX, int screenY, int pointer) {
		return false;
	}

	@Override
	public boolean mouseMoved(int screenX, int screenY) {
		return false;
	}

	@Override
	public boolean scrolled(int amount) {
		return false;
	}
}
