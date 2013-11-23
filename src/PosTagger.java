import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.StringTokenizer;

import org.apache.commons.io.IOUtils;

public class PosTagger {

	private static final String lexiconPath = "C:\\Users\\Thomas Zhang\\Documents\\Eclipse EE\\workspace\\cs446-hw6\\data\\HW6.lexicon.txt";
	private static final String trainPath = "C:\\Users\\Thomas Zhang\\Documents\\Eclipse EE\\workspace\\cs446-hw6\\data\\HW6.train.txt";

	private static ArrayList<String> words = new ArrayList<String>();
	private static ArrayList<String> labels = new ArrayList<String>();
	private static String[] train = null;

	private static HashMap<String, HashMap<String, Double>> transition = new HashMap<String, HashMap<String, Double>>();
	private static HashMap<String, HashMap<String, Double>> emission = new HashMap<String, HashMap<String, Double>>();
	private static HashMap<String, Double> initial = new HashMap<String, Double>();

	private static HashMap<String, ArrayList<Double>> forward = new HashMap<String, ArrayList<Double>>();
	private static HashMap<String, ArrayList<Double>> backward = new HashMap<String, ArrayList<Double>>();
	private static HashMap<String, ArrayList<Double>> gamma = new HashMap<String, ArrayList<Double>>();
	private static HashMap<String, HashMap<String, ArrayList<Double>>> xi = new HashMap<String, HashMap<String, ArrayList<Double>>>();

	private static final double epsilon = 0.001;

	private static void loadLexicon() throws Exception {
		System.out.println("loadLexicon()");

		BufferedReader br = new BufferedReader(new FileReader(lexiconPath));
		String line = null;
		while ((line = br.readLine()) != null) {
			StringTokenizer st = new StringTokenizer(line);
			if (st.hasMoreTokens()) {
				String word = st.nextToken();
				if (!words.contains(word)) {
					words.add(word);
				}
			}
			while (st.hasMoreTokens()) {
				String label = st.nextToken();
				if (!labels.contains(label)) {
					labels.add(label);
				}
			}
		}
		br.close();
	}

	private static void initTransition() {
		System.out.println("initTransition()");

		double transProb = 1.0 / labels.size();
		for (String label : labels) {
			transition.put(label, new HashMap<String, Double>());
		}
		for (String key : transition.keySet()) {
			HashMap<String, Double> val = transition.get(key);
			for (String label : labels) {
				val.put(label, transProb);
			}
			transition.put(key, val);
		}
	}

	private static void initEmission() {
		System.out.println("initEmission()");

		double emissProb = 1.0 / words.size();
		for (String label : labels) {
			emission.put(label, new HashMap<String, Double>());
		}
		for (String key : emission.keySet()) {
			HashMap<String, Double> val = emission.get(key);
			for (String word : words) {
				val.put(word, emissProb);
			}
			emission.put(key, val);
		}
	}

	private static void initInitial() {
		System.out.println("initInitial()");

		double initProb = 1.0 / labels.size();
		for (String label : labels) {
			initial.put(label, initProb);
		}
	}

	private static void loadTrain() throws Exception {
		System.out.println("loadTrain()");

		FileInputStream fis = new FileInputStream(trainPath);
		String trainText = IOUtils.toString(fis);
		train = trainText.split("\\s");
		fis.close();
	}

	private static void initProbs(HashMap<String, ArrayList<Double>> probs) {
		System.out.println("initProbs()");

		for (String label : labels) {
			probs.put(label, new ArrayList<Double>());
		}
		for (String key : probs.keySet()) {
			ArrayList<Double> val = probs.get(key);
			for (int i = 0; i < train.length; ++i) {
				val.add(0.0);
			}
			probs.put(key, val);
		}
	}

	private static void initXi() {
		System.out.println("initXi()");

		for (String label : labels) {
			xi.put(label, new HashMap<String, ArrayList<Double>>());
		}
		for (String key : xi.keySet()) {
			HashMap<String, ArrayList<Double>> val = xi.get(key);
			for (String label : labels) {
				val.put(label, new ArrayList<Double>());
			}
			xi.put(key, val);
		}
		for (String key : xi.keySet()) {
			HashMap<String, ArrayList<Double>> val = xi.get(key);
			for (String innerKey : val.keySet()) {
				ArrayList<Double> innerVal = val.get(innerKey);
				for (int i = 0; i < train.length - 1; ++i) {
					innerVal.add(0.0);
				}
				val.put(innerKey, innerVal);
			}
			xi.put(key, val);
		}
	}

	private static void executeBaumWelch() {
		System.out.println("executeBaumWelch()");

		double logLH = Double.MAX_VALUE;
		while (logLH >= epsilon) {
			startForward();
			recurseForward();
			startBackward();
			recurseBackward();
			populateGamma();
			populateXi();
			estTransition();
			estEmission();
			estInitial();
			double LH = 0.0;
			for (String key : forward.keySet()) {
				ArrayList<Double> val = forward.get(key);
				LH += val.get(train.length - 1);
			}
			logLH = Math.log(LH);
		}
	}

