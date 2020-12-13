package model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;

public class Individual {
	private HashSet<Integer> startNodes;
	private HashMap<Integer, Integer> next;
	private boolean fitnessCalculated = false;
	private int fitnessCache = -1;
	private HashMap<Integer, Integer> subFitness;
	private static Random r;
	
	static {
		r = new Random();
//		r.setSeed(100);
	}
	
	public Individual() {
		startNodes = new HashSet<>();
		next = new HashMap<>();
		subFitness = new HashMap<>();
	}
	
	public Individual clone() {
		Individual indi = new Individual();
		Iterator<Integer> startNodeIterator = this.startNodes.iterator();
		while (startNodeIterator.hasNext()) {
			int startNode = startNodeIterator.next();
			indi.startNodes.add(startNode);
		}
		
		Iterator<Integer> nextIterator = this.next.keySet().iterator();
		while (nextIterator.hasNext()) {
			int node = nextIterator.next();
			indi.next.put(node, this.next.get(node));
		}
		
		Iterator<Integer> subFitnessIterator = this.subFitness.keySet().iterator();
		while (subFitnessIterator.hasNext()) {
			int node = subFitnessIterator.next();
			indi.subFitness.put(node, this.subFitness.get(node));
		}
		
		indi.fitnessCache = this.fitnessCache;
		indi.fitnessCalculated = this.fitnessCalculated;
		
		return indi;
	}		
	
	public void build() {
		this.startNodes.clear();
		this.next.clear();
		this.subFitness.clear();
		fitnessCalculated = false;
		fitnessCache = -1;
		for (int i = 1; i < Config.N + 1; i++) {
			if (i == 1) {
				this.next.put(i, i + 1);
				this.startNodes.add(i);
			}
			else if (i == Config.N) {
				this.next.put(i, 0);
			}
			else {
				this.next.put(i, i + 1);
			}
		}
		this.getFitness();
	}
	
	public int getFitness() {
		if (!fitnessCalculated) {
			subFitness.clear();
			int fitness = 0;
			Iterator<Integer> it = this.startNodes.iterator();
			while (it.hasNext()) {
				int node = it.next();
				int prev = 0;
				while (node != 0) {
					fitness += Config.transferTime[0][node] + Config.deliveryTime[node];
					prev = node;
					node = this.next.get(node);
				}
				fitness += Config.transferTime[prev][0];
				this.subFitness.put(prev, fitness);
			}
			if (this.startNodes.size() < Config.K) {
				subFitness.put(0, 0);
			}
			fitnessCalculated = true;
			fitnessCache = fitness;
		}
		return fitnessCache;
	}
	
	private void updateFitness() {
		Iterator<Integer> it = this.startNodes.iterator();
		int maxSubFitness = Integer.MIN_VALUE;
		while (it.hasNext()) {
			int node = it.next();
			int prev = 0;
			while (node != 0) {
				prev = node;
				node = this.next.get(node);
			}
			if (maxSubFitness < this.subFitness.get(prev)) {
				maxSubFitness = this.subFitness.get(prev);
			}
		}
		this.fitnessCache = maxSubFitness;
	}

//	private void updateFitness(int startNode) {
//		try {
//			if (!this.startNodes.contains(startNode)) {
//				throw new Exception("Pass the startNode of a branch");
//			}
//		}
//		catch (Exception e) {
//			e.printStackTrace();
//		}
//		int node = startNode;
//		int prev = 0;
//		int subFitness = 0;
//		while (node != 0) {
//			subFitness += Config.transferTime[prev][node] + Config.deliveryTime[node];
//			prev = node;
//			node = this.next.get(node);
//		}
//		subFitness += Config.transferTime[prev][0];
//		this.subFitness.put(prev, subFitness);
//		
//		Iterator<Integer> it = this.subFitness.keySet().iterator();
//		int maxSubFitness = Integer.MIN_VALUE;
//		while (it.hasNext()) {
//			int endNode = it.next();
//			if (maxSubFitness < this.subFitness.get(endNode)) {
//				maxSubFitness = this.subFitness.get(endNode);
//			}
//		}
//		
//		this.fitnessCache = maxSubFitness;
//	}
	
