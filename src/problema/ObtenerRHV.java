package problema;

import jmetal.core.Solution;
import jmetal.core.SolutionSet;
import jmetal.qualityIndicator.QualityIndicator;
import jmetal.qualityIndicator.util.MetricsUtil;
import jmetal.util.JMException;
import jmetal.util.NonDominatedSolutionList;

import java.io.File;
import java.io.IOException;

/**
 * Created by Cristiano on 04/08/2015.
 */
public class ObtenerRHV {

    /**
     */
    public static void main(String[] args) throws JMException, ClassNotFoundException, IOException {

        if(args.length < 2){
            System.out.println("Error, se necesita archivo de salida de func y archivo con frente de pareto real/estimado");
            return;
        }

        MetricsUtil metrics = new MetricsUtil();

        String fun = args[0];
        String frenteRealPath = args[1];

        SolutionSet frente = metrics.readSolutionSet(fun);

        QualityIndicator indicator = new QualityIndicator(new Problema(), frenteRealPath);

        System.out.println("RHV: " + (indicator.getHypervolume(frente) / indicator.getTrueParetoFrontHypervolume()));
        System.out.println("Spread: " + indicator.getSpread(frente));




    }
}