	private static void startForward() {
		System.out.println("startForward()");

		for (String key : forward.keySet()) {
			ArrayList<Double> val = forward.get(key);
			val.set(0, initial.get(key) * emission.get(key).get(train[0]));
		}
	}

	private static void recurseForward() {
		System.out.println("recurseForward()");

		for (int i = 1; i < train.length; ++i) {
			for (String key : forward.keySet()) {
				ArrayList<Double> val = forward.get(key);
				double sum = 0.0;
				for (String keyPrime : forward.keySet()) {
					double priorProb = forward.get(keyPrime).get(i - 1);
					double transProb = transition.get(keyPrime).get(key);
					double emissProb = emission.get(key).get(train[i]);
					sum += priorProb * transProb * emissProb;
				}
				val.set(i, sum);
				forward.put(key, val);
			}
		}
	}

	private static void startBackward() {
		System.out.println("startBackward()");

		for (String key : backward.keySet()) {
			ArrayList<Double> val = backward.get(key);
			val.set(train.length - 1, 1.0);
		}
	}

	private static void recurseBackward() {
		System.out.println("recurseBackward()");

		for (int i = train.length - 2; i >= 0; --i) {
			for (String key : backward.keySet()) {
				ArrayList<Double> val = backward.get(key);
				double sum = 0.0;
				for (String keyPrime : backward.keySet()) {
					double priorProb = backward.get(keyPrime).get(i + 1);
					double transProb = transition.get(key).get(keyPrime);
					double emissProb = emission.get(keyPrime).get(train[i + 1]);
					sum += priorProb * transProb * emissProb;
				}
				val.set(i, sum);
				backward.put(key, val);
			}
		}
	}

	private static void populateGamma() {
		System.out.println("populateGamma()");

		for (String key : gamma.keySet()) {
			ArrayList<Double> val = gamma.get(key);
			for (int i = 0; i < train.length; ++i) {
				double num1 = forward.get(key).get(i);
				double num2 = backward.get(key).get(i);
				double num = num1 * num2;
				double denom = 0.0;
				for (String denomKey : forward.keySet()) {
					denom += forward.get(denomKey).get(i) * backward.get(denomKey).get(i);
				}
				val.set(i, num / denom);
			}
			gamma.put(key, val);
		}
	}

	private static void populateXi() {
		System.out.println("populateXi()");

		for (String key : xi.keySet()) {
			HashMap<String, ArrayList<Double>> val = xi.get(key);
			for (String innerKey : val.keySet()) {
				ArrayList<Double> innerVal = val.get(innerKey);
				for (int i = 0; i < train.length - 1; ++i) {
					double num1 = forward.get(key).get(i);
					double num2 = transition.get(key).get(innerKey);
					double num3 = emission.get(innerKey).get(i + 1);
					double num4 = backward.get(innerKey).get(i + 1);
					double num = num1 * num2 * num3 * num4;
					double denom = 0.0;
					for (String denomKey : forward.keySet()) {
						denom += forward.get(denomKey).get(i)
								* backward.get(denomKey).get(i);
					}
					innerVal.set(i, num / denom);
				}
				val.put(innerKey, innerVal);
			}
			xi.put(key, val);
		}
	}

	private static void estTransition() {
		System.out.println("estTransition()");

		for (String key : transition.keySet()) {
			HashMap<String, Double> val = transition.get(key);
			for (String innerKey : val.keySet()) {
				double num = 0.0;
				for (int i = 0; i < train.length - 1; ++i) {
					num += xi.get(key).get(innerKey).get(i);
				}
				double denom = 0.0;
				for (int i = 0; i < train.length - 1; ++i) {
					denom += gamma.get(key).get(i);
				}
				val.put(innerKey, num / denom);
			}
			transition.put(key, val);
		}
	}

	private static void estEmission() {
		System.out.println("estEmission()");

		for (String key : emission.keySet()) {
			HashMap<String, Double> val = emission.get(key);
			for (String innerKey : val.keySet()) {
				double num = 0.0;
				for (int i = 0; i < train.length; ++i) {
					if (train[i].equals(innerKey)) {
						num += gamma.get(key).get(i);
					}
				}
				double denom = 0.0;
				for (int i = 0; i < train.length; ++i) {
					denom += gamma.get(key).get(i);
				}
				val.put(innerKey, num / denom);
			}
			emission.put(key, val);
		}
	}

	private static void estInitial() {
		System.out.println("estInitial()");

		for (String key : initial.keySet()) {
			initial.put(key, gamma.get(key).get(0));
		}
	}

	public static void main(String[] args) throws Exception {
		loadLexicon();
		initTransition();
		initEmission();
		initInitial();

		loadTrain();
		initProbs(forward);
		initProbs(backward);
		initProbs(gamma);
		initXi();

		executeBaumWelch();
	}
}