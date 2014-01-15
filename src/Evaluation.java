import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Evaluation {

	private static final String outPath = "C:\\Users\\Thomas Zhang\\Desktop\\HW6.out.txt";
	private static final String goldPath = "C:\\Users\\Thomas Zhang\\Documents\\Eclipse EE\\workspace\\cs446-hw6\\data\\HW6.gold.txt";
	private static List<String> out = new ArrayList<String>();
	private static List<String> gold = new ArrayList<String>();
	
	private static HashMap<String, HashMap<String, Integer>> matrix = new HashMap<String, HashMap<String, Integer>>();

	private static double getAccuracy() {
		double cor = 0.0;
		for (int i = 0; i < out.size(); ++i) {
			if (out.get(i).equals(gold.get(i))) {
				++cor;
			}
		}
		return cor / out.size();
	}
	
	private static void initConfusionMatrix() {
		System.out.println("init()");
		
		for (String tag : out) {
			if (matrix.get(tag) == null) {
				matrix.put(tag, new HashMap<String, Integer>());
			}
		}
		for (String tag : matrix.keySet()) {
			HashMap<String, Integer> tagMap = matrix.get(tag);
			for (String pTag : matrix.keySet()) {
				tagMap.put(pTag, 0);	
			}
			matrix.put(tag, tagMap);
		}
	}
	
	private static void fillConfusionMatrix() {
		System.out.println("fill()");
		for (int i = 0; i < out.size(); ++i) {
			String outLabel = out.get(i);
			String goldLabel = gold.get(i);
			HashMap<String, Integer> tagMap = matrix.get(outLabel);
			tagMap.put(goldLabel, tagMap.get(goldLabel) + 1);
			matrix.put(outLabel, tagMap);
		}
	}

	private static void printConfusionMatrix() {
		System.out.println("print()");
		for (String label : matrix.keySet()) {
			System.out.print(label + "\t");
			HashMap<String, Integer> tagMap = matrix.get(label);
			for (String pLabel : tagMap.keySet()) {
				//System.out.print(tagMap.toString());
				int count = tagMap.get(pLabel);
				System.out.print(pLabel + "-" + count + "\t\t");
			}
			System.out.println();
		}
	}
	
	public static void main(String[] args) throws Exception {
		BufferedReader brOut = new BufferedReader(new FileReader(outPath));
		BufferedReader brGold = new BufferedReader(new FileReader(goldPath));
		String line;

		while ((line = brOut.readLine()) != null) {
			String[] tokens = line.split("\\s+");
			for (String token : tokens) {
				String[] sep = token.split("_");
				out.add(sep[1]);
			}
		}

		while ((line = brGold.readLine()) != null) {
			String[] tokens = line.split("\\s+");
			for (String token : tokens) {
				String[] sep = token.split("_");
				gold.add(sep[1]);
			}
		}

		brOut.close();
		brGold.close();

		double accuracy = getAccuracy();
		System.out.println(accuracy);
		
		initConfusionMatrix();
		fillConfusionMatrix();
		printConfusionMatrix();
	}
}