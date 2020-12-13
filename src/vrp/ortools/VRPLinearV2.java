package vrp.ortools;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import com.google.ortools.Loader;
import com.google.ortools.linearsolver.MPConstraint;
import com.google.ortools.linearsolver.MPObjective;
import com.google.ortools.linearsolver.MPSolver;
import com.google.ortools.linearsolver.MPVariable;

import model.Config;

public class VRPLinearV2 {
	MPSolver model;
	MPVariable[][] X;
	MPVariable[] Y;
	MPVariable Z;
	
	private static final long M = Integer.MAX_VALUE;
	private int N;
	private int K;
	private int[] deliveryTime;
	private int[][] transferTime;
	private long maxCost;
	
	public void readData() {
		String content = null;
		try {	
			byte[] bytes = Files.readAllBytes(Paths.get(Config.fileName));
			content = new String(bytes, StandardCharsets.UTF_8);
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		
		String[] lines = content.split("(\n)|(\r\n)");
		String[] configLine = lines[0].split(" ");
		N = Integer.parseInt(configLine[0]);
		K = Integer.parseInt(configLine[1]);
		
		String[] deliveryTimeString = lines[1].split(" ");
		deliveryTime = new int[N + 2];
		for (int i = 0; i < N + 2; i++) {
			if (i < N)
				deliveryTime[i] = Integer.parseInt(deliveryTimeString[i]);
			else deliveryTime[i] = 0;
		}
		
		transferTime = new int[N + 2][N + 2];
		int[][] cache = new int[N + 1][N + 1];
		for (int i = 0; i <= N; i++) {
			String[] lineSplit = lines[i + 2].split(" ");
			for (int j = 0; j <= N; j++) {
				cache[i][j] = Integer.parseInt(lineSplit[j]);
				maxCost += cache[i][j];
			}
		}
		
		for (int i = 0; i < N; i++) {
			for (int j = 0; j < N; j++) {
				transferTime[i][j] = cache[i + 1][j + 1];
			}
		}
		
		for (int k = 0; k < 2; k++) {
			for (int i = 0; i < N + 2; i++) {
				if (i < N) {
					transferTime[N + k][i] = cache[0][i + 1];
					transferTime[i][N + k] = cache[i + 1][0];
				}
				else {
					transferTime[N + k][i] = 0;
					transferTime[i][N + k] = 0;
				}
			}
		}
	}
	
	public void stateModel() {
		model = new MPSolver("VRPSchedule", MPSolver.OptimizationProblemType.valueOf("SAT_INTEGER_PROGRAMMING"));
//		model = new MPSolver("VRPSchedule", MPSolver.OptimizationProblemType.valueOf("CBC_MIXED_INTEGER_PROGRAMMING"));
		X = new MPVariable[N + 2][N + 2];
		Y = new MPVariable[N + 2];
		
		for (int i = 0; i < N + 2; i++) {
			for (int j = 0; j < N + 2; j++) {
				X[i][j] = model.makeIntVar(0, 1, "X[" + i + ", " + j + "]");
			}
		}
		
		for (int i = 0; i < N + 2; i++) {
			Y[i] = model.makeIntVar(0, maxCost, "Y[" + i + "]");
		}
		
		Z = model.makeIntVar(0, maxCost, "Z");
		
		// 0: abandon going to self
		MPConstraint selfAbandonConstraint = model.makeConstraint(0, 0);
		for (int i = 0; i < N + 2; i++) {
			selfAbandonConstraint.setCoefficient(X[i][i], 1);
		}
		
		// 1: real nodes
		MPConstraint[] inFlow = new MPConstraint[N];
		MPConstraint[] outFlow = new MPConstraint[N];
		for (int i = 0; i < N; i++) {
			inFlow[i] = model.makeConstraint(1, 1);
			outFlow[i] = model.makeConstraint(1, 1);
			for (int j = 0; j < N + 2; j++) {
				inFlow[i].setCoefficient(X[j][i], 1);
				outFlow[i].setCoefficient(X[i][j], 1);
			}
		}
		
		// 2: virtual left nodes
		MPConstraint leftInFlow = model.makeConstraint(0, 0);
		MPConstraint leftOutFlow = model.makeConstraint(K, K);
		for (int i = 0; i < N + 2; i++) {
			leftInFlow.setCoefficient(X[i][N], 1);
			leftOutFlow.setCoefficient(X[N][i], 1);
		}
		
		// 3: virtual right nodes
		MPConstraint rightInFlow = model.makeConstraint(K, K);
		MPConstraint rightOutFlow = model.makeConstraint(0, 0);
		for (int i = 0; i < N + 2; i++) {
			rightInFlow.setCoefficient(X[i][N + 1], 1);
			rightOutFlow.setCoefficient(X[N + 1][i], 1);
		}
			
		// 4: virtual left nodes have accumulate equal to 0
		MPConstraint initAccumulateConstraint = model.makeConstraint(0, 0);
		initAccumulateConstraint.setCoefficient(Y[N], 1);
		
		// 5: abandon sub-cycle
		MPConstraint[][] accumulateConstraintOne = new MPConstraint[N + 2][N + 2];
		MPConstraint[][] accumulateConstraintTwo = new MPConstraint[N + 2][N + 2];
		for (int i = 0; i < N + 1; i++) {
			for (int j = 0; j < N + 1; j++) {
				accumulateConstraintOne[i][j] = model.makeConstraint(-2 * maxCost, 
						M - transferTime[i][j] - deliveryTime[j]);
				accumulateConstraintOne[i][j].setCoefficient(X[i][j], M);
				accumulateConstraintOne[i][j].setCoefficient(Y[i], 1);
				accumulateConstraintOne[i][j].setCoefficient(Y[j], -1);
				
				accumulateConstraintTwo[i][j] = model.makeConstraint(-2 * maxCost, 
						M + transferTime[i][j] + deliveryTime[j]);
				accumulateConstraintTwo[i][j].setCoefficient(X[i][j], M);
				accumulateConstraintTwo[i][j].setCoefficient(Y[i], -1);
				accumulateConstraintTwo[i][j].setCoefficient(Y[j], 1);
			}
		}
		
		// 6: Z is the maximum time finished
		MPConstraint[] maximumConstraint = new MPConstraint[N + 1];
		for (int i = 0; i < N + 1; i++) {
			maximumConstraint[i] = model.makeConstraint(-maxCost, M - transferTime[i][N + 1]);
			maximumConstraint[i].setCoefficient(Z, -1);
			maximumConstraint[i].setCoefficient(Y[i], 1);
			maximumConstraint[i].setCoefficient(X[i][N + 1], M);
			
		}
		
		// objective function
		MPObjective obj = model.objective();
		obj.setCoefficient(Z, 1);
	}
	
	public void solve() {
		MPSolver.ResultStatus status = model.solve();
		System.out.println("Status = " + status);
		System.out.println("-----------------------------------------------------");
	}
	
	public void printResult() {
		
		for (int i = 0; i < N; i++) {
			if (X[N][i].solutionValue() == 1) {
				System.out.printf("%d -> ", N);
				int node = i;
				int prev = -1;
				while (node != N + 1) {
					System.out.print(node + " -> ");
					for (int j = 0; j < N + 2; j++) {
						if (X[node][j].solutionValue() == 1) {
							prev = node;
							node = j;
							break;
						}
					}
				}
				System.out.println(node + " - Finish time: " + (Y[prev].solutionValue() + transferTime[prev][N + 1]));
			}
		}
		System.out.println("-----------------------------------------------------");
		
//		for (int i = 0; i < N + 2; i++) {
//			System.out.printf(Y[i].name() + " = " + Y[i].solutionValue() + "\n");
//		}
//		System.out.println("-----------------------------------------------------");
		
		System.out.println("Objective = " + Z.solutionValue());
		System.out.println("-----------------------------------------------------");
	}
	
	public void check() {
		for (int i = 0; i < N + 2; i++) {
			System.out.print(i + ". ");
			for (int j = 0; j < N + 2; j++) {
				System.out.printf("%3d ", transferTime[i][j]);
			}
			System.out.println();
			System.out.println("-----------------------------------------------------");
		}
	}
	
	public static void main(String[] args) {
		Loader.loadNativeLibraries();
		VRPLinearV2 app = new VRPLinearV2();
		app.readData();
		app.stateModel();
		long start = System.currentTimeMillis();
		app.solve();
		long end = System.currentTimeMillis();
		System.out.println("Time elapsed: " + (end - start));
		System.out.println("-----------------------------------------------------");
		app.printResult();
	}
}
