package lab4;

import org.jacop.constraints.Cumulative;
import org.jacop.constraints.Diff2;
import org.jacop.constraints.Max;
import org.jacop.constraints.XplusCeqZ;
import org.jacop.constraints.XplusClteqZ;
import org.jacop.core.IntVar;
import org.jacop.core.Store;
import org.jacop.search.DepthFirstSearch;
import org.jacop.search.IndomainMin;
import org.jacop.search.MostConstrainedStatic;
import org.jacop.search.Search;
import org.jacop.search.SelectChoicePoint;
import org.jacop.search.SimpleSelect;
import org.jacop.search.SmallestDomain;

public class AutoRegressionFilter {
	private Store store;
	private int multiplicationDuration;
	private int additionDuration;
	private int numberOfMultiplicationOperations;
	private int numberOfAdditionOperations;
	private int totalNumberOfOperations;
	private String[] typeOfOperation;
	private int[] theLastNodes;
	private String multiplication;
	private String addition;
	private int numberOfAdders;
	private int numberOfMultipliers;
	private int[][] edges;
	private int[] operationDuration;

	public AutoRegressionFilter(int additionDuration, int multiplicationDuration, int numberOfAdditionOperations,
			int numberOfMultiplicationOperations, String[] typeOfOperation, int[] theLastNodes, int numberOfAdders,
			int numberOfMultipliers, int[][] edges) {
		this.additionDuration = additionDuration;
		this.multiplicationDuration = multiplicationDuration;
		this.numberOfAdditionOperations = numberOfAdditionOperations;
		this.numberOfMultiplicationOperations = numberOfMultiplicationOperations;
		this.totalNumberOfOperations = numberOfAdditionOperations + numberOfMultiplicationOperations;
		this.typeOfOperation = typeOfOperation;
		this.theLastNodes = theLastNodes;
		this.numberOfAdders = numberOfAdders;
		this.numberOfMultipliers = numberOfMultipliers;
		this.edges = edges;
		this.multiplication = "multiplcation";
		this.addition = "addition";
		operationDuration = new int[totalNumberOfOperations];
		for (int i = 0; i < operationDuration.length; i++) {
			if (typeOfOperation[i].equals(addition)) {
				operationDuration[i] = additionDuration;
			} else {
				operationDuration[i] = multiplicationDuration;
			}
		}
		store = new Store();
	}

