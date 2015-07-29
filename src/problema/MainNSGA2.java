package problema;

import jmetal.core.Algorithm;
import jmetal.core.Operator;
import jmetal.core.Problem;
import jmetal.core.SolutionSet;
import jmetal.metaheuristics.nsgaII.NSGAII;
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
public class MainNSGA2 {

    public static void main(String[] args) throws JMException, ClassNotFoundException, IOException {

        Datos datos;
        try{
            datos = Datos.cargarDatosDeArgs(args);
        }
        catch (Throwable t){
            System.out.println(t.getMessage());
            return;
        }


        Problem problem   ; // The problem to solve
        Algorithm algorithm ; // The algorithm to use
        Operator crossover ; // Crossover operator
        Operator  mutation  ; // Mutation operator
        Operator  selection ; // Selection operator
        HashMap parameters ; // Operator parameters
        QualityIndicator indicators = null; // Object to get quality indicators


        problem = new Problema(datos);
        algorithm = new NSGAII(problem);

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

        population.printFeasibleVAR("VAR_NSGA2");
        population.printFeasibleFUN("FUN_NSGA2");
        ((Problema)problem).imprimirSolucion("./SALIDA_NSGA2.txt", population);


    }
}
