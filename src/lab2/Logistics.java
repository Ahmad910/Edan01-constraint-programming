package lab2;

import java.util.ArrayList;

import org.jacop.constraints.*;
import org.jacop.core.*;
import org.jacop.search.*;

public class Logistics {
	public static void main(String[] args) {
		long start, end, result;
		start = System.currentTimeMillis();
		example(3);
		end = System.currentTimeMillis();
		result = end - start;
		System.out.println("\nExecution time = " + result + " ms");
	}

	public static void solve(int graph_size, int start, int n_dests, int[] dest, int n_edges, int[] from, int[] to,
			int[] cost) {
		Store store = new Store();
		IntVar[][] graph_edges = new IntVar[n_dests][graph_size];
		for (int i = 0; i < n_dests; i++) {
			for (int j = 0; j < graph_size; j++) {
				graph_edges[i][j] = new IntVar(store, "edge(" + (i + 1) + ", " + (j + 1) + ")");
				if (j != (start - 1)) {
					if (j != dest[i] - 1) {
						graph_edges[i][j].addDom(j + 1, j + 1);
					} else {
						// bara destinationer kan g� tillbaka till start punkten
						graph_edges[i][j].addDom(start, start);
					}
				}
			}
		}

		// add all the edges, add edges in both directions
		for (int i = 0; i < n_dests; i++) {
			for (int j = 0; j < n_edges; j++) {
				graph_edges[i][to[j] - 1].addDom(from[j], from[j]);
				graph_edges[i][from[j] - 1].addDom(to[j], to[j]);
			}
			store.impose(new Subcircuit(graph_edges[i]));
		}
		
		// create boolean array
		BooleanVar[] chosenEdges = new BooleanVar[n_edges];
		for (int i = 0; i < n_edges; i++) {
			chosenEdges[i] = new BooleanVar(store);
		}

		// kostnad f�r en edge f�r bara r�knas en g�ng.
		for (int j = 0; j < n_edges; j++) {
			ArrayList<PrimitiveConstraint> constraintList = new ArrayList<PrimitiveConstraint>();
			for (int i = 0; i < n_dests; i++) {
				constraintList.add(new XeqC(graph_edges[i][from[j] - 1], to[j]));
				constraintList.add(new XeqC(graph_edges[i][to[j] - 1], from[j]));
			}
			store.impose(new Reified(new Or(constraintList), chosenEdges[j]));
		}

		IntVar destCost = new IntVar(store, "Cost", 0, sum(cost));
		store.impose(new SumWeight(chosenEdges, cost, destCost));

		Search<IntVar> search = new DepthFirstSearch<IntVar>();
		
		
		
	

		SelectChoicePoint<IntVar> select = new SimpleMatrixSelect<IntVar>(graph_edges, null, new IndomainMin<IntVar>());
		boolean result = search.labeling(store, select, destCost);
		/*
		 * System.out.println("Cost = " + totalCost.value());
		 * System.out.println("Edges used in the answer:"); for (int i = 0; i <
		 * edgeIncluded.length; i++) { if(edgeIncluded[i].value() == 1) {
		 * System.out.println(from[i] + "-" + to[i] + " cost(" + cost[i] + ")");
		 * } }
		 * 
		 * for(int i = 0; i < graph_size ; i++){
		 * System.out.println(edges[0][i]); System.out.println(edges[1][i]); }
		 */
	}
	public static int sum(int[] v) {
		int sum = 0;
		for (int i = 0; i < v.length; i++) {
			sum += v[i];
		}
		return sum;
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

}