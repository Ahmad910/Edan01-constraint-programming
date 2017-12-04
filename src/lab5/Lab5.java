package lab5;

import org.jacop.constraints.Constraint;
import org.jacop.constraints.ElementIntegerFast;
import org.jacop.constraints.LexOrder;
import org.jacop.constraints.SumInt;
import org.jacop.constraints.XgteqY;
import org.jacop.constraints.XplusYeqC;
import org.jacop.core.IntVar;
import org.jacop.core.Store;
import org.jacop.search.DepthFirstSearch;
import org.jacop.search.IndomainMin;
import org.jacop.search.MostConstrainedStatic;
import org.jacop.search.Search;
import org.jacop.search.SelectChoicePoint;
import org.jacop.search.SimpleMatrixSelect;
import org.jacop.search.SmallestDomain;

public class Lab5 {
	private static int ex = 3;

	public static void main(String[] args) {
		long start, end;
		start = System.currentTimeMillis();
		example(ex);
		end = System.currentTimeMillis();
		System.out.println("Execution time: " + (end - start));
	}

	public static void solve(int n, int n_commercial, int n_residetial, int[] point_distribution) {
		Store store = new Store();
		// The matrix to be filled with ones or zeros
		// one stands for a res, zero stands for a com
		IntVar[][] matrix = new IntVar[n][n];
		for (int i = 0; i < matrix.length; i++) {
			for (int j = 0; j < matrix[i].length; j++) {
				matrix[i][j] = new IntVar(store, 0, 1);
			}
		}

		// The total number of ones(residential) must be equal to
		// n_residential
		IntVar numberOfResInMatrix = new IntVar(store, n_residetial, n_residetial);
		Constraint con = new SumInt(store, vectorizeIntVar(matrix), "==", numberOfResInMatrix);
		store.impose(con);

		// every row or column in the matrix must be equal to an IntVar{0..5}
		// the vector residentialSum will contain the sum of every row in the
		// elements from index 0 to n - 1, and the sum of every column in the
		// elements from index n to 2 * n - 1
		IntVar[] residentialSum = new IntVar[2 * n];
		for (int i = 0; i < n; i++) {
			IntVar sumForRow = new IntVar(store, 0, n);
			con = new SumInt(store, matrix[i], "==", sumForRow);
			store.impose(con);
			residentialSum[i] = sumForRow;
			IntVar sumForColumn = new IntVar(store, 0, n);
			con = new SumInt(store, getColumn(matrix, i), "==", sumForColumn);
			store.impose(con);
			residentialSum[i + n] = sumForColumn;
		}

		// put the points for every row and column
		// indexes from 0 to n - 1 corresponds to rows
		// indexes from n to 2 * n - 1 corresponds to columns
		// The constraint ElementIntegerFast is used to
		// impose that point is equal to the residentialSum[i]:th index
		// in point_distribution
		IntVar[] points = new IntVar[2 * n];
		IntVar point;
		for (int i = 0; i < points.length; i++) {
			point = new IntVar(store, -(n * n * 2), (n * n * 2));
			con = new ElementIntegerFast(residentialSum[i], point_distribution, point, -1);
			store.impose(con);
			points[i] = point;
		}

		// Lexicographical order defines a total number on solutions
		// and therefore breaks symmetries.
		// it is used to not allowed the permutations of the same solutions
		store.impose(new LexOrder(matrix[0], matrix[1]));
		store.impose(new LexOrder(getColumn(matrix, 0), getColumn(matrix, 1)));
		for (int i = 1; i < n - 1; i++) {
			store.impose(new LexOrder(matrix[i], matrix[i + 1]));
			store.impose(new LexOrder(getColumn(matrix, i), getColumn(matrix, i + 1)));
		}
		//These constraints are imposed when we have a lot of nodes to visit 
		//and thus a lot of solutions. The solutions are classed to the upper 
		//left. It does't work in the example 2 since we don't have a lot 
		//of solutions there.
		if (ex >= 3) {
			IntVar sumForRow1, sumForRow2, sumForCol1, sumForCol2;
			for (int i = 0; i < n - 1; i++) {
				// for the rows
				sumForRow1 = new IntVar(store, 0, n);
				sumForRow2 = new IntVar(store, 0, n);
				con = new SumInt(store, matrix[i], "==", sumForRow1);
				store.impose(con);
				con = new SumInt(store, matrix[i + 1], "==", sumForRow2);
				store.impose(con);
				con = new XgteqY(sumForRow1, sumForRow2);
				store.impose(con);
				// for the columns
				sumForCol1 = new IntVar(store, 0, n);
				sumForCol2 = new IntVar(store, 0, n);
				con = new SumInt(store, getColumn(matrix, i), "==", sumForCol1);
				store.impose(con);
				con = new SumInt(store, getColumn(matrix, i + 1), "==", sumForCol2);
				store.impose(con);
				con = new XgteqY(sumForCol1, sumForCol2);
				store.impose(con);
			}
		}

		// maxPoints is the cost that we want
		IntVar maxPoints = new IntVar(store, -(n * n * 2), n * n * 2);
		con = new SumInt(store, points, "==", maxPoints);
		store.impose(con);

		// we minimize the points and impose that the minPoints + maxPoints = 0
		// this is done because the search process will be more efficient
		IntVar minPoints = new IntVar(store, -(n * n * 2), n * n * 2);
		store.impose(new XplusYeqC(maxPoints, minPoints, 0));

		Search<IntVar> search = new DepthFirstSearch<IntVar>();
		SelectChoicePoint<IntVar> select = new SimpleMatrixSelect<IntVar>(matrix, new SmallestDomain<IntVar>(),
				new MostConstrainedStatic<IntVar>(), new IndomainMin<IntVar>());
		boolean result = search.labeling(store, select, minPoints);
		if (result) {
			System.out.println("Solution: ");
			for (int i = 0; i < n; i++) {
				for (int j = 0; j < n; j++) {
					if (matrix[i][j].value() == 1) {
						System.out.print("R ");
					} else {
						System.out.print("C ");
					}
				}
				System.out.println();
			}
			System.out.println("Output cost= " + maxPoints.value() + ".");
		} else {
			System.out.println("No solution Found.");
		}
	}

