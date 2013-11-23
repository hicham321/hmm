import java.util.HashMap;

public class Backward {

	private static double[] initial = new double[3];
	private static double[][] transition = new double[3][3];
	private static HashMap<Integer, HashMap<Character, Double>> emission = new HashMap<Integer, HashMap<Character, Double>>();
	private static double[][] probs = new double[3][3];
	private static final String seq = new String("adb");

	private static void initHmm() {
		initial[0] = 0.33;
		initial[1] = 0.33;
		initial[2] = 0.34;

		transition[0][0] = 0.5;
		transition[0][1] = 0.3;
		transition[0][2] = 0.2;
		transition[1][0] = 0.2;
		transition[1][1] = 0.4;
		transition[1][2] = 0.4;
		transition[2][0] = 0.3;
		transition[2][1] = 0.5;
		transition[2][2] = 0.2;

		HashMap<Character, Double> col1 = new HashMap<Character, Double>();
		col1.put('a', 0.6);
		col1.put('b', 0.1);
		col1.put('c', 0.2);
		col1.put('d', 0.1);
		emission.put(0, col1);

		HashMap<Character, Double> col2 = new HashMap<Character, Double>();
		col2.put('a', 0.3);
		col2.put('b', 0.4);
		col2.put('c', 0.1);
		col2.put('d', 0.2);
		emission.put(1, col2);

		HashMap<Character, Double> col3 = new HashMap<Character, Double>();
		col3.put('a', 0.1);
		col3.put('b', 0.2);
		col3.put('c', 0.3);
		col3.put('d', 0.4);
		emission.put(2, col3);
	}

	private static void start() {
		for (int i = 0; i < probs.length; ++i) {
			probs[i][probs[0].length - 1] = 1;
		}
	}

	private static void recurse() {
		for (int i = probs[0].length - 2; i >= 0; --i) {
			for (int t = 0; t < probs.length; ++t) {
				double sum = 0.0;
				for (int tPrime = 0; tPrime < probs.length; ++tPrime) {
					double result = probs[tPrime][i + 1] * transition[t][tPrime]
							* emission.get(tPrime).get(seq.charAt(i + 1));
					sum += result;
				}
				probs[t][i] = sum;
			}
		}
	}

	public static void main(String[] args) {
		initHmm();
		start();
		recurse();
		for (int i = 0; i < probs.length; ++i) {
			for (int j = 0; j < probs[0].length; ++j) {
				System.out.print(probs[i][j]);
				System.out.print(" ");
			}
			System.out.println();
		}
	}
}