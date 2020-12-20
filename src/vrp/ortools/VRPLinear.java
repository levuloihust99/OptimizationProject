package vrp.ortools;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import com.google.ortools.Loader;
import com.google.ortools.linearsolver.MPConstraint;
import com.google.ortools.linearsolver.MPObjective;
import com.google.ortools.linearsolver.MPSolver;
import com.google.ortools.linearsolver.MPVariable;

import model.Config;

public class VRPLinear {
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
		deliveryTime = new int[N + 2*K];
		for (int i = 0; i < N; i++) {
			deliveryTime[i] = Integer.parseInt(deliveryTimeString[i]);
		}
		for (int i = 0; i < 2*K; i++) {
			deliveryTime[i + N] = 0; 
		}
		
		transferTime = new int[N + 2*K][N + 2*K];
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
		
		for (int k = 0; k < 2*K; k++) {
			for (int i = 0; i < N + 2*K; i++) {
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
		X = new MPVariable[N + 2*K][N + 2*K];
		Y = new MPVariable[N + 2*K];
		
		for (int i = 0; i < N + 2*K; i++) {
			for (int j = 0; j < N + 2*K; j++) {
				X[i][j] = model.makeIntVar(0, 1, "X[" + i + ", " + j + "]");
			}
		}
		
		for (int i = 0; i < N + 2*K; i++) {
			Y[i] = model.makeIntVar(0, maxCost, "Y[" + i + "]");
		}
		
		Z = model.makeIntVar(0, maxCost, "Z");
		
		// 0: abandon going to self
		MPConstraint selfAbandonConstraint = model.makeConstraint(0, 0);
		for (int i = 0; i < N + 2*K; i++) {
			selfAbandonConstraint.setCoefficient(X[i][i], 1);
		}
		
		// 1: real nodes
		MPConstraint[] inFlow = new MPConstraint[N];
		MPConstraint[] outFlow = new MPConstraint[N];
		for (int i = 0; i < N; i++) {
			inFlow[i] = model.makeConstraint(1, 1);
			outFlow[i] = model.makeConstraint(1, 1);
			for (int j = 0; j < N + 2*K; j++) {
				inFlow[i].setCoefficient(X[j][i], 1);
				outFlow[i].setCoefficient(X[i][j], 1);
			}
		}
		
		// 2: virtual left nodes
		MPConstraint[] leftMostOutFlow = new MPConstraint[K];
		MPConstraint leftMostInFlow = model.makeConstraint(0, 0);
		for (int kClone = 0; kClone < K; kClone++) {
			leftMostOutFlow[kClone] = model.makeConstraint(1, 1);
			for (int i = 0; i < N + 2*K; i++) {
				leftMostOutFlow[kClone].setCoefficient(X[kClone + N][i], 1);
				leftMostInFlow.setCoefficient(X[i][kClone + N], 1);
			}
		}
		
		// 3: virtual right nodes
		MPConstraint rightMostOutFlow = model.makeConstraint(0, 0);
		MPConstraint[] rightMostInFlow = new MPConstraint[K];
		for (int kClone = 0; kClone < K; kClone++) {
			rightMostInFlow[kClone] = model.makeConstraint(1, 1);
			for (int i = 0; i < N + 2*K; i++) {
				rightMostInFlow[kClone].setCoefficient(X[i][N + K + kClone], 1);
				rightMostOutFlow.setCoefficient(X[kClone + N + K][i], 1);
			}
		}
		
		// 4: virtual left nodes have accumulate equal to 0
		MPConstraint initAccumulateConstraint = model.makeConstraint(0, 0);
		for (int kClone = 0; kClone < K; kClone++) {
			initAccumulateConstraint.setCoefficient(Y[kClone + N], 1);
		}
		
		// 5: abandon sub-cycle
		MPConstraint[][] accumulateConstraintOne = new MPConstraint[N + 2*K][N + 2*K];
		MPConstraint[][] accumulateConstraintTwo = new MPConstraint[N + 2*K][N + 2*K];
		for (int i = 0; i < N + 2*K; i++) {
			for (int j = 0; j < N + 2*K; j++) {
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
		MPConstraint[] maximumConstraint = new MPConstraint[K];
		for (int kClone = 0; kClone < K; kClone++) {
			maximumConstraint[kClone] = model.makeConstraint(0, maxCost);
			maximumConstraint[kClone].setCoefficient(Z, 1);
			maximumConstraint[kClone].setCoefficient(Y[kClone + N + K], -1);
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
		for (int kClone = 0; kClone < K; kClone++) {
			int node = N + kClone;
			while (node < N + K) {
				System.out.printf("%d -> ", node);
				for (int i = 0; i < N + 2*K; i++) {
					if (X[node][i].solutionValue() == 1) {
						node = i;
						break;
					}
				}
			}
			System.out.printf("%d - Finish time: %d\n", node, (int)Y[node].solutionValue());
		}
		
//		System.out.println("-----------------------------------------------------");
//		for (int i = 0; i < N + 2*K; i++) {
//			System.out.printf(Y[i].name() + " = " + Y[i].solutionValue() + "\n");
//		}
		
		System.out.println("-----------------------------------------------------");
		System.out.println("Objective = " + Z.solutionValue() + "\n");
	}
	
	public void run(PrintStream pStr) {
		Loader.loadNativeLibraries();
		this.readData();
		this.stateModel();
		long start = System.currentTimeMillis();
		this.solve();
		long end = System.currentTimeMillis();
		pStr.println("Elapsed: " + (end - start));
		pStr.println("Objective: " + Z.solutionValue());
		pStr.println("-----------------------------------------------------");
	}
	
	public static void main(String[] args) {
		Loader.loadNativeLibraries();
		VRPLinear app = new VRPLinear();
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
