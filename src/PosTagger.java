import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.IOUtils;

public class PosTagger {

	private static final String lexiconPath = "C:\\Users\\Thomas Zhang\\Documents\\Eclipse EE\\workspace\\cs446-hw6\\data\\HW6.lexicon.txt";
	private static final String trainPath = "C:\\Users\\Thomas Zhang\\Documents\\Eclipse EE\\workspace\\cs446-hw6\\data\\HW6.train.txt";
	private static final String outputPath = "C:\\Users\\Thomas Zhang\\Documents\\Eclipse EE\\workspace\\cs446-hw6\\output\\HW6.out.txt";
	private static final String outputHMMPath = "C:\\Users\\Thomas Zhang\\Documents\\Eclipse EE\\workspace\\cs446-hw6\\output\\HW6.outHmm.txt";

	private static String[] train = null;

	private static Map<String, HashMap<String, Double>> label2Words = new HashMap<String, HashMap<String, Double>>();
	private static Map<String, Double> labelCounts = new HashMap<String, Double>();

	private static List<HashMap<String, ArrayList<Integer>>> wordIndices = new ArrayList<HashMap<String, ArrayList<Integer>>>();

	private static Map<String, HashMap<String, Double>> transition = new HashMap<String, HashMap<String, Double>>();
	private static Map<String, HashMap<String, Double>> emission = new HashMap<String, HashMap<String, Double>>();
	private static Map<String, Double> initial = new HashMap<String, Double>();

	private static List<Map<String, List<Double>>> forwards = new ArrayList<Map<String, List<Double>>>();
	private static List<Map<String, List<Double>>> backwards = new ArrayList<Map<String, List<Double>>>();

	private static List<Map<String, List<Double>>> gammas = new ArrayList<Map<String, List<Double>>>();
	private static List<Map<String, HashMap<String, Double>>> xis = new ArrayList<Map<String, HashMap<String, Double>>>();

	private static final double epsilon = 0.01;

	public static double logAdd(double logX, double logY) {
		// 1. make X the max
		if (logY > logX) {
			double temp = logX;
			logX = logY;
			logY = temp;
		}
		// 2. now X is bigger
		if (logX == Double.NEGATIVE_INFINITY) {
			return logX;
		}
		// 3. how far "down" (think decibels) is logY from logX?
		// if it's really small (20 orders of magnitude smaller), then ignore
		double negDiff = logY - logX;
		if (negDiff < -20) {
			return logX;
		}
		// 4. otherwise use some nice algebra to stay in the log domain
		// (except for negDiff)
		return logX + java.lang.Math.log(1.0 + java.lang.Math.exp(negDiff));
	}

	private static void loadLexicon() throws Exception {
		System.out.println("loadLexicon()");

		BufferedReader br = new BufferedReader(new FileReader(lexiconPath));
		String line = null;
		while ((line = br.readLine()) != null) {
			String[] tokens = line.split("\\s+");
			String word = tokens[0];

			// for every label
			for (int i = 1; i < tokens.length; ++i) {
				String label = tokens[i];

				// update counts for word
				if (label2Words.get(label) == null) {
					label2Words.put(label, new HashMap<String, Double>());
				}
				HashMap<String, Double> wordMap = label2Words.get(label);
				if (wordMap.get(word) == null) {
					wordMap.put(word, 1.0);
				} else {
					wordMap.put(word, wordMap.get(word) + 1);
				}
				label2Words.put(label, wordMap);

				// update total word count
				if (labelCounts.get(label) == null) {
					labelCounts.put(label, 1.0);
				} else {
					labelCounts.put(label, labelCounts.get(label) + 1.0);
				}
			}
		}
		br.close();
	}

	private static void loadTrain() throws Exception {
		System.out.println("loadTrain()");

		FileInputStream fis = new FileInputStream(trainPath);
		String trainText = IOUtils.toString(fis);
		train = trainText.split("\\n"); // train is split by sentences
		fis.close();
	}

	private static void loadWordIndices() {
		System.out.println("loadWordIndices()");

		for (int i = 0; i < train.length; ++i) {
			wordIndices.add(new HashMap<String, ArrayList<Integer>>());
		}
		for (int i = 0; i < train.length; ++i) {
			HashMap<String, ArrayList<Integer>> indices = wordIndices.get(i);
			String[] words = train[i].split("\\s+");
			for (String word : words) {
				ArrayList<Integer> index = new ArrayList<Integer>();
				for (int j = 0; j < words.length; ++j) {
					if (words[j].equals(word)) {
						index.add(j);
					}
				}
				indices.put(word, index);
			}
			wordIndices.set(i, indices);
		}
	}

