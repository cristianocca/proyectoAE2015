package problema;

import jmetal.core.Problem;
import jmetal.core.Solution;
import jmetal.core.SolutionSet;
import jmetal.core.Variable;
import jmetal.encodings.solutionType.IntSolutionType;
import jmetal.encodings.solutionType.PermutationSolutionType;
import jmetal.encodings.solutionType.ZeroPermutationSolutionType;
import jmetal.encodings.variable.Permutation;
import jmetal.encodings.variable.ZeroPermutation;
import jmetal.operators.mutation.MutationFactory;
import jmetal.util.JMException;
import jmetal.util.NonDominatedSolutionList;
import jmetal.util.PseudoRandom;
import jmetal.util.comparators.ObjectiveComparator;
import problema.datos.Datos;
import problema.datos.LlenadoInicial;
import problema.datos.Velocidad;

import java.io.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;

/**
 * Created by Cristiano on 27/07/2015.
 */
public class Problema extends Problem {

    public Datos datos;
    public int cantContenedores;
    public int cantCamiones;
    public int capCamiones;
    public int capCamionesAprox; //capCamiones + capCamiones / 2
    public static TiempoComparator tiempoComparator = new TiempoComparator();

    //Para instanciarlo sin datos.
    public Problema(){

        numberOfObjectives_ = 2;        //0: Min trayectoria, 1: Maximizar QoS
        solutionType_ = new IntSolutionType(this) ;
        problemName_        = "Recoleccion de basura";
    }

    public Problema(Datos datos){
        this.datos = datos;
        this.cantCamiones = datos.datosBasicos.cantidadCamiones;
        this.capCamiones = datos.datosBasicos.capacidadCamiones;
        this.capCamionesAprox = (this.capCamiones + this.capCamiones / 2);


        this.cantContenedores = datos.puntos.length;

        numberOfObjectives_ = 2;        //0: Min trayectoria, 1: Maximizar QoS

        solutionType_ = new ZeroPermutationSolutionType(this) ;

        numberOfVariables_  = 1;

        length_ = new int[2];
        length_[0] = this.capCamionesAprox  * this.cantCamiones;
        length_[1] = this.cantContenedores-1;



        problemName_        = "Recoleccion de basura";

    }



    //Devuelve un puntaje segun la cantidad de basura dejada en el contenedor
    public static double getPuntajeNoRecogido(double porcentaje){


        double mult;
        if(porcentaje < 40){
            mult = 1;
        }
        else if(porcentaje < 80){
            mult = 2;
        }
        else {
            mult = 4;
        }


        return porcentaje * mult;



    }