	private void updateFitness(int predecessor, int middle) {
		int successor = this.next.get(middle);
		int diff = Config.transferTime[predecessor][middle] + Config.deliveryTime[middle]
				+ Config.transferTime[middle][successor] - Config.transferTime[predecessor][successor];
		int node = successor;
		int prev = middle;
		boolean exeFlag = false;
		while (node != 0) {
			exeFlag = true;
			prev = node;
			node = this.next.get(node);
		}
		int terminalNode = prev;
		if (!exeFlag) {
			terminalNode = predecessor;
		}
		
		this.subFitness.put(prev, this.subFitness.get(terminalNode) + diff);
		if (terminalNode != prev && terminalNode != 0) {
			this.subFitness.remove(terminalNode);
		}
		if (this.subFitness.size() > Config.K) {
			this.subFitness.remove(0);
		}
		if (this.subFitness.get(prev) > this.getFitness()) {
			this.fitnessCache = this.subFitness.get(prev);
		}
		else {
			Iterator<Integer> it = this.subFitness.keySet().iterator();
			int fitness = this.subFitness.get(prev);
			while (it.hasNext()) {
				int endNode = it.next();
				if (endNode != prev && this.subFitness.get(endNode) > fitness) {
					fitness = this.subFitness.get(endNode);
				}
			}
			this.fitnessCache = fitness;
		}
	}
	
	public Pair getOptimalPosition(int comingNode) {
		ArrayList<Pair> candidates = getCandidatePosition(comingNode);
		return candidates.get(r.nextInt(candidates.size()));
	}
	
	public ArrayList<Pair> getCandidatePosition(int comingNode) {
		if (!this.fitnessCalculated) {
			this.getFitness();
		}
		ArrayList<Pair> candidates = new ArrayList<>();
		Iterator<Integer> it = this.startNodes.iterator();
		int min = Integer.MAX_VALUE;
		while (it.hasNext()) {
			int node = it.next();
			int prev =  0;
			while (node != 0) {
				int diff = this.difference(comingNode, prev, node);
				if (diff < min) {
					min = diff;
					candidates.clear();
					candidates.add(new Pair(prev, node));
				}
				else if (diff == min) {
					candidates.add(new Pair(prev, node));
				}
				prev = node;
				node = this.next.get(node);
			}
			int diffOut = this.difference(comingNode, prev, node);
			if (diffOut < min) {
				min = diffOut;
				candidates.clear();
				candidates.add(new Pair(prev, node));
			}
			else if (diffOut == min) {
				candidates.add(new Pair(prev, node));
			}
		}
		
		if (this.startNodes.size() < Config.K) {
			int diffZero = this.difference(comingNode, 0, 0);
			if (diffZero < min) {
				candidates.clear();
				candidates.add(new Pair(0, 0));
			}
			else if (diffZero == min) {
				candidates.add(new Pair(0, 0));
			}
		}
		
		return candidates;
	}
	
	public int difference(int comingNode, int predecessor, int successor) {
		int diff = Config.transferTime[predecessor][comingNode] + Config.deliveryTime[comingNode] 
				+ Config.transferTime[comingNode][successor] - Config.transferTime[predecessor][successor];
		int node = successor;
		int prev = predecessor;
		while (node != 0) {
			prev = node;
			node = this.next.get(node);
		}
		int fitnessDiff = this.subFitness.get(prev) + diff - this.getFitness();
		if (fitnessDiff >= 0) {
			return fitnessDiff;
		}
		
		Iterator<Integer> it = this.subFitness.keySet().iterator();
		int fitness = this.subFitness.get(prev) + diff;
		while (it.hasNext()) {
			int terminalNode = it.next();
			if (terminalNode != prev && this.subFitness.get(terminalNode) > fitness) {
				fitness = this.subFitness.get(terminalNode);
			}
		}
		return fitness - this.getFitness();
	}
	