	private static void initialize() {
		initTransition();
		initEmission();
		initInitial();

		for (int i = 0; i < train.length; ++i) {
			initForward(i);
		}

		for (int i = 0; i < train.length; ++i) {
			initBackward(i);
		}

		for (int i = 0; i < train.length; ++i) {
			initGamma(i);
		}

		for (int i = 0; i < train.length; ++i) {
			initXi(i);
		}
	}

	private static void initTransition() {
		System.out.println("initTransition()");

		double prob = 1.0 / label2Words.keySet().size();
		for (String label : label2Words.keySet()) {
			transition.put(label, new HashMap<String, Double>());
		}
		for (String labelRow : label2Words.keySet()) {
			HashMap<String, Double> labelMap = transition.get(labelRow);
			for (String labelCol : label2Words.keySet()) {
				labelMap.put(labelCol, Math.log(prob));
			}
			transition.put(labelRow, labelMap);
		}
	}

	private static void initEmission() {
		System.out.println("initEmission()");

		for (String label : label2Words.keySet()) {
			emission.put(label, new HashMap<String, Double>());
		}
		for (String label : label2Words.keySet()) {
			HashMap<String, Double> wordMap = label2Words.get(label);
			HashMap<String, Double> eWordMap = emission.get(label);
			for (String word : wordMap.keySet()) {
				eWordMap
						.put(word, Math.log(wordMap.get(word) / labelCounts.get(label)));
			}
			emission.put(label, eWordMap);
		}
	}

	private static void initInitial() {
		System.out.println("initInitial()");

		double prob = 1.0 / label2Words.keySet().size();
		for (String label : label2Words.keySet()) {
			initial.put(label, Math.log(prob));
		}
	}

	private static void executeBaumWelch() throws Exception {
		System.out.println("executeBaumWelch()");

		double logLHPrev = 0.0;
		double logLHNext = 0.0;
		int numIter = 0;
		do {
			for (int i = 0; i < train.length; ++i) {
				startForward(i);
				recurseForward(i);
			}

			for (int i = 0; i < train.length; ++i) {
				startBackward(i);
				recurseBackward(i);
			}

			for (int i = 0; i < train.length; ++i) {
				computeGamma(i);
			}

			for (int i = 0; i < train.length; ++i) {
				computeXi(i);
			}

			updateTransition();
			updateEmission();
			updateInitial();

			double LH = 0.0;
			for (Map<String, List<Double>> forward : forwards) {
				double sentLH = 0.0;
				for (String label : forward.keySet()) {
					List<Double> wordArr = forward.get(label);
					sentLH = logAdd(sentLH, wordArr.get(wordArr.size() - 1));
				}
				LH = logAdd(LH, sentLH);
			}

			++numIter;
			logLHPrev = logLHNext;
			logLHNext = LH;
			System.out.println("logLHPrev = " + logLHPrev);
			System.out.println("logLHNext = " + logLHNext);
			System.out.println(numIter + " = " + (logLHNext - logLHPrev));

		} while ((logLHNext - logLHPrev) >= epsilon);
	}

	private static void initForward(int trainIdx) {
		Map<String, List<Double>> trellis = new HashMap<String, List<Double>>();
		String sent = train[trainIdx];
		String[] words = sent.split("\\s+");

		for (String label : label2Words.keySet()) {
			trellis.put(label, new ArrayList<Double>());
		}
		for (String label : label2Words.keySet()) {
			List<Double> wordArr = trellis.get(label);
			for (int i = 0; i < words.length; ++i) {
				wordArr.add(0.0);
			}
			trellis.put(label, wordArr);
		}

		forwards.add(trellis);
	}

	private static void startForward(int trainIdx) {
		Map<String, List<Double>> trellis = forwards.get(trainIdx);
		String sent = train[trainIdx];
		String[] words = sent.split("\\s+");

		for (String label : trellis.keySet()) {
			List<Double> wordArr = trellis.get(label);
			Double initProb = initial.get(label);
			Double emissProb = emission.get(label).get(words[0]);
			if (emissProb == null) {
				emissProb = 0.0;
			}
			wordArr.set(0, initProb + emissProb);
			trellis.put(label, wordArr);
		}

		forwards.set(trainIdx, trellis);
	}