    @Override
    public void evaluate(Solution solution) {

        //******************************************************************************************************************
        //******** Variables que voy a necesitar ************
        //******************************************************************************************************************

        int[] variables = ((Permutation)solution.getDecisionVariables()[0]).vector_;

        int tiempoRecol = this.datos.datosBasicos.tiempoRecoleccionContenedor;

        float[][] distancias = this.datos.distancias;
        int[][] tiempos = this.datos.tiempos;
        int indice = 0;
        int indiceFinal = 0;


        LlenadoInicial[] b = this.datos.llenados;   //basura inicial
        Velocidad[] velocidades = this.datos.velocidades;   //velocidades de llenado


        //******************************************************************************************************************
        //******** Restricciones y correccion de soluciones ************
        //******************************************************************************************************************

        boolean violoCond;
        int tiempoMax = 0;
        int ultimoContenedor = -1;

        //Itero hasta que la solucion sea factible
        do {

            violoCond = false;


            //La cantidad de basura recogida por cada camión no puede exceder su capacidad real
            //Para cada camión, luego que se tiene un 0 en su solución, todos los restantes valores a la derecha también deberán ser 0
            for (int i = 0; i < this.cantCamiones; i++) {

                indice = i * this.capCamionesAprox;
                indiceFinal = indice + this.capCamionesAprox;

                try {

                    //pongo todos los ceros lo mas a la derecha posible, y no dejo ceros entre medio.
                    for (int j = indice; j < indiceFinal; j++) {

                        if (variables[j] == 0) {
                            //si tengo un cero, muevo de derecha a izquierda todo lo que no sea cero
                            int indice3 = j + 1;

                            //Hago una especie de selection sort, dejando todos los 0's a la derecha.
                            while (indice3 < indiceFinal) {
                                if (variables[indice3] != 0 ) {
                                    int temp = variables[j];
                                    variables[j] = variables[indice3];
                                    variables[indice3] = temp;
                                    break;
                                }
                                indice3++;
                            }
                        }

                    }

                    //La cantidad de basura recogida por cada camión no puede exceder su capacidad real, para esto se utiliza el % de basura recogido de cada contenedor y se valida que el total no supere su capacidad.

                    double recogido = 0;        //double para manejar fracciones.
                    double sumaBasura;
                    int tiempo = 0;
                    int sumaTiempo = 0;
                    int actual = 0;

                    for(int j = indice; j < indiceFinal; j++){
                        int contenedor = variables[j];

                        if(contenedor != 0){
                            sumaTiempo = tiempo + tiempos[actual][contenedor];
                            sumaBasura = (b[contenedor].v + velocidades[contenedor].v * sumaTiempo) / 100;      //divido entre 100 para utilizar fracciones de contenedores.

                            if (recogido + sumaBasura <= this.capCamiones) {
                                tiempo = sumaTiempo + tiempoRecol;
                                recogido += sumaBasura;
                                actual = contenedor;
                                if(tiempo > tiempoMax){
                                    tiempoMax = tiempo;
                                    ultimoContenedor = contenedor;
                                }
                            }
                            else {
                                violoCond = true;

                                //Si un contenedor llena la capacidad del camion, elimino de derecha a izquierda hasta que no se supere la capacidad.
                                //Esto es, elimino el ultimo distinto de cero/dummy, y sigo la iteracion.
                                //Como seteo violoCond = true, se re calcula y si se sigue violando, se vuelve a corregir.
                                //En el 99% de los casos se corrige al eliminar un contenedor.
                                //Caso borde: El contenedor que sobrepasa es el ultimo, esta controlado de todas formas, ya que el for siguiente itera una sola vez
                                //Y la primer condicion da true, porque solo se entro aca con la misma condicion.
                                for (int z = indiceFinal - 1; z >= j; z--) {
                                    if (variables[z] != 0) {
                                        //Lo elimino
                                        variables[z] = 0;
                                        break;
                                    }
                                }


                                break; // y termino el loop de este camion

                            }
                        }
                        else {
                            break;  //si es cero, como estan ordenados con todos los ceros a la derecha, termino este camion.
                        }

                    }


                } catch (Throwable t) {
                    throw new RuntimeException("ERROR: " + t.getMessage(), t);
                }
            }

        }while(violoCond);

        if(ultimoContenedor != -1){
            tiempoMax+=tiempos[ultimoContenedor][0];    //Sumo tiempo desde el ultimo contenedor a la vuelta de ese camion.
        }


        //******************************************************************************************************************
        //******** Calculo de funciones objetivo ************
        //******************************************************************************************************************

        double f1 = 0;

        // -- Primer funcion objetivo --


        for(int i = 0; i < this.cantCamiones; i++){

            indice = i * this.capCamionesAprox;
            indiceFinal = indice + this.capCamionesAprox;
            int actual = 0;
            try {
                for (int j = indice; j < indiceFinal; j++) {
                    if (variables[j] != 0) {
                        f1 += distancias[actual][variables[j]];
                        actual = variables[j];
                    } else {
                        break;
                    }
                }

                if (actual != 0) {
                    f1 += distancias[actual][0];  //Distancias del ultimo contenedor al origen
                }
            }
            catch (Throwable t){
                throw t;
            }

        }


        // -- Segunda funcion objetivo ---

        //Veo que contenedores son recogidos
        double f2 = 0;

        boolean[] recogidos = new boolean[this.cantContenedores];
        for(int i = 0; i < this.cantContenedores; i++){
            recogidos[i] = false;
        }

        for(int i = 0; i < variables.length; i++) {
            int contenedor = variables[i];
            if (contenedor != 0) {
                recogidos[contenedor] = true;
            }
        }

        //Por ultimo sumo todos los contenedores no recogidos
        for (int i = 1; i < this.cantContenedores; i++) {
            if(!recogidos[i]) {
                f2 += getPuntajeNoRecogido(b[i].v + tiempoMax*velocidades[i].v);
            }

        }

        solution.setObjective(0, f1);
        solution.setObjective(1, f2);


    } // evaluate


