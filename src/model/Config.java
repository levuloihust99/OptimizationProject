package model;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Config {
	public static final String fileName = "data/VRP-N014-K006/B20000.ins";
	private static boolean flag = true;
	public static int K;
	public static int N;
	public static int[][] transferTime;
	public static int[] deliveryTime;
	
	private static void _runConfiguration() {
		String content = null;
		try {	
			byte[] bytes = Files.readAllBytes(Paths.get(fileName));
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
		deliveryTime = new int[N + 1];
		deliveryTime[0] = -1;
		for (int i = 0; i < N; i++) {
			deliveryTime[i + 1] = Integer.parseInt(deliveryTimeString[i]);
		}
		
		transferTime = new int[N + 1][N + 1];
		for (int i = 0; i < N + 1; i++) {
			String[] lineSplit = lines[i + 2].split(" ");
			for (int j = 0; j < N + 1; j++) {
				transferTime[i][j] = Integer.parseInt(lineSplit[j]);
			}
		}
	}
	
	public static void runConfiguration() {
		if (flag) {
			_runConfiguration();
			flag = false;
		}
	}
}