	private static void recurseForward(int trainIdx) {
		Map<String, List<Double>> trellis = forwards.get(trainIdx);
		String sent = train[trainIdx];
		String[] words = sent.split("\\s+");

		for (int i = 1; i < words.length; ++i) {
			for (String label : trellis.keySet()) {
				List<Double> wordArr = trellis.get(label);
				double prob = 0.0;
				for (String pLabel : trellis.keySet()) {
					Double priorProb = trellis.get(pLabel).get(i - 1);
					Double transProb = transition.get(pLabel).get(label);
					Double emissProb = emission.get(label).get(words[i]);
					if (emissProb == null) {
						emissProb = 0.0;
					}
					prob = logAdd(prob, priorProb + transProb + emissProb);
				}
				wordArr.set(i, prob);
				trellis.put(label, wordArr);
			}
		}

		forwards.set(trainIdx, trellis);
	}

	private static void initBackward(int trainIdx) {
		Map<String, List<Double>> trellis = new HashMap<String, List<Double>>();
		String sent = train[trainIdx];
		String[] words = sent.split("\\s+");

		for (String label : label2Words.keySet()) {
			trellis.put(label, new ArrayList<Double>());
		}
		for (String label : label2Words.keySet()) {
			List<Double> wordArr = trellis.get(label);
			for (int i = 0; i < words.length; ++i) {
				wordArr.add(0.0);
			}
			trellis.put(label, wordArr);
		}
		
		backwards.add(trellis);
	}

	private static void startBackward(int trainIdx) {
		Map<String, List<Double>> trellis = backwards.get(trainIdx);
		String sent = train[trainIdx];
		String[] words = sent.split("\\s+");

		for (String label : trellis.keySet()) {
			List<Double> wordArr = trellis.get(label);
			wordArr.set(words.length - 1, Math.log(1.0));
			trellis.put(label, wordArr);
		}

		backwards.set(trainIdx, trellis);
	}

	private static void recurseBackward(int trainIdx) {
		Map<String, List<Double>> trellis = backwards.get(trainIdx);
		String sent = train[trainIdx];
		String[] words = sent.split("\\s+");

		for (int i = words.length - 2; i >= 0; --i) {
			for (String label : trellis.keySet()) {
				List<Double> wordArr = trellis.get(label);
				double prob = 0.0;
				for (String pLabel : trellis.keySet()) {
					double priorProb = trellis.get(pLabel).get(i + 1);
					double transProb = transition.get(label).get(pLabel);
					Double emissProb = emission.get(pLabel).get(words[i + 1]);
					if (emissProb == null) {
						emissProb = 0.0;
					}
					prob = logAdd(prob, priorProb + transProb + emissProb);
				}
				wordArr.set(i, prob);
				trellis.put(label, wordArr);
			}
		}

		backwards.set(trainIdx, trellis);
	}

	private static void initGamma(int trainIdx) {
		Map<String, List<Double>> gamma = new HashMap<String, List<Double>>();
		Map<String, List<Double>> fTrellis = forwards.get(trainIdx);
		String sent = train[trainIdx];
		String[] words = sent.split("\\s+");

		for (String label : fTrellis.keySet()) {
			gamma.put(label, new ArrayList<Double>());
		}
		for (String label : fTrellis.keySet()) {
			List<Double> wordArr = gamma.get(label);
			for (int i = 0; i < words.length; ++i) {
				wordArr.add(0.0);
			}
		}

		gammas.add(gamma);
	}

	private static void computeGamma(int trainIdx) {
		Map<String, List<Double>> gamma = gammas.get(trainIdx);
		Map<String, List<Double>> fTrellis = forwards.get(trainIdx);
		Map<String, List<Double>> bTrellis = backwards.get(trainIdx);
		String sent = train[trainIdx];
		String[] words = sent.split("\\s+");

		for (String label : gamma.keySet()) {
			List<Double> wordArr = gamma.get(label);
			for (int i = 0; i < words.length; ++i) {
				Double fNum = fTrellis.get(label).get(i);
				Double bNum = bTrellis.get(label).get(i);
				double num = fNum + bNum;
				double denom = 0.0;
				for (String pLabel : fTrellis.keySet()) {
					Double fDenom = fTrellis.get(pLabel).get(i);
					Double bDenom = bTrellis.get(pLabel).get(i);
					denom = logAdd(denom, fDenom + bDenom);
				}
				wordArr.set(i, num / denom);
			}
			gamma.put(label, wordArr);
		}

		gammas.set(trainIdx, gamma);
	}

