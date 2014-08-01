package com.axnsan.airplanes;

import java.util.ArrayList;

import com.axnsan.airplanes.util.Point2D;

public class Player {
	public PlayingGrid grid = null;
	public ArrayList<Plane.Location> planes = new ArrayList<Plane.Location>();
	public ArrayList<Point2D> moves = new ArrayList<Point2D>();
	public String name;
	public int movesLeft = 0;
	public Point2D selectedCell = new Point2D(-1, -1);
	public static enum State { NONE, PLACING, READY, LASTSTAND, DEAD, DISCONNECTED };
	public State state = State.NONE;
	public int turnOfDeath = -1;
	
	private boolean turn;
	
	@Override
	public String toString()
	{
		StringBuilder str = new StringBuilder();
		str.append(name).append('|');
		str.append(state.toString()).append('|');
		for (Plane.Location loc : planes)
			str.append(loc.toString());
		str.append('|');
		for (Point2D move : moves)
			str.append(move.toString());
		str.append('|');
		str.append(selectedCell.toString()).append('|');
		str.append(turnOfDeath).append('|');
		str.append(movesLeft).append('|');
		str.append(turn).append('|');
		
		return str.toString();
	}
	public void fromString(String string) 
	{
		String[] mem = string.split("\\|");
		name = mem[0];
		state = State.valueOf(mem[1]);
		String[] locs = mem[2].split("\\;");
		for (int i = 0; i+2 < locs.length; i += 3)
			planes.add(new Plane.Location(Integer.parseInt(locs[i])
					, Integer.parseInt(locs[i+1])
					, Plane.Orientation.valueOf(locs[i+2])));
		String[] moves = mem[3].split("\\;");
		for (int i = 0; i+1 < moves.length; i += 2)
			this.moves.add(new Point2D(Integer.parseInt(moves[i])
					, Integer.parseInt(moves[i+1])));
		String[] sel = mem[4].split("\\;");
		selectedCell.x = Integer.parseInt(sel[0]);
		selectedCell.y = Integer.parseInt(sel[1]);
		turnOfDeath = Integer.parseInt(mem[5]);
		movesLeft = Integer.parseInt(mem[6]);
		turn = Boolean.parseBoolean(mem[7]);
	}
	
	public Player(String repr) {
		this.fromString(repr);
	}
	
	public Player(PlayingGrid grid, ArrayList<Plane.Location> planes, int playerID) {
		this.grid = grid;
		if (planes != null)
			this.planes = planes;
		name = Airplanes.game.config.playerNames[playerID];
		//name = StringManager.getString("player_%d").replace("%d", Integer.toString(id));
		//++id;
	}
	
	public void beginTurn()
	{
		turn = true;
		movesLeft = 1;
		selectedCell.x = selectedCell.y = -1;
	}
	
	public void endTurn()
	{
		turn = false;
		if (state == State.LASTSTAND)
			state = State.DEAD;
	}
	
	
	public boolean isTurn()
	{
		return turn;
	}
	
	public void dispose() {
		if (grid != null)
			grid.dispose();
		if (planes != null)
			planes.clear();
		if (moves != null)
			moves.clear();
		
		grid = null;
	}
}
