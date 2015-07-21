/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package test;
import java.io.*;
import java.util.HashMap;
import jmetal.core.*;
import jmetal.metaheuristics.nsgaII.*;
import jmetal.operators.crossover.CrossoverFactory;
import jmetal.operators.mutation.MutationFactory;
import jmetal.operators.selection.SelectionFactory;
import jmetal.qualityIndicator.QualityIndicator;
import jmetal.util.Configuration;
import jmetal.util.JMException;
import jmetal.util.comparators.CrowdingComparator;
import jmetal.util.comparators.FitnessAndCrowdingDistanceComparator;
import jmetal.util.comparators.FitnessComparator;
import jmetal.util.comparators.ObjectiveComparator;
import problema.datos.Datos;

/**
 *
 * @author Cristiano
 */
public class Test {
    
   
    
    public static Entrada cargarEntrada(String ruta){
        //deberia leer el archivo y cargar los datos

        int N = 30;
        int CBMin = 0;
        int[] C = new int[]{4170,4379,4780,2457,2091,2151,2977,4704,2190,336,2779,3147,2313,4088,2353,1186,4024,1861,3009,3361,399,1373,3580,1821,652,265,301,1364,4485,715};
        int[] CB = new int[]{63,98,51,38,50,89,74,18,12,94,77,28,90,46,53,42,63,64,96,79,52,5,37,73,64,55,31,36,66,45};    
        
        return new Entrada(N, C, CB, CBMin);
    }
    
    public static void imprimirSolucion(SolutionSet frentePareto, String path){
        try {
            /* Open the file */
            FileOutputStream fos   = new FileOutputStream(path)     ;
            OutputStreamWriter osw = new OutputStreamWriter(fos)    ;
            BufferedWriter bw      = new BufferedWriter(osw)        ;

            //Tengo hardcodeado el acceso a la estructura ya que ya conozco la estructura de la solucion
            //Pero idealmente deberia ser generico usando los datos como cantidad de objetivos, cantidad de variables etc..
            frentePareto.sort(new ObjectiveComparator(0));
            for (int i = 0; i < frentePareto.size(); i++){
              Solution s = frentePareto.get(i);
              bw.write(s.getDecisionVariables()[0].toString());
              bw.newLine();
              bw.write(String.valueOf(s.getObjective(0)));
              bw.newLine();
              bw.write(String.valueOf(-1*s.getObjective(1)));
              bw.newLine();
              bw.write("--------------------");
              bw.newLine();
            }

            /* Close the file */
            bw.close();
        }catch (IOException e) {
            Configuration.logger_.severe("Error acceding to the file");
        }
    }
        

    /**
     * @param args the command line arguments
     * @throws jmetal.util.JMException
     * @throws java.lang.ClassNotFoundException
     */
    public static void main(String[] args) throws JMException, ClassNotFoundException, FileNotFoundException {


        Problem   problem   ; // The problem to solve
        Algorithm algorithm ; // The algorithm to use
        Operator  crossover ; // Crossover operator
        Operator  mutation  ; // Mutation operator
        Operator  selection ; // Selection operator
        HashMap  parameters ; // Operator parameters
        QualityIndicator indicators = null; // Object to get quality indicators
        
        Entrada entrada = cargarEntrada("");
        
        problem = new Problema(entrada.N, entrada.C, entrada.CB, entrada.CBMin);
        
        algorithm = new NSGAII(problem);
        //algorithm = new ssNSGAII(problem);

        // Algorithm parameters
        algorithm.setInputParameter("populationSize",100);
        algorithm.setInputParameter("maxEvaluations",25000);

        // Mutation and Crossover for Real codification 
        parameters = new HashMap() ;
        parameters.put("probability", 0.75) ;
        crossover = CrossoverFactory.getCrossoverOperator("SinglePointCrossover", parameters);                   

        parameters = new HashMap() ;
        parameters.put("probability", 0.01) ;
        mutation = MutationFactory.getMutationOperator("BitFlipMutation", parameters);                    

        // Selection Operator 
        parameters = null ;
        selection = SelectionFactory.getSelectionOperator("BinaryTournament2", parameters) ;                           

        // Add the operators to the algorithm
        algorithm.addOperator("crossover",crossover);
        algorithm.addOperator("mutation",mutation);
        algorithm.addOperator("selection",selection);

        // Add the indicator object to the algorithm
        algorithm.setInputParameter("indicators", indicators) ;
        
        long initTime = System.currentTimeMillis();
        SolutionSet population = algorithm.execute();
        long elapsedTime = System.currentTimeMillis() - initTime;
        
        System.out.println("Tiempo total: " + elapsedTime/1000 + "s");
        
        
        imprimirSolucion(population, "solucion_jmetal.in");
        //population.printFeasibleVAR("VAR");
        //population.printFeasibleFUN("FUN");

        
    }
    
}
