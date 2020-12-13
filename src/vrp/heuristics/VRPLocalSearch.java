package vrp.heuristics;

import model.Config;
import model.Individual;

public class VRPLocalSearch {
	
	static {
		Config.runConfiguration();
	}
	
	public static void main(String[] args) {
		Individual indi = new Individual();
		indi.build();
		for (int i = 0; i < 1000; i++) {
			System.out.println("Iteration #" + (i + 1));
			indi.balanceMaxMin();
			System.out.println("Fitness = " + indi.getFitness());
			System.out.println("--------------------------------------");
		}
		indi.printIndi();
	}
}