	public void insert(int node, int predecessor, int successor) {
		if (predecessor == 0) {
			this.startNodes.add(node);
			if (successor != 0) {
				this.startNodes.remove(successor);
			}
		}
		else {
			this.next.put(predecessor, node);
		}
		this.next.put(node, successor);
		this.updateFitness(predecessor, node);
	}
	
	public int balanceMaxMin() {
		// calculate fitness first, to know about subFitness
		if (!fitnessCalculated) {
			this.getFitness();
		}
		
		// case 0 -> 0
		if (this.startNodes.size() == 0) return 0;
		
		// In this case, there is at least one startNode, then the block inside while loop
		// is executed at least once.
		int maxNode = 0;
		int minNode = 0;
		int maxSubFitness = Integer.MIN_VALUE;
		int minSubFitness = Integer.MAX_VALUE;
		
		Iterator<Integer> it = this.startNodes.iterator();
		while (it.hasNext()) {
			int origin = it.next();
			int prev = 0;
			int node = origin;
			while (node != 0) {
				prev = node;
				node = this.next.get(node);
			}
			
			if (this.subFitness.get(prev) > maxSubFitness) {
				maxSubFitness = this.subFitness.get(prev);
				maxNode = origin;
			}
			if (this.subFitness.get(prev) < minSubFitness) {
				minSubFitness = this.subFitness.get(prev);
				minNode = origin;
			}
		}
		
		// Max branch and min branch are identical
		if (this.startNodes.size() == Config.K && maxNode == minNode) return 0;
		
		// max branch process
		int maxReduce = Integer.MIN_VALUE;
		int maxReduceNode = 0;
		int prevMaxReduceNode = 0;
		int prevMaxBranch;
		
		prevMaxBranch = 0;
		int currentMaxBranch = maxNode;
		while (currentMaxBranch != 0) {
			int nextMaxBranch = this.next.get(currentMaxBranch);
			int reduce = Config.transferTime[prevMaxBranch][currentMaxBranch] + Config.deliveryTime[currentMaxBranch]
					+ Config.transferTime[currentMaxBranch][nextMaxBranch] - Config.transferTime[prevMaxBranch][nextMaxBranch];
			if (maxReduce < reduce) {
				maxReduce = reduce;
				maxReduceNode = currentMaxBranch;
				prevMaxReduceNode = prevMaxBranch;
			}
			prevMaxBranch = currentMaxBranch;
			currentMaxBranch = this.next.get(currentMaxBranch);
		}
		
		// 4 if-else
		int nextMaxReduceNode = this.next.get(maxReduceNode);
		if (prevMaxReduceNode == 0 && nextMaxReduceNode == 0) {
			this.startNodes.remove(maxReduceNode);
			this.subFitness.remove(maxReduceNode);
			maxNode = 0;
		}
		else if (prevMaxReduceNode == 0 && nextMaxReduceNode != 0) {
			this.startNodes.add(nextMaxReduceNode);
			this.startNodes.remove(maxReduceNode);
			this.subFitness.put(prevMaxBranch, maxSubFitness - maxReduce);
			maxNode = nextMaxReduceNode;
		}
		else if (prevMaxReduceNode != 0 && nextMaxReduceNode == 0) {
			this.next.put(prevMaxReduceNode, 0);
			this.subFitness.put(prevMaxReduceNode, maxSubFitness - maxReduce);
			this.subFitness.remove(maxReduceNode);
		}
		else {
			this.next.put(prevMaxReduceNode, nextMaxReduceNode);
			this.subFitness.put(prevMaxBranch, maxSubFitness - maxReduce);
		}
		this.next.remove(maxReduceNode); // can be ignored
		
		// min branch process
		int minIncrease = Integer.MAX_VALUE;
		int minIncreasePredePos = 0;
		int minIncreaseSuccPos = 0;
		int prevMinBranch = 0;
		if (this.startNodes.size() < Config.K) {
			minIncrease = Config.transferTime[0][maxReduceNode] + Config.deliveryTime[maxReduceNode] + 
					Config.transferTime[maxReduceNode][0];
			minIncreasePredePos = 0;
			minIncreaseSuccPos = 0;
			prevMinBranch = 0;
		}
		else {
			prevMinBranch = 0;
			int currentMinBranch = minNode;
			while (currentMinBranch != 0) {
				int increase = Config.transferTime[prevMinBranch][maxReduceNode] + Config.deliveryTime[maxReduceNode]
						+ Config.transferTime[maxReduceNode][currentMinBranch] - Config.transferTime[prevMinBranch][currentMinBranch];
				if (minIncrease > increase) {
					minIncrease = increase;
					minIncreasePredePos = prevMinBranch;
					minIncreaseSuccPos = currentMinBranch;
				}
				prevMinBranch = currentMinBranch;
				currentMinBranch = this.next.get(currentMinBranch);
			}
			
			// add here 
			int increase = Config.transferTime[prevMinBranch][maxReduceNode] + Config.deliveryTime[maxReduceNode]
					+ Config.transferTime[maxReduceNode][0] - Config.transferTime[prevMinBranch][0];
			if (minIncrease > increase) {
				minIncrease = increase;
				minIncreasePredePos = prevMinBranch;
				minIncreaseSuccPos = 0;
			}
		}
		
		// 4 if-else
		if (minIncreasePredePos == 0 && minIncreaseSuccPos == 0) {
			this.startNodes.add(maxReduceNode);
			this.next.put(maxReduceNode, 0);
			this.subFitness.put(maxReduceNode, minIncrease);
			if (this.subFitness.size() > Config.K) {
				this.subFitness.remove(0);
			}
			minSubFitness = 0;
			minNode = maxReduceNode;
		}
		else if (minIncreasePredePos == 0 && minIncreaseSuccPos != 0) {
			this.startNodes.add(maxReduceNode);
			this.startNodes.remove(minIncreaseSuccPos);
			this.next.put(maxReduceNode, minIncreaseSuccPos);
			this.subFitness.put(prevMinBranch, minSubFitness + minIncrease);
			minNode = maxReduceNode;
		}
		else if (minIncreasePredePos != 0 && minIncreaseSuccPos == 0) {
			this.next.put(maxReduceNode, 0);
			this.next.put(minIncreasePredePos, maxReduceNode);
			this.subFitness.put(maxReduceNode, minSubFitness + minIncrease);
			this.subFitness.remove(minIncreasePredePos);
		}
		else {
			this.next.put(minIncreasePredePos, maxReduceNode);
			this.next.put(maxReduceNode, minIncreaseSuccPos);
			this.subFitness.put(prevMinBranch, minSubFitness + minIncrease);
		}
		
		this.updateFitness();
		return Math.max(-maxReduce, minSubFitness + minIncrease - maxSubFitness);
	}
	
	public void printIndi() {
		Iterator<Integer> it = this.startNodes.iterator();
		int count = 1;
		while (it.hasNext()) {
			System.out.printf("Tour #%d: ", count);
			int node = it.next();
			int prev = 0;
			while (node != 0) {
				System.out.printf("%d ", node);
				prev = node;
				node = this.next.get(node);
				if (node != 0) {
					System.out.print(" -> ");
				}
			}
			System.out.printf("\nFinish time: %d\n", this.subFitness.get(prev));
			System.out.println("--------------------------------------");
			count++;
		}
		System.out.println("Fitness = " + this.getFitness());
		System.out.println("--------------------------------------");
	}
}
