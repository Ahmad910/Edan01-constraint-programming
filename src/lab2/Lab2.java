package lab2;

import org.jacop.constraints.IfThenElse;
import org.jacop.constraints.PrimitiveConstraint;
import org.jacop.constraints.SumInt;
import org.jacop.constraints.XeqC;
import org.jacop.core.IntVar;
import org.jacop.core.Store;
import org.jacop.search.DepthFirstSearch;
import org.jacop.search.IndomainMin;
import org.jacop.search.Search;
import org.jacop.search.SelectChoicePoint;
import org.jacop.search.SimpleMatrixSelect;
import org.jacop.constraints.*;

public class Lab2 {
	public static void main(String[] args) {
		long start, end, result;
		start = System.currentTimeMillis();
		example(3);
		//myExample(6);
		end = System.currentTimeMillis();
		result = end - start;
		System.out.println("\nExecution time = " + result + " ms");
	}

	@SuppressWarnings("deprecation")
	public static void solve(int graph_size, int start, int n_dests, int[] dest, 
			int n_edges, int[] from, int[] to,
			int[] cost) {
		Store store = new Store();
		int[][] weights = new int[graph_size][graph_size];
		IntVar[][] paths = new IntVar[graph_size][graph_size];
		for (int i = 0; i < graph_size; i++) {
			for (int j = 0; j < graph_size; j++) {
				paths[i][j] = new IntVar(store, 0, 1);
				weights[i][j] = fillWeight(i, j, from, to, cost);
				if (weights[i][j] == 0) {
					XeqC eq = new XeqC(paths[i][j], 0);
					store.impose(eq);
				}
			}
		}
		SumInt startNodeRowConstraint = new SumInt(store, paths[start - 1], ">=", new IntVar(store, 1, 1));
		SumInt startNodeRowConstraint2 = new SumInt(store, paths[start - 1], "<=", 
				new IntVar(store, n_dests, n_dests));
		store.impose(startNodeRowConstraint);
		store.impose(startNodeRowConstraint2);
		//store.impose(new SumInt(store, getColumn(paths, start - 1), "==", new IntVar(store, 0, 0)));
		PrimitiveConstraint p1, p2, p3;
		IfThenElse y;
		for (int i = 0; i < graph_size; i++) {
			if (i != start - 1) {
				p1 = new SumInt(store, paths[i], "==", new IntVar(store, 0, 0));
				p2 = new SumInt(store, getColumn(paths, i), "==", new IntVar(store, 0, 0));
				p3 = new SumInt(store, paths[i], ">=", new IntVar(store, 0, 0));
				y = new IfThenElse(p2, p1, p3);
				store.impose(y);
				store.impose(new SumInt(store, getColumn(paths, i), "<=", new IntVar(store, 1, 1)));
			}
		}
		for (int i = 0; i < n_dests; i++) {
			store.impose(new SumInt(store, getColumn(paths, dest[i] - 1), "==", new IntVar(store, 1, 1)));
		}
		IfThen x;
		for (int i = 0; i < graph_size; i++) {
			for (int j = 0; j < graph_size; j++) {
				p1 = new XeqC(paths[i][j], 1);
				p2 = new XneqY(paths[i][j], paths[j][i]);
				x = new IfThen(p1, p2);
				store.impose(x);
			}
		}

		IntVar destCost = new IntVar(store, "Cost", 0, sum(cost));
		store.impose(new SumWeight(FromIntVarMatrixToVector(paths), FromIntMatrixToVector(weights), destCost));
		Search<IntVar> search = new DepthFirstSearch<IntVar>();
		SelectChoicePoint<IntVar> select = new SimpleMatrixSelect<IntVar>(paths, null, new IndomainMin<IntVar>());
		boolean result = search.labeling(store, select, destCost);
		if (result) {
			System.out.println("Solution : " + destCost);
			System.out.println("------------\nPrinting the paths:");
			System.out.print("   ");
			for(int i = 0; i < graph_size; i++){
				System.out.print(i + 1 + " ");
			}
			System.out.println();
			for (int i = 0; i < paths.length; i++) {
				System.out.print(i + 1 + "  ");
				for (int j = 0; j < paths[0].length; j++) {
					System.out.print(paths[i][j].value() + " ");
				}
				System.out.println();
			}
		} else {
			System.out.println("There is no solution!");
		}
	}

	public static IntVar[] getColumn(IntVar[][] matrix, int i) {
		IntVar[] col = new IntVar[matrix.length];
		for (int j = 0; j < matrix.length; j++) {
			col[j] = matrix[j][i];
		}
		return col;
	}

	public static int fillWeight(int f, int t, int[] from, int[] to, int[] cost) {
		for (int i = 0; i < from.length; i++) {
			if (f + 1 == from[i] && t + 1 == to[i]) {
				return cost[i];
			}
			if (t + 1 == from[i] && f + 1 == to[i]) {
				return cost[i];
			}
		}
		return 0;
	}

	public static int sum(int[] array) {
		int sum = 0;
		for (int i = 0; i < array.length; i++) {
			sum += array[i];
		}
		return sum;
	}

