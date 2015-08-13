//  SwapMutation.java
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

package jmetal.operators.mutation;

import jmetal.core.Solution;
import jmetal.encodings.solutionType.PermutationSolutionType;
import jmetal.encodings.solutionType.ZeroPermutationSolutionType;
import jmetal.encodings.variable.Permutation;
import jmetal.util.Configuration;
import jmetal.util.JMException;
import jmetal.util.PseudoRandom;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * This class implements a swap mutation. The solution type of the solution
 * must be Permutation.
 */
public class ZeroPermBitFlipMutation extends Mutation{
  /**
   * Valid solution types to apply this operator
   */
  private static final List VALID_TYPES = Arrays.asList(ZeroPermutationSolutionType.class) ;

  private Double mutationProbability_ = null ;

  /**
   * Constructor
   */
  public ZeroPermBitFlipMutation(HashMap<String, Object> parameters) {
  	super(parameters) ;

  	if (parameters.get("probability") != null)
  		mutationProbability_ = (Double) parameters.get("probability") ;
  } // Constructor


  /**
   * Constructor
   */
  //public SwapMutation(Properties properties) {
  //  this();
  //} // Constructor

  /**
   * Performs the operation
   * @param probability Mutation probability
   * @param solution The solution to mutate
   * @throws JMException
   */
  public void doMutation(double probability, Solution solution) throws JMException {
    int permutation[] ;
    int permutationLength ;
	    if (solution.getType().getClass() == ZeroPermutationSolutionType.class) {


            if (PseudoRandom.randDouble() < probability) {

                permutationLength = ((Permutation) solution.getDecisionVariables()[0]).getLength();
                permutation = ((Permutation) solution.getDecisionVariables()[0]).vector_;


                int maxVal = solution.getProblem().getLength(1);
                int pos1 = PseudoRandom.randInt(0, permutationLength - 1);
                int newVal = PseudoRandom.randInt(0, maxVal);


                boolean existe = false;
                for(int i = 0; i < permutationLength; i++){
                    if(permutation[i] == newVal){

                        permutation[i] = 0;
                        existe = true;
                        break;
                    }
                }

                if(!existe) {
                    permutation[pos1] = newVal;
                }



            } // if
        } // if
	    else {
            Configuration.logger_.severe("SwapMutation.doMutation: invalid type. " +
                    "" + solution.getDecisionVariables()[0].getVariableType());

            Class cls = String.class;
            String name = cls.getName();
            throw new JMException("Exception in " + name + ".doMutation()");
        }
  } // doMutation

  /**
   * Executes the operation
   * @param object An object containing the solution to mutate
   * @return an object containing the mutated solution
   * @throws JMException
   */
  public Object execute(Object object) throws JMException {
    Solution solution = (Solution)object;

		if (!VALID_TYPES.contains(solution.getType().getClass())) {
			Configuration.logger_.severe("SwapMutation.execute: the solution " +
					"is not of the right type. The type should be 'Binary', " +
					"'BinaryReal' or 'Int', but " + solution.getType() + " is obtained");

			Class cls = String.class;
			String name = cls.getName();
			throw new JMException("Exception in " + name + ".execute()");
		} // if 

    
    this.doMutation(mutationProbability_, solution);
    return solution;
  } // execute  
} // SwapMutation