    /**
     * Genera archivos con datos de la solucion/es.
     * @param path
     * @param soluciones
     * @throws IOException
     * @throws JMException
     */
    public void imprimirSolucion(String path, SolutionSet soluciones) throws IOException, JMException {


        int tiempoRecol = this.datos.datosBasicos.tiempoRecoleccionContenedor;

        // --- Calculo de primer funcion objetivo ----

        float[][] distancias = this.datos.distancias;
        int[][] tiempos = this.datos.tiempos;
        LlenadoInicial[] b = this.datos.llenados;
        Velocidad[] velocidades = this.datos.velocidades;

        FileOutputStream fos   = new FileOutputStream(path)     ;
        OutputStreamWriter osw = new OutputStreamWriter(fos)    ;
        BufferedWriter bw      = new BufferedWriter(osw)        ;

        //Elimino duplicados...
        SolutionSet hof = new NonDominatedSolutionList();
        for(int i = 0; i < soluciones.size();i++) {
            hof.add(soluciones.get(i));
        }


        //Tengo hardcodeado el acceso a la estructura ya que ya conozco la estructura de la solucion
        //Pero idealmente deberia ser generico usando los datos como cantidad de objetivos, cantidad de variables etc..
        hof.sort(new ObjectiveComparator(0));
        for (int i = 0; i < hof.size(); i++){
            Solution s = hof.get(i);

            if (s.getOverallConstraintViolation() == 0.0) {

                int[] variables = ((Permutation)s.getDecisionVariables()[0]).vector_;
                for (int j = 0; j < variables.length; j++) {

                    if (j % this.capCamionesAprox == 0 && j != 0) {
                        bw.write("  |  ");

                    }
                    else if (j != 0) {
                        bw.write(" ");
                    }

                    //String id = this.datos.puntos[(int)s.getDecisionVariables()[j].getValue()].id;
                    //bw.write(id);
                    bw.write(String.valueOf(variables[j]));
                }


                bw.newLine();
                bw.write(String.valueOf(s.getObjective(0)));
                bw.newLine();
                bw.write(String.valueOf(s.getObjective(1)));
                bw.newLine();

                int tiempoFinReal = 0;
                int ultimoContenedor = -1;

                //primero calculo tiempo de fin.

                for (int j = 0; j < this.cantCamiones; j++) {

                    int indice = j * this.capCamionesAprox;
                    int indiceFinal = indice + this.capCamionesAprox;

                    try {

                        int tiempo = 0;
                        int sumaTiempo = 0;
                        int actual = 0;

                        for(int k = indice; k < indiceFinal; k++){
                            int contenedor = variables[k];
                            if(contenedor != 0){
                                sumaTiempo = tiempo + tiempos[actual][contenedor];


                                if(sumaTiempo > tiempoFinReal){
                                    tiempoFinReal = sumaTiempo;
                                    ultimoContenedor = contenedor;
                                }

                                tiempo = sumaTiempo + tiempoRecol;
                                actual = contenedor;
                            }
                            else {
                                break;  //si es cero, como estan ordenados con todos los ceros a la derecha, termino este camion.
                            }

                        }


                    } catch (Throwable t) {
                        throw new RuntimeException("ERROR: " + t.getMessage(), t);
                    }
                }
                bw.write("Tiempo real en que es recolectado ultimo contenedor en h: " + String.valueOf(tiempoFinReal/60.0/60.0));
                bw.newLine();

                //Agrego tiempo de recoleccion y de vuelta
                if(ultimoContenedor != -1){
                    tiempoFinReal+=tiempoRecol + tiempos[ultimoContenedor][0];
                }
                bw.write("Tiempo real en que retorna el ultimo camion en h: " + String.valueOf(tiempoFinReal / 60.0 / 60.0));
                bw.newLine();
                bw.newLine();

                boolean[] recogidos = new boolean[this.cantContenedores];
                for(int j = 0; j < this.cantContenedores; j++){
                    recogidos[j] = false;
                }

                for (int j = 0; j < this.cantCamiones; j++) {

                    int indice = j * this.capCamionesAprox;
                    int indiceFinal = indice + this.capCamionesAprox;

                    try {
                        int tiempo = 0;
                        int sumaTiempo = 0;
                        int actual = 0;

                        for(int k = indice; k < indiceFinal; k++){
                            int contenedor = variables[k];
                            if(contenedor != 0){
                                sumaTiempo = tiempo + tiempos[actual][contenedor];

                                bw.write(String.format("Contenedor [%s] Recogido: al %s %%", contenedor, b[contenedor].v + velocidades[contenedor].v * sumaTiempo));

                                tiempo = sumaTiempo + tiempoRecol;
                                actual = contenedor;

                                bw.newLine();
                                recogidos[contenedor] = true;

                            }
                            else {
                                break;  //si es cero, como estan ordenados con todos los ceros a la derecha, termino este camion.
                            }

                        }


                    } catch (Throwable t) {
                        throw new RuntimeException("ERROR: " + t.getMessage(), t);
                    }
                }

                for (int j = 1; j < this.cantContenedores; j++) {
                    if(!recogidos[j]) {
                        bw.write(String.format("Contenedor [%s] no recogido, dejado en %s %%", j,b[j].v + velocidades[j].v * tiempoFinReal));
                        bw.newLine();
                    }

                }

                bw.write("--------------------");
                bw.newLine();
            }
        }

        /* Close the file */
        bw.close();
    }