	public void addConstraintsandSearchForASolution() {
		// comulative constraint takes four parameters
		// for the addition
		// startsForAddition is the first origin for the diff2 constraint
		IntVar[] startsForAddition = new IntVar[numberOfAdditionOperations];
		IntVar[] durationsForAddition = new IntVar[numberOfAdditionOperations];
		IntVar[] resourcesForAddition = new IntVar[numberOfAdditionOperations];
		IntVar limitForAddition = new IntVar(store, "limit", 1, numberOfAdders);
		// for the multiplication
		// startsForTheMultiplication is the first origin for the diff2
		// constraint
		IntVar[] startsForTheMultiplication = new IntVar[numberOfMultiplicationOperations];
		IntVar[] durationsForTheMultiplication = new IntVar[numberOfMultiplicationOperations];
		IntVar[] resourcesForTheMultiplication = new IntVar[numberOfMultiplicationOperations];
		IntVar limitForTheMultiplication = new IntVar(store, "limit", 1, numberOfMultipliers);

		// Origin2 for respectively addition and multiplication
		IntVar[] origin2ForAddition = new IntVar[numberOfAdditionOperations];
		IntVar[] oringin2ForMultiplication = new IntVar[numberOfMultiplicationOperations];
		// origin1 and origin2 for all the operations
		IntVar[] starts = new IntVar[totalNumberOfOperations];
		IntVar[] origin2 = new IntVar[totalNumberOfOperations];

		// fill the above IntVar vectors
		int multiplicationIndex = 0;
		int additionIndex = 0;
		for (int i = 0; i < totalNumberOfOperations; i++) {
			starts[i] = new IntVar(store, "start" + i, 0, 1000);
			if (typeOfOperation[i].equals(multiplication)) {
				startsForTheMultiplication[multiplicationIndex] = starts[i];
				durationsForTheMultiplication[multiplicationIndex] = new IntVar(store, "duration" + i,
						multiplicationDuration, multiplicationDuration);
				// each operation requires no more than one multiplier
				resourcesForTheMultiplication[multiplicationIndex] = new IntVar(store, "resources" + i, 1, 1);
				origin2[i] = new IntVar(store, "occupy" + i, numberOfAdders + 1, numberOfAdders + numberOfMultipliers);
				oringin2ForMultiplication[multiplicationIndex] = origin2[i];
				multiplicationIndex++;
			} else {
		
				startsForAddition[additionIndex] = starts[i];
				durationsForAddition[additionIndex] = new IntVar(store, "duration" + i, additionDuration,
						additionDuration);
				resourcesForAddition[additionIndex] = new IntVar(store, "resources" + i, 1, 1);
				origin2[i] = new IntVar(store, "occupy" + i, 1, numberOfAdders);
				origin2ForAddition[additionIndex] = origin2[i];
				additionIndex++;
			}
		}
		// imposing the constraints

		// The task in "from" need to be done before the task in "to" get
		// started
		int from;
		int to;
		for (int i = 0; i < edges.length; i++) {
			from = edges[i][0];
			to = edges[i][1];
			store.impose(new XplusClteqZ(starts[from], operationDuration[from], starts[to]));
		}

		IntVar clockCycles = new IntVar(store, "clock cycles", 0, 1000);
		IntVar[] theLastOperations = new IntVar[theLastNodes.length];

		for (int i = 0; i < theLastNodes.length; i++) {
			int endNode = theLastNodes[i];
			theLastOperations[i] = new IntVar(store, "The node in the end", 0, 1000);
			store.impose(new XplusCeqZ(starts[endNode], operationDuration[endNode], theLastOperations[i]));
		}
		store.impose(new Max(theLastOperations, clockCycles));

		store.impose(new Cumulative(startsForAddition, durationsForAddition, resourcesForAddition, limitForAddition));
		store.impose(new Cumulative(startsForTheMultiplication, durationsForTheMultiplication,
				resourcesForTheMultiplication, limitForTheMultiplication));

		store.impose(new Diff2(startsForAddition, origin2ForAddition, durationsForAddition, resourcesForAddition));
		store.impose(new Diff2(startsForTheMultiplication, oringin2ForMultiplication, durationsForTheMultiplication,
				resourcesForTheMultiplication));

		// select the variables using the size of their domains and using the
		// one which has the
		// most constraints
		SelectChoicePoint<IntVar> selectStart = new SimpleSelect<IntVar>(starts, new SmallestDomain<IntVar>(),
				new MostConstrainedStatic<IntVar>(),
				 new IndomainMin<IntVar>());
		SelectChoicePoint<IntVar> selectOrigin = new SimpleSelect<IntVar>(origin2, new SmallestDomain<IntVar>()
				, new MostConstrainedStatic<IntVar>(),
				 new IndomainMin<IntVar>());
		long start;
		long end = 0;
		boolean result;
		Search<IntVar> search = new DepthFirstSearch<IntVar>();
		start = System.currentTimeMillis();
		result = search.labeling(store, selectStart, clockCycles);
		if(result){
			search = new DepthFirstSearch<IntVar>();
			result = search.labeling(store, selectOrigin);
			end = System.currentTimeMillis();
		}
		if(result){
			System.out.println("Optimal: Yes");

		}else{
			System.out.println("Optimal: No");
		}
		System.out.println("Exection time: " + (end - start));
	}
	
	public static void main(String[] args) {
		int multiplicationDuration = 2; 
		int additionDuration = 1;
		
		int numberOfAdders = 1; 
		int numberOfMultipliers = 1;
		
		int numberOfMultiplicationOperations = 16; 
		int numberOfAdditionOperations = 12;
		int theLastNodes[] = { 26, 27 };
		String add = "addition";
		String mul = "multiplcation";
		
		String[] typeOfOperation = { mul, mul, mul, mul, mul, mul, mul, mul, add, add, add, add, add, add,
				mul, mul, mul, mul,
				add, add, mul, mul, mul, mul, add, add, add, add };

		int[][] edges = { { 0, 8 }, { 1, 8 }, { 2, 9 }, { 3, 9 }, { 4, 10 }, { 5, 10 }, { 6, 11 }, { 7, 11 }, { 8, 26 },
				{ 9, 12 }, { 10, 13 }, { 11, 27 }, { 12, 14 }, { 12, 16 }, 
				{ 13, 15 }, { 13, 17 }, { 14, 18 },
				{ 15, 18 }, { 16, 19 }, { 17, 19 }, { 18, 20 }, { 18, 22 }, 
				{ 19, 21 }, { 19, 23 }, { 20, 24 },
				{ 21, 24 }, { 22, 25 }, { 23, 25 }, { 24, 26 }, { 25, 27 } };
		
		AutoRegressionFilter regFilter = new AutoRegressionFilter(additionDuration, multiplicationDuration, numberOfAdditionOperations, numberOfMultiplicationOperations, typeOfOperation, theLastNodes, numberOfAdders, numberOfMultipliers, edges);
		regFilter.addConstraintsandSearchForASolution();
	}
}