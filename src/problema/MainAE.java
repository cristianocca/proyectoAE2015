package problema;

import jmetal.core.*;
import jmetal.metaheuristics.nsgaII.NSGAII;
import jmetal.metaheuristics.randomSearch.RandomSearch;
import jmetal.metaheuristics.spea2.SPEA2;
import jmetal.operators.crossover.CrossoverFactory;
import jmetal.operators.localSearch.LocalSearch;
import jmetal.operators.localSearch.MutationLocalSearch;
import jmetal.operators.mutation.MutationFactory;
import jmetal.operators.selection.SelectionFactory;
import jmetal.qualityIndicator.QualityIndicator;
import jmetal.util.JMException;
import problema.datos.*;
import test.*;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;

/**
 * Created by Cristiano on 27/07/2015.
 */
public class MainAE {

    public static void main(String[] args) throws JMException, ClassNotFoundException, IOException {

        Datos datos;
        try{
            datos = Datos.cargarDatosDeArgs(args);
        }
        catch (Throwable t){
            System.out.println(t.getMessage());
            return;
        }


        int popSize = 200;
        int maxEval = 45000;
        double crossProb = 0.75;
        double mutProb = 0.01;
        String algoritmo = "NSGA2";
        String salidaFun = "SALIDA_FUN.txt";
        String salidaVar = "SALIDA_VAR.txt";


        if(args.length >= 7){
            algoritmo = args[6];
        }

        if(args.length >= 8){
            popSize = Integer.parseInt(args[7]);
        }

        if(args.length >= 9){
            maxEval = Integer.parseInt(args[8]);
        }

        if(args.length >= 10){
            crossProb = Double.parseDouble(args[9]);
        }

        if(args.length >= 11){
            mutProb = Double.parseDouble(args[10]);
        }

        if(args.length >= 12){
            salidaFun = args[11];
        }

        if(args.length >= 13){
            salidaVar = args[12];
        }

        System.out.println("---- Parametros a utilizar ----");
        System.out.println("Algoritmo: " + algoritmo);
        System.out.println("Tam Pob: " + popSize);
        System.out.println("Max Eval: " + maxEval);
        System.out.println("Cross Prob: " + crossProb);
        System.out.println("Mut Prob: " + mutProb);
        System.out.println("Salidas: " + salidaFun + ", " + salidaVar);


        Problem problem   ; // The problem to solve
        Algorithm algorithm ; // The algorithm to use
        Operator crossover ; // Crossover operator
        Operator  mutation  ; // Mutation operator
        Operator  selection ; // Selection operator
        HashMap parameters ; // Operator parameters
        QualityIndicator indicators = null; // Object to get quality indicators

        problem = new Problema(datos);

        if(algoritmo.equalsIgnoreCase("NSGA2")){
            algorithm = new NSGAII(problem);
        }
        else if(algoritmo.equalsIgnoreCase("SPEA2")){
            algorithm = new SPEA2(problem);
            algorithm.setInputParameter("archiveSize",popSize/2);
        }
        else if (algoritmo.equalsIgnoreCase("RANDOM")){
            algorithm = new RandomSearch(problem);
        }
        else {
            System.out.println("Agoritmo invalido, opciones: NSGA2 y SPEA2");
            return;
        }



        // Algorithm parameters
        algorithm.setInputParameter("populationSize",popSize);
        algorithm.setInputParameter("maxEvaluations",maxEval);

        // Mutation and Crossover for Real codification
        parameters = new HashMap() ;
        parameters.put("probability", crossProb) ;
        crossover = CrossoverFactory.getCrossoverOperator("ZeroPMXCrossover", parameters);
        //crossover = CrossoverFactory.getCrossoverOperator("SinglePointCrossover", parameters);

        parameters = new HashMap() ;
        parameters.put("probability", mutProb) ;
        mutation = MutationFactory.getMutationOperator("ZeroPermBitFlipMutation", parameters);

        // Selection Operator
        parameters = null ;
        if (algoritmo.equalsIgnoreCase("NSGA2")) {
            selection = SelectionFactory.getSelectionOperator("BinaryTournament3", parameters) ;
        }
        else {
            selection = SelectionFactory.getSelectionOperator("BinaryTournament", parameters) ;
        }


        // Add the operators to the algorithm
        algorithm.addOperator("crossover",crossover);
        algorithm.addOperator("mutation",mutation);
        algorithm.addOperator("selection",selection);

/**
        //Agrego local search
        parameters = new HashMap() ;
        parameters.put("improvementRounds", 10) ;
        parameters.put("problem",problem) ;
        parameters.put("mutation",mutation) ;
        algorithm.addOperator("localSearch",new MutationLocalSearch(parameters));
**/


        long initTime = System.currentTimeMillis();
        SolutionSet population = algorithm.execute();
        long elapsedTime = System.currentTimeMillis() - initTime;



        Solution compromiso = ((Problema)problem).getSolucionDeCompromiso(population);


        System.out.println("Compromiso F1: " + compromiso.getObjective(0));
        System.out.println("Compromiso F2: " + compromiso.getObjective(1));
        System.out.println("Tiempo Algoritmo: " + elapsedTime/1000 + "s");

        population.printFeasibleFUN(salidaFun);
        population.printFeasibleVAR(salidaVar);


        ((Problema) problem).imprimirSolucion("SALIDA.txt", population);
        //SolutionSet compromisoSet = new SolutionSet(1);
        //compromisoSet.add(compromiso);
        //((Problema)problem).imprimirSolucion("SALIDA_COMPROMISO.txt", compromisoSet);



    }
}