    /**
     * Devuelve la solucion de compromiso. Esto es, la que esta mas cerca de los mejores valores extremos.
     * @param soluciones
     * @return
     */
    public Solution getSolucionDeCompromiso(SolutionSet soluciones){
        double bestF1 = Double.MAX_VALUE;   //Asigno peor valor que puede tomar f1
        double bestF2 = Double.MAX_VALUE;   //idem f2. Siempre se minimiza...

        for(int i = 0; i < soluciones.size();i++) {
            Solution sol = soluciones.get(i);
            if(sol.getObjective(0) < bestF1){
                bestF1 = sol.getObjective(0);
            }

            if (sol.getObjective(1) < bestF2){
                bestF2 = sol.getObjective(1);
            }
        }

        //tengo los mejores valores objetivos, busco la solucion que este mas cerca
        //En mi caso estoy buscando el x tal que min ||f(x)-z|| siendo z el optimo.
        //uso la normal euclidiana... : ||b - a||_2 = sqrt((b1-a1)^2 + (b2-a2)^2 + ....)
        //Ademas tengo que normalizar los valores antes para que de bien la distancia.
        double largo = Math.sqrt(Math.pow(bestF1, 2) + Math.pow(bestF2, 2));
        bestF1 = bestF1 / largo;
        bestF2 = bestF2 / largo;

        Solution best = null;
        double bestNorm = Double.MAX_VALUE;
        for(int i = 0; i < soluciones.size();i++) {
            Solution sol = soluciones.get(i);

            double f1 = sol.getObjective(0);
            double f2 = sol.getObjective(1);
            double largof = Math.sqrt(Math.pow(f1, 2) + Math.pow(f2, 2));
            f1 = f1 / largof;
            f2 = f2 / largof;

            double euclid = Math.sqrt( Math.pow(f1-bestF1, 2) + Math.pow(f2-bestF2, 2) );
            if (euclid < bestNorm){
                bestNorm = euclid;
                best = sol;
            }
        }

        return best;



    }


