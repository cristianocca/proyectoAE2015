package problema;

import jmetal.qualityIndicator.util.MetricsUtil;
import jmetal.util.JMException;
import jmetal.util.NonDominatedSolutionList;

import java.io.File;
import java.io.IOException;

/**
 * Created by Cristiano on 04/08/2015.
 */
public class ObtenerFrente {



    /**
     * Recibe una carpeta y nombre de archivo de salida
     * Y deja en la carpeta el mejor frente de pareto construido con todos los archivos.
     * Solo agarra aquellos archivos que contengan "fun" en su nombre.
     *
     * @param args
     * @throws JMException
     * @throws ClassNotFoundException
     * @throws IOException
     */
    public static void main(String[] args) throws JMException, ClassNotFoundException, IOException {

        if(args.length != 2){
            System.out.println("Error, se necesita carpeta y archivo de salida");
            return;
        }

        MetricsUtil metrics = new MetricsUtil();

        String carpeta = args[0];
        String salida = args[1];

        NonDominatedSolutionList frente = new NonDominatedSolutionList();

        File folder = new File(carpeta);
        File[] files = folder.listFiles();

        if (folder == null){
            System.out.println("No encontrado: " + carpeta);
            return;
        }

        for(File f : files){
            if(f.isFile() && f.getName().toLowerCase().contains("fun")){
                metrics.readNonDominatedSolutionSet(f.getAbsolutePath(), frente);
            }
        }

        frente.printFeasibleFUN(salida);
        System.out.println("Frente obtenido para: " + salida);




    }
}
