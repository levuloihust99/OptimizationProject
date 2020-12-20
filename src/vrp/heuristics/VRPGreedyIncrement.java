package vrp.heuristics;

import java.io.PrintStream;
import java.util.HashSet;
import java.util.Iterator;

import model.Config;
import model.Individual;
import model.Pair;

public class VRPGreedyIncrement {
	private int iteration;
	private HashSet<Individual> maintainSet;
	
	static {
		Config.runConfiguration();
	}
	
	public void init() {
		iteration = 1;
		maintainSet = new HashSet<>();
		maintainSet.add(new Individual());
	}
	
	public void incrementalGenerate() {
		Iterator<Individual> it = maintainSet.iterator();
		while (it.hasNext()) {
			Individual indi = it.next();
			Pair optPos = indi.getOptimalPosition(iteration);
			indi.insert(iteration, optPos.getPredecessor(), optPos.getSuccessor());
		}
		iteration++;
	}
	
	public void greedyBuild() {
		for (int i = 0; i < Config.N; i++) {
			incrementalGenerate();
		}
	}
	
	public void run(PrintStream pStr) {
		this.init();
		long start = System.currentTimeMillis();
		this.greedyBuild();
		long end = System.currentTimeMillis();
//		this.maintainSet.iterator().next().printIndi();
		pStr.println("Objective: " + this.maintainSet.iterator().next().getFitness());
		pStr.println("Elapsed: " + (end - start));
		pStr.println("-----------------------------------");
		System.out.println("Objective: " + this.maintainSet.iterator().next().getFitness());
		System.out.println("Elapsed: " + (end - start));
		System.out.println("-----------------------------------");
	}
	
	public static void main(String[] args) {
		VRPGreedyIncrement app = new VRPGreedyIncrement();
		app.init();
		long start = System.currentTimeMillis();
		app.greedyBuild();
		long end = System.currentTimeMillis();
		app.maintainSet.iterator().next().printIndi();
		System.out.println("Elapsed: " + (end - start));
	}
}
