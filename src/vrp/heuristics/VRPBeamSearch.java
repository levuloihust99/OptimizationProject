package vrp.heuristics;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;

import model.Config;
import model.Individual;
import model.Pair;

public class VRPBeamSearch {
	private int iteration;
	private HashSet<Individual> maintainSet;
	private static final int CUTTHRESHOLD = 50;
	
	static {
		Config.runConfiguration();
	}
	
	public void init() {
		iteration = 1;
		maintainSet = new HashSet<>();
		maintainSet.add(new Individual());
	}
	
	private void _beamSearchHelper() {
		Iterator<Individual> it = maintainSet.iterator();
		HashSet<Individual> nextSet = new HashSet<>();
		while (it.hasNext()) {
			Individual indi = it.next();
			ArrayList<Pair> candidates = indi.getCandidatePosition(iteration);
			for (int i = 0; i < candidates.size(); i++) {
				Pair pair = candidates.get(i);
				Individual clone = indi.clone();
				clone.insert(iteration, pair.getPredecessor(), pair.getSuccessor());
				nextSet.add(clone);
			}
		}
		
		Individual[] indiArr = new Individual[nextSet.size()];
		Iterator<Individual> indiIter = nextSet.iterator();
		int idx = 0;
		while (indiIter.hasNext()) {
			indiArr[idx] = indiIter.next();
			idx++;
		}
		
		Arrays.sort(indiArr, new Comparator<Individual>() {
			public int compare(Individual one, Individual two) {
				if (one.getFitness() > two.getFitness()) {
					return 1;
				}
				else if (one.getFitness() < two.getFitness()) {
					return -1;
				}
				else return 0;
			}
		});
		
		int cut = Math.min(CUTTHRESHOLD, indiArr.length);
		maintainSet.clear();
		for (int i = 0; i < cut; i++) {
			maintainSet.add(indiArr[i]);
		}
		
		iteration++;
	}
	
	public void beamSearch() {
		for (int i = 0; i < Config.N; i++) {
			_beamSearchHelper();
		}
	}
	
	public void printResult() {
		Iterator<Individual> it = maintainSet.iterator();
		Individual opt = it.next();
		int minFitness = opt.getFitness();
		while (it.hasNext()) {
			Individual indi = it.next();
			if (minFitness > indi.getFitness()) {
				minFitness = indi.getFitness();
				opt = indi;
			}
		}
		
		opt.printIndi();
	}
	
	public static void main(String[] args) {
		VRPBeamSearch app = new VRPBeamSearch();
		app.init();
		long start = System.currentTimeMillis();
		app.beamSearch();
		long end = System.currentTimeMillis();
		app.printResult();
		System.out.println("Elapsed: " + (end - start));
	}
}
