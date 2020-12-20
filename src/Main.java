import java.io.PrintStream;

import model.Config;
import vrp.heuristics.*;

public class Main {
	private static int[] Narr = new int[] {6, 14, 49, 99, 499, 499, 999, 999, 999};
	private static int[] Karr = new int[] {2, 6, 9, 20, 20, 50, 20, 50, 100};
	
	public static void main(String[] args) throws Exception{
//		VRPGreedyIncrement app = new VRPGreedyIncrement();
//		PrintStream pStr = new PrintStream("logs/VRPGreedyIncrement.txt");
		VRPBeamSearch app = new VRPBeamSearch();
		PrintStream pStr = new PrintStream("logs/VRPBeamSearch.txt");
		for (int i = 0; i < Narr.length; i++) {
			int N = Narr[i];
			int K = Karr[i];
			String NString;
			if (N < 10) {
				NString = String.format("00%d", N);
			}
			else if (N < 100) {
				NString = String.format("0%d", N);
			}
			else {
				NString = String.format("%d", N);
			}
			
			String KString;
			if (K < 10) {
				KString = String.format("00%d", K);
			}
			else if (K < 100) {
				KString = String.format("0%d", K);
			}
			else {
				KString = String.format("%d", K);
			}
			String fileName = String.format("data/VRP-N%s-K%s/B100.ins", NString, KString);
			Config.runConfiguration(fileName);
			for (int j = 0; j < 10; j++) {
				pStr.println(fileName + "#" + j);
				System.out.println(fileName + "#" + j);
				app.run(pStr);
			}
		}
		pStr.close();
	}
}
