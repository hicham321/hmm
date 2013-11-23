import java.util.HashMap;

public class Viterbi {

	private static double[] initial = new double[3];
	private static double[][] transition = new double[3][3];
	private static HashMap<Integer, HashMap<Character, Double>> emission = new HashMap<Integer, HashMap<Character, Double>>();
	private static double[][] trellisProbs = new double[3][5];
	private static int[][] trellisTags = new int[3][5];
	private static final String seq = new String("adbac");
	private static final int[] tags = new int[5];

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
		for (int i = 0; i < trellisProbs.length; ++i) {
			trellisProbs[i][0] = initial[i] * emission.get(i).get(seq.charAt(0));
			trellisTags[i][0] = -1;
		}
	}

	private static void recurse() {
		for (int i = 1; i < trellisProbs[0].length; ++i) {
			for (int t = 0; t < trellisProbs.length; ++t) {
				double maxProb = 0.0;
				int lastTag = -1;
				for (int tPrime = 0; tPrime < trellisProbs.length; ++tPrime) {
					double result = trellisProbs[tPrime][i - 1] * transition[tPrime][t]
							* emission.get(t).get(seq.charAt(i));
					if (result > maxProb) {
						maxProb = result;
						lastTag = tPrime;
					}
				}
				trellisProbs[t][i] = maxProb;
				trellisTags[t][i] = lastTag;
			}
		}
	}

	private static void recover() {
		double maxProb = 0.0;
		int lastTag = -1;
		for (int t = 0; t < trellisProbs.length; ++t) {
			double result = trellisProbs[t][trellisProbs[0].length - 1];
			if (result > maxProb) {
				maxProb = result;
				lastTag = t;
			}
		}
		tags[4] = lastTag;

		for (int i = trellisProbs[0].length - 2; i >= 0; --i) {
			lastTag = trellisTags[lastTag][i + 1];
			tags[i] = lastTag;
		}
	}

	public static void main(String[] args) {
		initHmm();
		start();
		recurse();
		recover();
		for (int i = 0; i < trellisProbs[0].length; ++i) {
			System.out.print(tags[i] + " ");
		}
	}
}