    //Deforma una solucion
    public Solution deformarSolucion(Solution sol) throws JMException {

        int[] variables = ((Permutation)sol.getDecisionVariables()[0]).vector_;
        int indice;
        int indiceFinal;

        boolean eliminar = false;
        if(PseudoRandom.randDouble() <= 0.2) {
            eliminar = true;
        }

        for (int i = 0; i < this.cantCamiones; i++) {

            indice = i * this.capCamionesAprox;
            indiceFinal = indice + this.capCamionesAprox;

            try {

                //Por cada camion, deformo la mitad de sus contenedores

                for(int j = indice; j < indiceFinal; j++){


                    if(PseudoRandom.randDouble()<= 0.5) {

                        int contenedor = variables[j];

                        //Una opcion, elimina el contenedor
                        if(eliminar) {

                            if (contenedor != 0) {
                                variables[j] = 0;
                            }
                        }

                        //La otra, mueve contenedores a otros camiones de derecha a izquierda
                        else {
                            if (contenedor != 0) {
                                for (int z = variables.length - 1; z >= 0; z--) {
                                    if(z < indice || z >= indiceFinal) {
                                        int contenedor2 = variables[z];
                                        if (contenedor2 == 0) {
                                            variables[j] = contenedor2;
                                            variables[z] = contenedor;
                                            break;
                                        }
                                    }
                                }

                            }

                        }
                    }



                }


            } catch (Throwable t) {
                throw new RuntimeException("ERROR: " + t.getMessage(), t);
            }
        }

        this.evaluate(sol);
        this.evaluateConstraints(sol);
        return sol;
    }


    //Devuelve una lista de soluciones greedy, incluyendo la original y deformadas.
    //cant1: cantidad de soluciones greedy
    public Solution[] getSolucionesGreedy(int cant) throws JMException {

        Solution[] res = new Solution[cant];

        /**
        for(int i = 0; i < cant; i++){

            Solution solucionGreedy = new Solution(this, new Variable[]{new ZeroPermutation(length_[0], length_[1])});
            this.evaluate(solucionGreedy);
            this.evaluateConstraints(solucionGreedy);
            res[i] = solucionGreedy;
        }

        **/

        double[] params = {0.0, 0.05, 0.1, 0.15, 0.20, 0.25, 0.3, 0.35, 0.4, 0.45, 0.5};


        ZeroPermutation[] permGreedys = new ZeroPermutation[params.length];
        for(int i = 0; i < params.length; i++){
            permGreedys[i] = MainGreedy.ejecutarGreedy(this.datos, params[i]);
        }



        HashMap parameters = new HashMap() ;
        parameters.put("probability", 1.0) ;

        for(int i = 0; i < cant; i++){
            Solution solucionGreedy = null ;

            if (i < params.length) {
                solucionGreedy = new Solution(this, new Variable[]{new ZeroPermutation(permGreedys[i])});
            }
            else {
                solucionGreedy = new Solution(this, new Variable[]{new ZeroPermutation(permGreedys[i % params.length])});

                for(int j = 0; j < i; j++) {
                    MutationFactory.getMutationOperator("ZeroPermBitFlipMutation", parameters).execute(solucionGreedy);
                }

            }

            this.evaluate(solucionGreedy);
            this.evaluateConstraints(solucionGreedy);
            res[i] = solucionGreedy;
        }


        return res;
    }




}

class TiempoComparator implements Comparator<int[]> {

    @Override
    public int compare(int[] o1, int[] o2) {
        int v1 = o1[0];
        int v2 = o2[0];
        if(v1 < v2){
            return -1;
        }
        else if(v1 > v2){
            return 1;
        }
        else {
            return 0;
        }
    }
}
