package utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Random;

public class DataGenerator {
	private static final int[] NVALUES = new int[] {6, 14, 49, 99, 499, 499, 999, 999, 999};
	private static final int[] KVALUES = new int[] {2, 6, 9, 20, 20, 50, 20, 50, 100};
	private static final int[] BOUNDS = new int[] {100, 200, 400, 500, 1000, 2000, 4000, 5000, 10000, 20000};
	private static final int NUMINSTANCE = 10;
	private static final Random r;
	
	static {
		r = new Random();
		r.setSeed(100);
	}
	
	private static void generate(int N, int K) {
		String nString;
		if (N < 10) {
			nString = String.format("00%d", N);
		}
		else if (N < 100) {
			nString = String.format("0%d", N);
		}
		else nString = String.format("%d", N);
		
		String kString;
		if (K < 10) {
			kString = String.format("00%d", K);
		}
		else if (K < 100) {
			kString = String.format("0%d", K);
		}
		else kString = String.format("%d", K);
		
		String dirName = String.format("VRP-N%s-K%s", nString, kString);
		File dir = new File(dirName);
		if (!dir.exists()) {
			dir.mkdir();
		}
		
		for (int i = 0; i < NUMINSTANCE; i++) {
			int bound = BOUNDS[i];
			String configLine = String.format("%d %d", N, K);
			int[] deliveryTime = new int[N];
			for (int j = 0; j < N; j++) {
				deliveryTime[j] = r.nextInt(bound) + 1;
			}
			int[][] transferTime = new int[N + 1][N + 1];
			for (int outer = 0; outer < N + 1; outer++) {
				for (int inner = 0; inner < N + 1; inner++) {
					if (outer == inner) transferTime[outer][inner] = 0;
					else transferTime[outer][inner] = r.nextInt(bound) + 1;
				}
			}
			
			ArrayList<String> deliveryTimeString = new ArrayList<>();
			for (int loop = 0; loop < deliveryTime.length; loop++) {
				deliveryTimeString.add(String.format("%s", deliveryTime[loop]));
			}
			String deliveryLine = String.join(" ", deliveryTimeString);
			
			ArrayList<String> transferTimeMatrix = new ArrayList<>();
			for (int outer = 0; outer < N + 1; outer++) {
				ArrayList<String> transferTimeString = new ArrayList<>();
				for (int inner = 0; inner < N + 1; inner++) {
					transferTimeString.add(String.format("%s", transferTime[outer][inner]));
				}
				transferTimeMatrix.add(String.join(" ", transferTimeString));
			}
			String transferMultiLine = String.join("\n", transferTimeMatrix);
			
			String outputString = String.join("\n", configLine, deliveryLine, transferMultiLine);
			
			PrintStream pStr = null;
			try {
				pStr = new PrintStream(String.format("data/%s/B%d.ins", dirName, bound));
				pStr.print(outputString);
				pStr.close();
			}
			catch (FileNotFoundException e) {
				e.printStackTrace();
			}
			
			System.out.println(String.format("%s/B%d.ins generated", dirName, bound));
		}
	}
	
	public static void main(String[] args) throws Exception {
		if (NVALUES.length != KVALUES.length) {
			throw new Exception("Length of NVALUES must be equal to length of KVALUES");
		}
		
		if (BOUNDS.length != NUMINSTANCE) {
			throw new Exception("Length of BOUNDS must be equal to length of NUMINSTANCE");
		}
		
		int length = NVALUES.length;
		for (int i = 0; i < length; i++) {
			int N = NVALUES[i];
			int K = KVALUES[i];
			generate(N, K);
		}
	}
}
