package problema;

import jmetal.core.Problem;
import jmetal.core.Solution;
import jmetal.core.SolutionSet;
import jmetal.encodings.solutionType.IntSolutionType;
import jmetal.encodings.solutionType.PermutationSolutionType;
import jmetal.encodings.variable.Permutation;
import jmetal.util.JMException;
import jmetal.util.NonDominatedSolutionList;
import jmetal.util.comparators.ObjectiveComparator;
import problema.datos.Datos;
import problema.datos.LlenadoInicial;
import problema.datos.Velocidad;

import java.io.*;
import java.util.ArrayList;
import java.util.Comparator;

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


    public int indiceLimite;

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


        this.cantContenedores = datos.puntos.length-1;    //no incluye el origen. Se usa para ver si estoy viendo un contenedor valido o un valor dummy.

        numberOfObjectives_ = 2;        //0: Min trayectoria, 1: Maximizar QoS

        solutionType_ = new PermutationSolutionType(this) ;

        numberOfVariables_  = 1; //this.capCamionesAprox  * this.cantCamiones;

        length_ = new int[1];
        length_[0] = this.capCamionesAprox  * this.cantCamiones + this.cantContenedores; //saco 1 por el origen, se crean lugares adicionales para el limite dummy

        this.indiceLimite = this.cantCamiones * this.capCamionesAprox; //Indice a partir el cual no estoy viendo ningun camion. Inclusive

        problemName_        = "Recoleccion de basura";

    }



    //Devuelve le puntaje asociado segun el porcentaje de llenado, al momento de ser recogido
    public static double getPuntajeRecogido(double porcentaje){

        if(porcentaje < 20){
            return 0;
        }
        if (porcentaje < 100 ){
            //return 1;
            return porcentaje;
        }
        //Si es >= 100, negativo
        return -1 * porcentaje;

    }

    //Devuelve el puntaje asociado segun el porcentaje de llenado, si no es recogido.
    public static double getPuntajeNoRecogido(double porcentaje){
        if(porcentaje < 20){
            return 0;
        }
        return -1*porcentaje;

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

                        if (variables[j] == 0 || variables[j] > this.cantContenedores) {
                            //si tengo un cero, muevo de derecha a izquierda todo lo que no sea cero
                            int indice3 = j + 1;

                            //Hago una especie de selection sort, dejando todos los 0's a la derecha.
                            while (indice3 < indiceFinal) {
                                if (variables[indice3] != 0 && variables[indice3] <= this.cantContenedores ) {
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

                        if(contenedor != 0 && contenedor <= this.cantContenedores){
                            sumaTiempo = tiempo + datos.tiempos[actual][contenedor];
                            sumaBasura = (b[contenedor].v + velocidades[contenedor].v * sumaTiempo) / 100;      //divido entre 100 para utilizar fracciones de contenedores.

                            if (recogido + sumaBasura <= this.capCamiones) {
                                tiempo = sumaTiempo + tiempoRecol;
                                recogido += sumaBasura;
                                actual = contenedor;
                            }
                            else {
                                violoCond = true;

                                //Corrijo. Elimino el contenedor. Debo ponerlo en algun lugar de las celdas dummy
                                //Que sean cero, o en algun camion que tenga 0.
                                for (int k = indiceLimite; k < variables.length; k++) {
                                    contenedor = variables[k];
                                    if(contenedor == 0 || contenedor > this.cantContenedores) {
                                        int temp = variables[j];
                                        variables[j] = contenedor;
                                        variables[k] = temp;
                                        break;
                                    }

                                }

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
                    if (variables[j] != 0 && variables[j] <= this.cantContenedores) {
                        f1 += distancias[actual][variables[j]];
                        actual = variables[j];
                    } else {
                        break;
                    }
                }

                if (actual != 0 && actual <= this.cantContenedores) {
                    f1 += distancias[actual][0];  //Distancias del ultimo contenedor al origen
                }
            }
            catch (Throwable t){
                throw t;
            }

        }


        // -- Segunda funcion objetivo ---
        double f2 = 0;


        for (int i = 0; i < this.cantCamiones; i++) {

            indice = i * this.capCamionesAprox;
            indiceFinal = indice + this.capCamionesAprox;

            try {

                double sumaBasura;
                int tiempo = 0;
                int sumaTiempo = 0;
                int actual = 0;

                for(int j = indice; j < indiceFinal; j++){
                    int contenedor = variables[j];
                    if(contenedor != 0 && contenedor <= this.cantContenedores){
                        sumaTiempo = tiempo + tiempos[actual][contenedor];
                        sumaBasura = (b[contenedor].v + velocidades[contenedor].v * sumaTiempo);

                        f2 += getPuntajeRecogido(sumaBasura);

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

        //Por ultimo sumo todos los contenedores no recogidos
        for (int i = this.indiceLimite; i < variables.length; i++) {
            int contenedor = variables[i];
            if(contenedor != 0 && contenedor <= this.cantContenedores) {
                f2 += getPuntajeNoRecogido(b[contenedor].v);
            }

        }

        solution.setObjective(0, f1);
        solution.setObjective(1, -1*f2);


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
                for (int j = 0; j < this.cantCamiones * this.capCamionesAprox; j++) {

                    if (j % this.capCamionesAprox == 0 && j != 0) {
                        bw.write("  |  ");

                    }
                    else if (j != 0) {
                        bw.write(" ");
                    }

                    //String id = this.datos.puntos[(int)s.getDecisionVariables()[j].getValue()].id;
                    //bw.write(id);
                    int val = 0;
                    if(variables[j] <= this.cantContenedores){
                        val = variables[j];
                    }
                    bw.write(String.valueOf(val));
                }
                bw.write("  |- limite -|  ");
                for(int j = this.cantCamiones * this.capCamionesAprox; j < variables.length; j ++){
                    int val = 0;
                    if(variables[j] <= this.cantContenedores){
                        val = variables[j];
                    }
                    bw.write(String.valueOf(val) + " ");

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
                            if(contenedor != 0 && contenedor <= this.cantContenedores){
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

                for (int j = 0; j < this.cantCamiones; j++) {

                    int indice = j * this.capCamionesAprox;
                    int indiceFinal = indice + this.capCamionesAprox;

                    try {
                        int tiempo = 0;
                        int sumaTiempo = 0;
                        int actual = 0;

                        for(int k = indice; k < indiceFinal; k++){
                            int contenedor = variables[k];
                            if(contenedor != 0 && contenedor <= this.cantContenedores){
                                sumaTiempo = tiempo + tiempos[actual][contenedor];

                                bw.write(String.format("Contenedor [%s] Recogido: al %s %%", contenedor, b[contenedor].v + velocidades[contenedor].v * sumaTiempo));
                                if(getPuntajeRecogido(b[contenedor].v + velocidades[contenedor].v * sumaTiempo) < 0){
                                    bw.write("---- Ver ---");
                                }
                                tiempo = sumaTiempo + tiempoRecol;
                                actual = contenedor;

                                bw.newLine();

                            }
                            else {
                                break;  //si es cero, como estan ordenados con todos los ceros a la derecha, termino este camion.
                            }

                        }


                    } catch (Throwable t) {
                        throw new RuntimeException("ERROR: " + t.getMessage(), t);
                    }
                }

                //Por ultimo sumo todos los contenedores no recogidos
                //Estos son, todos los valores luego del ultimo contenedor del ultimo camion. O sea los que estan luego del limite dummy
                for (int j = this.indiceLimite; j < variables.length; j++) {
                    int contenedor = variables[j];
                    if(contenedor != 0 && contenedor <= this.cantContenedores) {
                        bw.write(String.format("Contenedor [%s] no recogido, dejado en %s %%", contenedor,b[contenedor].v));
                        if(getPuntajeNoRecogido(b[contenedor].v) < 0){
                            bw.write("---- Ver ---");
                        }
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

    //Devuelve una solucion que es el extremo donde no se recoge ningun contenedor.
    public static Permutation obtenerExtremoCeroContenedores(Datos datos){

        boolean[] visitados = new boolean[datos.puntos.length];
        for(int i = 0; i<visitados.length; i++)
            visitados[i]=false;

        visitados[0] = true;    //origen visitado para no tomarlo en cuenta.

        int cantPorCamion = datos.datosBasicos.capacidadCamiones + datos.datosBasicos.capacidadCamiones/2;
        int[] resultado = new int[datos.datosBasicos.cantidadCamiones*cantPorCamion];
        for(int i = 0; i < resultado.length;i++){
            resultado[i] = 0;
        }


        //Construyo resultado de permutacion para poder utilizar con AE

        int[] p = new int[resultado.length+datos.puntos.length-1];
        for(int i = 0; i < resultado.length; i++){
            p[i] = resultado[i];
        }
        for(int i = resultado.length; i < p.length; i++){
            p[i] = 0;
        }
        for(int i = 1; i < visitados.length; i++){
            if(!visitados[i]){
                p[resultado.length + i-1] = i;
            }
        }

        int similCero = 0;
        //Corrijo ceros para que sea igual que la sol AE
        for(int i = 0; i < p.length; i++){
            if(p[i] == 0){
                p[i] = similCero;
                if(similCero == 0){
                    similCero = datos.puntos.length;
                }
                else {
                    similCero++;
                }
            }
        }

        Permutation res = new Permutation();
        res.vector_ = p;
        res.size_ = p.length;
        return res;
    }

    /**

    //Greedy que busca primero los que esten mas llenos
    public static Permutation obtenerExtremoLlenos(Datos datos){

        double[] ponderaciones = new double[datos.puntos.length-1]; //cada valor almacena la ponderacion, excluyo el origen

        for(int i = 1; i < datos.puntos.length; i++){
            int llenado = datos.llenados[i].v;
            ponderaciones[i-1] = llenado;
        }

        int[] puntosOrdenados = new int[datos.puntos.length-1];   //guarda de menor a mayor los puntos segun su ponderacion. El valor es le indice del punto a utilizar

        for(int i = 0; i < ponderaciones.length; i++){
            double max = -1;
            int indiceMax = -1;
            for(int j = 0; j < ponderaciones.length; j++){
                if(ponderaciones[j] > max){
                    max = ponderaciones[j];
                    indiceMax = j;
                }
            }
            if(indiceMax != -1) {
                ponderaciones[indiceMax] = -1; //elimino ese valor como candidato posible de ponderacion
                puntosOrdenados[i] = indiceMax;
            }
            else{
                System.out.println("Warning: no se encontro indice");
            }
        }


        boolean[] visitados = new boolean[datos.puntos.length];
        for(int i = 0; i<visitados.length; i++)
            visitados[i]=false;

        visitados[0] = true;    //origen visitado para no tomarlo en cuenta.

        int cantPorCamion = datos.datosBasicos.capacidadCamiones + datos.datosBasicos.capacidadCamiones/2;

        int[]  resultado = new int[datos.datosBasicos.cantidadCamiones*cantPorCamion];

        int libres = visitados.length - 1;
        int index = 0;
        for(int i=0; i< datos.datosBasicos.cantidadCamiones; i++){
            int actual = 0; //indice del punto actual
            double recogido = 0;
            int cant = 0; //cantidad de contenedores que ha visitado un camion
            int tiempo = 0;

            while (cant < cantPorCamion && libres > 0) {


                int elegido = -1;
                double sumaBasura = -1;
                int sumaTiempo = -1;
                boolean encontrado = false;


                //de los mas cercanos, voy al mas cercano que no supere la capacidad del camion.
                for (int j = 0; j < puntosOrdenados.length; j++) {
                    elegido = puntosOrdenados[j];

                    if(!visitados[elegido]) {
                        sumaTiempo = tiempo + datos.tiempos[actual][elegido];
                        sumaBasura = (datos.llenados[elegido].v + datos.velocidades[elegido].v * sumaTiempo) / 100;

                        //Solo lo agrego si tiene mas de 20% de basura, y entra en el camion
                        if (recogido + sumaBasura <= datos.datosBasicos.capacidadCamiones) {
                            encontrado = true;
                            break;
                        }
                    }

                }


                if (encontrado) {

                    visitados[elegido] = true;

                    tiempo = sumaTiempo + datos.datosBasicos.tiempoRecoleccionContenedor;
                    recogido += sumaBasura;

                    libres--;
                    resultado[index] = elegido;
                    actual = elegido;

                    cant++;
                    index++;
                } else {
                    break; //si llegue aca quiere decir que se lleno el camion.
                }

            }

            if(cant < cantPorCamion){
                while (cant < cantPorCamion){
                    resultado[index] = 0;
                    index++;
                    cant++;
                }
            }
        }



        //Construyo resultado de permutacion para poder utilizar con AE

        int[] p = new int[resultado.length+datos.puntos.length-1];
        for(int i = 0; i < resultado.length; i++){
            p[i] = resultado[i];
        }
        for(int i = resultado.length; i < p.length; i++){
            p[i] = 0;
        }
        for(int i = 1; i < visitados.length; i++){
            if(!visitados[i]){
                p[resultado.length + i-1] = i;
            }
        }

        int similCero = 0;
        //Corrijo ceros para que sea igual que la sol AE
        for(int i = 0; i < p.length; i++){
            if(p[i] == 0){
                p[i] = similCero;
                if(similCero == 0){
                    similCero = datos.puntos.length;
                }
                else {
                    similCero++;
                }
            }
        }

        Permutation res = new Permutation();
        res.vector_ = p;
        res.size_ = p.length;
        return res;
    }
**/


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
