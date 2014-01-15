public class Logistic {

	private static double[][] W = new double[5][2];
	private static double[][] gradient = new double[5][2];
	private static double[][] x = new double[3][5];
	private static double[] y = new double[3];
	private static final int d = 4;
	private static final int K = 3;

	private static void init() {
		for (int i = 0; i < W.length; ++i) {
			for (int j = 0; j < W[0].length; ++j) {
				W[i][j] = 1;
			}
		}
		x[0][0] = 3.5;
		x[0][1] = 5.3;
		x[0][2] = 0.2;
		x[0][3] = -1.2;
		x[0][4] = 1;
		x[1][0] = 4.4;
		x[1][1] = 2.2;
		x[1][2] = 0.3;
		x[1][3] = 0.4;
		x[1][4] = 1;
		x[2][0] = 1.3;
		x[2][1] = 0.5;
		x[2][2] = 4.1;
		x[2][3] = 3.2;
		x[2][4] = 1;
		y[0] = 1;
		y[1] = 2;
		y[2] = 3;
	}

	private static double getProb(int xIdx, int k) {
		double numSum = 0.0;
		for (int i = 0; i < d; ++i) {
			numSum += W[i][k] * x[xIdx][i];
		}

		double denomSum = 0.0;
		for (int dk = 0; dk < K - 1; ++dk) {
			double innerSum = 0.0;
			for (int i = 0; i < d; ++i) {
				innerSum += W[i][dk] * x[xIdx][i];
			}
			denomSum += Math.exp(W[d][dk] + innerSum);
		}

		double num = Math.exp(W[d][k] + numSum);
		double denom = 1 + denomSum;
		double prob = num / denom;

		return prob;
	}

	private static double getGradient(int i, int k) {
		double sum = 0.0;
		for (int j = 0; j < x.length; ++j) {
			double xij = x[j][i];
			int yjyk = (y[j] == y[k]) ? 1 : 0;
			double prob = getProb(j, k);
			sum += xij * (yjyk - prob);
		}
		return sum;
	}

	public static void main(String[] args) {
		init();
		for (int i = 0; i < W.length; ++i) {
			for (int k = 0; k < W[0].length; ++k) {
				gradient[i][k] = getGradient(i, k);
			}
		}
		for (int i = 0; i < gradient.length; ++i) {
			for (int k = 0; k < gradient[0].length; ++k) {
				System.out.print(gradient[i][k] + " ");
			}
			System.out.println();
		}
	}
}