//  Permutation.java
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

package jmetal.encodings.variable;

import jmetal.core.Variable;
import jmetal.util.PseudoRandom;

/**
 * Class implementing a permutation of integer decision encodings.variable
 */
public class ZeroPermutation extends Permutation {


  /**
   * Constructor
   */
  public ZeroPermutation() {
    size_   = 0;
    vector_ = null;

  } //Permutation



  /**
   * Se rellenan con valores hasta limit (inclusive), y luego todo 0's
   */
  public ZeroPermutation(int size, int limit) {
    size_   = size;
    vector_ = new int[size_];

    java.util.ArrayList<Integer> randomSequence = new
            java.util.ArrayList<Integer>(size_);

    for (int i = 0; i <= limit; i++)
      randomSequence.add(i);

    java.util.Collections.shuffle(randomSequence);

    for (int i = limit + 1; i < size_; i++) {
      randomSequence.add(0);
    }



    for(int j = 0; j < randomSequence.size(); j++)
      vector_[j] = randomSequence.get(j);
  } // Constructor


  /**
   * Copy Constructor
   * @param permutation The permutation to copy
   */
  public ZeroPermutation(ZeroPermutation permutation) {
    size_   = permutation.size_;
    vector_ = new int[size_];

    System.arraycopy(permutation.vector_, 0, vector_, 0, size_);
  } //Permutation


  /**
   * Create an exact copy of the <code>Permutation</code> object.
   * @return An exact copy of the object.
   */
  public Variable deepCopy() {
    return new ZeroPermutation(this);
  } //deepCopy

  /**
   * Returns the length of the permutation.
   * @return The length
   */
  public int getLength(){
    return size_;
  } //getNumberOfBits

  /**
   * Returns a string representing the object
   * @return The string
   */
  public String toString(){
    String string ;

    string = "" ;
    for (int i = 0; i < size_ ; i ++)
      string += vector_[i] + " " ;

    return string ;
  } // toString
} // Permutation
