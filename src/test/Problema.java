/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package test;

import jmetal.core.Problem;
import jmetal.core.Solution;
import jmetal.encodings.solutionType.BinarySolutionType;
import jmetal.encodings.variable.Binary;
import jmetal.util.PseudoRandom;

/**
 *
 * @author Cristiano
 */
public class Problema extends Problem {
    private final int N;
    private final int[] C;
    private final int[] CB;
    private final int CBMin;
    
 
    /***
     * Instancia el problema
     * @param N Cantidad de antenas
     * @param C Costo de cada antena
     * @param CB Cobertura de cada antena
     * @param CBMin Cobertura minima buscada
     */
    public Problema(int N, int[] C, int[] CB, int CBMin){
        
        this.N = N;
        this.C = C;
        this.CB = CB;
        this.CBMin = CBMin;
        
        numberOfVariables_  = 1;    
        numberOfObjectives_ = 2;
        numberOfConstraints_= 0;
        problemName_        = "Prueba";

                
        solutionType_ = new BinarySolutionType(this) ;

        length_       = new int[]{N};        
        //lowerLimit_ = new double[]{0};
        //upperLimit_ = new double[]{1};
        
    }
    
    @Override
     public void evaluate(Solution solution) {
        Binary variable ;
        int costo;
        int cobertura;

        variable = ((Binary)solution.getDecisionVariables()[0]) ;

        while(true){
            costo = 0;
            cobertura = 0;
            for (int i = 0; i < variable.getNumberOfBits() ; i++){
                costo+=variable.getIth(i) ? this.C[i] : 0;
                cobertura+=variable.getIth(i) ? this.CB[i] : 0;
            }
            
            if (cobertura >= this.CBMin){
                //multiply by -1 to minimize
                solution.setObjective(0, costo);            
                solution.setObjective(1, -1*cobertura); 
                return;
            }
            else{
                variable.setIth(PseudoRandom.randInt(0,1), true);
                
            }

                       
        }
      } // evaluate

    
}