	public static IntVar[] vectorizeIntVar(IntVar[][] matrix) {
		IntVar[] vector = new IntVar[matrix.length * matrix[0].length];
		int index = 0;
		for (int i = 0; i < matrix.length; i++) {
			for (int j = 0; j < matrix[0].length; j++) {
				vector[index] = matrix[i][j];
				index++;
			}
		}
		return vector;
	}

	public static IntVar[] getColumn(IntVar[][] matrix, int i) {
		IntVar[] col = new IntVar[matrix.length];
		for (int j = 0; j < matrix.length; j++) {
			col[j] = matrix[j][i];
		}
		return col;
	}

	public static void example(int exampleNumber) {
		switch (exampleNumber) {
		case 1:
			int n1 = 5;
			int n1_commercial = 10;
			int n1_residential = 15;
			int[] point_distribution1 = { -5, -4, -3, 3, 4, 5 };
			solve(n1, n1_commercial, n1_residential, point_distribution1);
			break;
		case 2:
			int n2 = 5;
			int n_commercial2 = 7;
			int n_residential2 = 18;
			int[] point_distribution2 = { -5, -4, -3, 3, 4, 5 };
			solve(n2, n_commercial2, n_residential2, point_distribution2);
			break;
		case 3:
			int n3 = 7;
			int n_commercial3 = 20;
			int n_residential3 = 29;
			int[] point_distribution3 = { -7, -6, -5, -4, 4, 5, 6, 7 };
			solve(n3, n_commercial3, n_residential3, point_distribution3);
			break;
		default:
			System.err.println("This example is not defined");
		}
	}
}