	private static void initXi(int trainIdx) {
		Map<String, HashMap<String, Double>> xi = new HashMap<String, HashMap<String, Double>>();
		Map<String, List<Double>> fTrellis = forwards.get(trainIdx);

		for (String label : fTrellis.keySet()) {
			xi.put(label, new HashMap<String, Double>());
		}
		for (String label : fTrellis.keySet()) {
			HashMap<String, Double> labelMap = xi.get(label);
			for (String plabel : fTrellis.keySet()) {
				labelMap.put(plabel, 0.0);
			}
			xi.put(label, labelMap);
		}

		xis.add(xi);
	}

	private static void computeXi(int trainIdx) {
		Map<String, HashMap<String, Double>> xi = xis.get(trainIdx);
		Map<String, List<Double>> fTrellis = forwards.get(trainIdx);
		Map<String, List<Double>> bTrellis = backwards.get(trainIdx);
		String sent = train[trainIdx];
		String[] words = sent.split("\\s+");

		for (String label : xi.keySet()) {
			HashMap<String, Double> labelMap = xi.get(label);
			for (String pLabel : labelMap.keySet()) {
				double prob = 0.0;
				for (int i = 0; i < words.length - 1; ++i) {
					Double fNum = fTrellis.get(label).get(i);
					Double bNum = bTrellis.get(pLabel).get(i + 1);
					Double tNum = transition.get(label).get(pLabel);
					Double eNum = emission.get(pLabel).get(words[i + 1]);
					if (eNum == null) {
						eNum = 0.0;
					}
					double num = fNum + bNum + tNum + eNum;
					double denom = 0.0;
					for (String ppLabel : fTrellis.keySet()) {
						Double fDenom = fTrellis.get(ppLabel).get(i);
						Double bDenom = bTrellis.get(ppLabel).get(i);
						denom = logAdd(denom, fDenom + bDenom);
					}
					prob = logAdd(prob, num / denom);
				}
				labelMap.put(pLabel, prob);
			}
			xi.put(label, labelMap);
		}

		xis.set(trainIdx, xi);
	}

	private static void updateTransition() {
		System.out.println("updateTransition()");

		for (String label : transition.keySet()) {
			HashMap<String, Double> labelMap = transition.get(label);

			// get denominator
			double denom = 0.0;
			for (int trainIdx = 0; trainIdx < train.length; ++trainIdx) {
				String sent = train[trainIdx];
				String[] words = sent.split("\\s+");
				Map<String, List<Double>> gamma = gammas.get(trainIdx);
				for (int i = 0; i < words.length - 1; ++i) {
					denom = logAdd(denom, gamma.get(label).get(i));
				}
			}

			// get numerator
			for (String pLabel : labelMap.keySet()) {
				double num = 0.0;
				for (Map<String, HashMap<String, Double>> xi : xis) {
					num = logAdd(num, xi.get(label).get(pLabel));
				}
				labelMap.put(pLabel, num / denom);
			}

			transition.put(label, labelMap);
		}
	}

	private static void updateEmission() {
		System.out.println("updateEmission()");

		for (String label : emission.keySet()) {
			HashMap<String, Double> wordMap = emission.get(label);

			// get denominator
			double denom = 0.0;
			for (int trainIdx = 0; trainIdx < train.length; ++trainIdx) {
				String sent = train[trainIdx];
				String[] words = sent.split("\\s+");
				Map<String, List<Double>> gamma = gammas.get(trainIdx);
				for (int i = 0; i < words.length; ++i) {
					denom = logAdd(denom, gamma.get(label).get(i));
				}
			}

			// get numerator
			for (String word : wordMap.keySet()) {
				double num = 0.0;
				for (int trainIdx = 0; trainIdx < train.length; ++trainIdx) {
					HashMap<String, ArrayList<Integer>> indices = wordIndices
							.get(trainIdx);
					Map<String, List<Double>> gamma = gammas.get(trainIdx);

					ArrayList<Integer> index = indices.get(word);
					if (index != null) {
						for (int idx : index) {
							num = logAdd(num, gamma.get(label).get(idx));
						}
					}
				}
				wordMap.put(word, num / denom);
			}

			emission.put(label, wordMap);
		}
	}

