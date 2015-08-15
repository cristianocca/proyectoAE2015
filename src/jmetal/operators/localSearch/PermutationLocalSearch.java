//  MutationLocalSearch.java
//
//  Author:
//       Antonio J. Nebro <antonio@lcc.uma.es>
//       Juan J. Durillo <durillo@lcc.uma.es>
//
//  Copyright (c) 2011 Antonio J. Nebro, Juan J. Durillo
//
//  This program is free software: you can redistribute it and/or modify
//  it under the terms of the GNU Lesser General Public License as published by
//  the Free Software Foundation, either version 3 of the License, or
//  (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful,
//  but WITHOUT ANY WARRANTY; without even the implied warranty of
//  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//  GNU Lesser General Public License for more details.
// 
//  You should have received a copy of the GNU Lesser General Public License
//  along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jmetal.operators.localSearch;

import jmetal.core.*;
import jmetal.encodings.variable.Permutation;
import jmetal.operators.mutation.Mutation;
import jmetal.util.JMException;
import jmetal.util.PseudoRandom;
import jmetal.util.comparators.DominanceComparator;
import jmetal.util.comparators.OverallConstraintViolationComparator;
import problema.Problema;

import java.util.*;

/**
 * This class implements an local search operator based in the use of a 
 * mutation operator. An archive is used to store the non-dominated solutions
 * found during the search.
 */
public class PermutationLocalSearch extends LocalSearch {


  private Problema problem_;
  private int improvementRounds = 1;
  private Comparator constraintComparator_ ;
  private Comparator dominanceComparator_ ;


  /**
  * Constructor.
  * Creates a new local search object.
  * @param parameters The parameters

  */
  public PermutationLocalSearch(HashMap<String, Object> parameters) {
  	super(parameters) ;
  	if (parameters.get("problem") != null) {
      Problem problem = (Problem) parameters.get("problem");
      if (!(problem instanceof Problema)) {
        throw new RuntimeException("Permutation local search solo puede ser usado con instancia de Problema");
      }
      problem_ = (Problema)problem;
    }

    if(parameters.get("improvementRounds")!=null){
      this.improvementRounds = ((Integer)parameters.get("improvementRounds")).intValue();
    }

    dominanceComparator_  = new DominanceComparator();
    constraintComparator_ = new OverallConstraintViolationComparator();
  } //Mutation improvement


  public Object execute(Object object) throws JMException {

    Solution mejorSol = new Solution((Solution)object); //Para mantener los mejores valores de forma dummy.
    Solution mutatedSolution;

    int best = 0;


    int indice, indiceFinal;

    for (int i = 0; i < problem_.cantCamiones; i++) {

      indice = i * problem_.capCamionesAprox;
      indiceFinal = indice + problem_.capCamionesAprox;


      mutatedSolution = new Solution(mejorSol);

      for(int j = 0; j < improvementRounds; j++){

        int pos1 = PseudoRandom.randInt(indice, indiceFinal-1);
        int pos2 = PseudoRandom.randInt(indice, indiceFinal-1);

        if(pos1 > pos2){
          int tmp = pos1;
          pos1 = pos2;
          pos2 = tmp;
        }


        int[] variables = ((Permutation)(mutatedSolution.getDecisionVariables()[0])).vector_;

        int tmp = variables[pos1];
        variables[pos1] = variables[pos2];
        variables[pos2] = tmp;

        problem_.evaluate(mutatedSolution);

        best = dominanceComparator_.compare(mutatedSolution, mejorSol);

        if (best == -1) { //es mejor
          //No hago nada, acepto el cambio.
          mejorSol = mutatedSolution;
          break;
        }
        else { //No es mejor, no hago nada

        }

      }

    }

    return mejorSol;

/**
    problem_.evaluate(mutatedSolution);
    best = dominanceComparator_.compare(mutatedSolution,solution);

    if (best == -1) // This is: Mutated is best
      solution = mutatedSolution;
    else if (best == 1) // This is: Original is best
      //delete mutatedSolution
      ;
    else // This is mutatedSolution and original are non-dominated
    {
    }


    return new Solution(solution);
 **/

  } // execute
  

  /**
   * Returns the number of evaluations maded
   */
  public int getEvaluations() {
    return 1;
  } // evaluations
} // MutationLocalSearch