	public static IntVar[] FromIntVarMatrixToVector(IntVar[][] matrix) {
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

	public static int[] FromIntMatrixToVector(int[][] matrix) {
		int[] vector = new int[matrix.length * matrix[0].length];
		int index = 0;
		for (int i = 0; i < matrix.length; i++) {
			for (int j = 0; j < matrix[0].length; j++) {
				vector[index] = matrix[i][j];
				index++;
			}
		}
		return vector;
	}


	public static void example(int ex) {
		switch (ex) {
		case 1:
			int graph_size1 = 6;
			int start1 = 1;
			int n_dests1 = 1;
			int[] dest1 = { 6 };
			int n_edges1 = 7;
			int[] from1 = { 1, 1, 2, 2, 3, 4, 4 };
			int[] to1 = { 2, 3, 3, 4, 5, 5, 6 };
			int[] cost1 = { 4, 2, 5, 10, 3, 4, 11 };
			solve(graph_size1, start1, n_dests1, dest1, n_edges1, from1, to1, cost1);
			break;
		case 2:
			int graph_size2 = 6;
			int start2 = 1;
			int n_dests2 = 2;
			int[] dest2 = { 5, 6 };
			int n_edges2 = 7;
			int[] from2 = { 1, 1, 2, 2, 3, 4, 4 };
			int[] to2 = { 2, 3, 3, 4, 5, 5, 6 };
			int[] cost2 = { 4, 2, 5, 10, 3, 4, 11 };
			solve(graph_size2, start2, n_dests2, dest2, n_edges2, from2, to2, cost2);
			break;
		case 3:
			int graph_size3 = 6;
			int start3 = 1;
			int n_dests3 = 2;
			int[] dest3 = { 5, 6 };
			int n_edges3 = 9;
			int[] from3 = { 1, 1, 1, 2, 2, 3, 3, 3, 4 };
			int[] to3 = { 2, 3, 4, 3, 5, 4, 5, 6, 6 };
			int[] cost3 = { 6, 1, 5, 5, 3, 5, 6, 4, 2 };
			solve(graph_size3, start3, n_dests3, dest3, n_edges3, from3, to3, cost3);
			break;
		}
	}

	public static void myExample(int ex) {
		switch (ex) {
		case 1:
			int graph_size1 = 3;
			int start1 = 1;
			int n_dests1 = 1;
			int[] dest1 = { 3 };
			int n_edges1 = 3;
			int[] from1 = { 1, 1, 2 };
			int[] to1 = { 2, 3, 3 };
			int[] cost1 = { 1, 5, 2 };
			solve(graph_size1, start1, n_dests1, dest1, n_edges1, from1, to1, cost1);
			break;
		case 2:
			int graph_size2 = 4;
			int start2 = 1;
			int n_dests2 = 2;
			int[] dest2 = { 3, 4 };
			int n_edges2 = 3;
			int[] from2 = { 1, 2, 2 };
			int[] to2 = { 2, 3, 4 };
			int[] cost2 = { 1, 2, 3 };
			solve(graph_size2, start2, n_dests2, dest2, n_edges2, from2, to2, cost2);
			break;
		case 3:
			int graph_size3 = 3;
			int start3 = 1;
			int n_dests3 = 2;
			int[] dest3 = { 2, 3 };
			int n_edges3 = 2;
			int[] from3 = { 1, 1 };
			int[] to3 = { 2, 3 };
			int[] cost3 = { 5, 6 };
			solve(graph_size3, start3, n_dests3, dest3, n_edges3, from3, to3, cost3);
			break;
		case 4:
			int graph_size4 = 5;
			int start4 = 1;
			int n_dests4 = 2;
			int[] dest4 = { 4, 5 };
			int n_edges4 = 6;
			int[] from4 = { 1, 1, 2, 3, 3, 4 };
			int[] to4 = { 2, 4, 3, 4, 5, 5 };
			int[] cost4 = { 1, 3, 5, 20, 1, 9 };
			solve(graph_size4, start4, n_dests4, dest4, n_edges4, from4, to4, cost4);
			break;
		case 5:
			int graph_size5 = 6;
			int start5 = 1;
			int n_dests5 = 2;
			int[] dest5 = { 5, 6 };
			int n_edges5 = 8;
			int[] from5 = { 1, 1, 1, 2, 2, 2, 3, 4 };
			int[] to5 = {   2, 3, 6, 4, 5, 6, 5, 6 };
			int[] cost5 = { 6, 4, 14,2, 3, 7, 2, 4 };
			solve(graph_size5, start5, n_dests5, dest5, n_edges5, from5, to5, cost5);
			break;
		case 6: 
			int graph_size6 = 8;
			int start6 = 1;
			int n_dests6 = 4;
			int[] dest6 = {4, 5, 7, 8};
			int n_edges6 = 13;
			int[] from6 = {1, 1, 1, 2, 2, 2, 3,  3, 3, 4, 5, 6, 6};
			int[] to6 =   {2, 3, 7, 3, 5, 8, 4,  5, 6, 6, 6, 7, 8};
			int[] cost6=  {1, 4, 11,1, 6, 14,10, 4, 1, 1, 1, 1, 1};
			solve(graph_size6, start6, n_dests6, dest6, n_edges6, from6, to6, cost6);
		}
	}
}
