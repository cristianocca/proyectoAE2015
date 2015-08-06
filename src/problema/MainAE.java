package problema;

import jmetal.core.*;
import jmetal.metaheuristics.nsgaII.NSGAII;
import jmetal.metaheuristics.spea2.SPEA2;
import jmetal.operators.crossover.CrossoverFactory;
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
            datos = Datos.cargarDatosDeArgs(args, false);
        }
        catch (Throwable t){
            System.out.println(t.getMessage());
            return;
        }


        int popSize = 100;
        int maxEval = 25000;
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
            algorithm.setInputParameter("archiveSize",popSize);
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
        crossover = CrossoverFactory.getCrossoverOperator("TwoPointsCrossover2", parameters);
        //crossover = CrossoverFactory.getCrossoverOperator("SinglePointCrossover", parameters);

        parameters = new HashMap() ;
        parameters.put("probability", mutProb) ;
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



        Solution compromiso = ((Problema)problem).getSolucionDeCompromiso(population);

        double peorF1 = Double.MAX_VALUE * -1;
        double mejorF1 = Double.MAX_VALUE;
        double peorF2 = Double.MAX_VALUE * -1;
        double mejorF2 = Double.MAX_VALUE;

        for(int i = 0; i < population.size();i++) {
            Solution sol = population.get(i);

            if(sol.getObjective(0) > peorF1){
                peorF1 = sol.getObjective(0);
            }

            if(sol.getObjective(0) < mejorF1){
                mejorF1 = sol.getObjective(0);
            }

            if (sol.getObjective(1) > peorF2){
                peorF2 = sol.getObjective(1);
            }

            if (sol.getObjective(1) < mejorF2){
                mejorF2 = sol.getObjective(1);
            }
        }


        System.out.println("Peor F1: " + peorF1);
        System.out.println("Mejor F1: " + mejorF1);
        System.out.println("Peor F2: " + peorF2);
        System.out.println("Mejor F2: " + mejorF2);
        System.out.println("Compromiso F1: " + compromiso.getObjective(0));
        System.out.println("Compromiso F2: " + compromiso.getObjective(1));
        System.out.println("Tiempo Algoritmo: " + elapsedTime/1000 + "s");

        population.printFeasibleFUN(salidaFun);
        population.printFeasibleVAR(salidaVar);


        //((Problema) problem).imprimirSolucion("SALIDA.txt", population);
        //SolutionSet compromisoSet = new SolutionSet(1);
        //compromisoSet.add(compromiso);
        //((Problema)problem).imprimirSolucion("SALIDA_COMPROMISO.txt", compromisoSet);



    }
}