	private static void updateInitial() {
		System.out.println("updateInitial()");

		for (String label : initial.keySet()) {
			double prob = 0.0;
			for (int trainIdx = 0; trainIdx < train.length; ++trainIdx) {
				Map<String, List<Double>> gamma = gammas.get(trainIdx);
				prob = logAdd(prob, gamma.get(label).get(0));
			}
			initial.put(label, prob);
		}
	}

	private static void printHMM() throws Exception {
		PrintWriter writer = new PrintWriter(outputHMMPath, "UTF-8");

		writer.println("Transition");
		for (String label : transition.keySet()) {
			writer.print(label + ": ");
			HashMap<String, Double> labelMap = transition.get(label);
			for (String pLabel : labelMap.keySet()) {
				writer.print(pLabel + "-" + labelMap.get(pLabel) + " ");
			}
			writer.println();
		}

		writer.println("Emission");
		for (String label : emission.keySet()) {
			writer.print(label + ": ");
			HashMap<String, Double> wordMap = emission.get(label);
			for (String word : wordMap.keySet()) {
				writer.print(word + "-" + wordMap.get(word) + " ");
			}
			writer.println();
		}

		writer.println("Initial");
		for (String label : initial.keySet()) {
			writer.print(label + "-" + initial.get(label) + " ");
		}

		writer.close();
	}

	public static void tag() throws Exception {
		System.out.println("tag()");

		BufferedReader br = new BufferedReader(new FileReader(trainPath));
		PrintWriter writer = new PrintWriter(outputPath, "UTF-8");
		String line = null;
		while ((line = br.readLine()) != null) {
			String[] words = line.split("\\s+");

			Map<String, ArrayList<Double>> trellis = new HashMap<String, ArrayList<Double>>();
			Map<String, ArrayList<String>> trellisTags = new HashMap<String, ArrayList<String>>();
			String[] tags = new String[words.length];

			// initialize trellis
			for (String label : label2Words.keySet()) {
				trellis.put(label, new ArrayList<Double>());
				trellisTags.put(label, new ArrayList<String>());
			}
			for (String label : label2Words.keySet()) {
				ArrayList<Double> sentProbs = trellis.get(label);
				ArrayList<String> sentTags = trellisTags.get(label);
				for (int i = 0; i < words.length; ++i) {
					sentProbs.add(0.0);
					sentTags.add("");
				}
				trellis.put(label, sentProbs);
				trellisTags.put(label, sentTags);
			}

			// start trellis
			for (String label : trellis.keySet()) {
				ArrayList<Double> sentProbs = trellis.get(label);
				Double initProb = initial.get(label);
				Double emisProb = emission.get(label).get(words[0]);
				if (emisProb == null) {
					emisProb = 0.0;
				}
				sentProbs.set(0, initProb * emisProb);
			}

			// recurse
			for (int i = 1; i < words.length; ++i) {
				for (String label : trellis.keySet()) {
					double maxProb = 0.0;
					String lastTag = "";
					for (String pLabel : trellis.keySet()) {
						Double priorProb = trellis.get(pLabel).get(i - 1);
						Double transProb = transition.get(pLabel).get(label);
						Double emisProb = emission.get(label).get(words[i]);
						if (emisProb == null) {
							emisProb = 0.0;
						}
						double result = priorProb * transProb * emisProb;
						if (result > maxProb) {
							maxProb = result;
							lastTag = pLabel;
						}
					}
					ArrayList<Double> sentProbs = trellis.get(label);
					sentProbs.set(i, maxProb);
					trellis.put(label, sentProbs);

					ArrayList<String> sentTags = trellisTags.get(label);
					sentTags.set(i, lastTag);
					trellisTags.put(label, sentTags);
				}
			}

			// recover
			double maxProb = 0.0;
			String lastTag = "";
			for (String label : trellis.keySet()) {
				double result = trellis.get(label).get(words.length - 1);
				if (result > maxProb) {
					maxProb = result;
					lastTag = label;
				}
			}
			tags[words.length - 1] = lastTag;

			for (int i = words.length - 2; i >= 0; --i) {
				lastTag = trellisTags.get(lastTag).get(i + 1);
				tags[i] = lastTag;
			}

			// write tags
			for (int i = 0; i < tags.length; ++i) {
				writer.print(words[i] + "_" + tags[i] + " ");
			}
			writer.println();
		}
		writer.close();
		br.close();
	}

	public static void main(String[] args) throws Exception {
		loadLexicon();
		loadTrain();
		loadWordIndices();
		initialize();

		executeBaumWelch();
		printHMM();
		tag();
	}
}