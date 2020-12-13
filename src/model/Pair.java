package model;

public class Pair {
	private int predecessor;
	private int successor;
	
	public Pair(int predecessor, int successor) {
		this.predecessor = predecessor;
		this.successor = successor;
	}
	
	public int getPredecessor() {
		return this.predecessor;
	}
	
	public int getSuccessor() {
		return this.successor;
	}
}
