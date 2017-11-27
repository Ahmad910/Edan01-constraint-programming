include "globals.mzn";
int: n;
int: n_prefs;
array[1..n_prefs, 0..1] of int: prefs;

array[1..n] of var 1..n: positions;
var int: satisfies;

%output[show(prefs[2,0]), " ",show(prefs[2,1]), "\n"];

array[1..n_prefs] of var bool: countArray;
constraint positions[1] < positions[2];


constraint
  forall (i in 1..n_prefs) (
    let {
      int: a = prefs[i,0],
      int: b = prefs[i,1]
    } in
   %countArray[i] = (positions[b]-positions[a] == 1 \/ positions[a]-positions[b] == 1) \/ (positions[b]-positions[a] == 2 \/ positions[a]-positions[b] == 2)
   countArray[i] = (positions[b]-positions[a] == 1 \/ positions[a]-positions[b] == 1) 
   );

constraint sum (i in 1..n_prefs) (bool2int(countArray[i])) = satisfies;

%output["countArray: ", show(countArray), "\n"];
constraint alldifferent(positions);
  
solve :: int_search(positions, first_fail, indomain_max, complete)
  maximize satisfies;
output [
    "satisifes = ", show(satisfies), "\n"
];
