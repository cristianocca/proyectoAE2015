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
import java.util.HashMap;

/**
 * Created by Cristiano on 27/07/2015.
 */
public class MainNSGA2 {

    public static void main(String[] args) throws JMException, ClassNotFoundException, FileNotFoundException {

        if(args.length != 7){
            System.out.println("Se necesitan 6 argumentos.");

            System.out.println("Path datosBasicos.json");
            System.out.println("Path puntos.json");
            System.out.println("Path velocidades.json");
            System.out.println("Path llenado_x.json, donde x se reemplazara por arg de instancias.");
            System.out.println("Instancias (para x) separadas por coma: ej: 1,2,3,4,5");
            System.out.println("Path distancias.json");
            System.out.println("Path tiempos.json");
            return;
        }

        System.out.println(args[0]);
        System.out.println(args[1]);
        System.out.println(args[2]);
        System.out.println(args[3]);
        System.out.print("Instancias: ");
        String[] instanciasArg = args[4].split(",");
        if(instanciasArg.length <= 0){
            System.out.println("Argumento de instancias debe ser al menos 1");
            return;
        }
        for(String s : instanciasArg){
            System.out.print(s + " ");
        }
        System.out.println();
        System.out.println(args[5]);
        System.out.println(args[6]);

        Datos datos = Datos.cargarDatos(args[0],args[1],args[2],args[5],args[6]);
        //Para probar cargo solo el primer argumento
        datos.cargarLlenados(args[3].replace("x",instanciasArg[0]));
        System.out.println("Total puntos: " + datos.puntos.length);


        //*************************
        //PARA PROBAR-----------
        //ROMPO LOS DATOS, REDUZCO LA CANTIDAD DE DATOS A ALGO CHICO
        Datos datos2 = new Datos();
        datos2.datosBasicos = new DatoBasico();
        int puntosPrueba = 3;
        datos2.datosBasicos.cantidadCamiones = 2;
        datos2.datosBasicos.capacidadCamiones = 2;
        datos2.datosBasicos.tiempoRecoleccionContenedor = datos.datosBasicos.tiempoRecoleccionContenedor;
        datos2.datosBasicos.tiempoTrabajo = datos.datosBasicos.tiempoTrabajo;


        datos2.puntos = new Punto[puntosPrueba];
        datos2.velocidades = new Velocidad[puntosPrueba];
        datos2.llenados = new LlenadoInicial[puntosPrueba];

        datos2.distancias = new float[puntosPrueba][puntosPrueba];
        datos2.tiempos = new float[puntosPrueba][puntosPrueba];
        for(int i=0; i < puntosPrueba; i++){
            datos2.puntos[i] = datos.puntos[i];
            datos2.velocidades[i] = datos.velocidades[i];
            datos2.llenados[i] = datos.llenados[i];

            for(int j = 0; j < puntosPrueba; j++){
                datos2.distancias[i][j] = datos.distancias[i][j];
                datos2.tiempos[i][j] = datos.distancias[i][j];
            }
        }

        datos = datos2;

        System.out.println("Total puntos prueba: " + datos.puntos.length);

        //*************************


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


    }
}
