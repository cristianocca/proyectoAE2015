package problema;

import jmetal.core.Problem;
import jmetal.core.Solution;
import jmetal.core.SolutionSet;
import jmetal.core.Variable;
import jmetal.encodings.solutionType.ArrayIntSolutionType;
import jmetal.encodings.solutionType.IntSolutionType;
import jmetal.encodings.variable.ArrayInt;
import jmetal.encodings.variable.Int;
import jmetal.util.Configuration;
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

/**
 * Created by Cristiano on 27/07/2015.
 */
public class Problema extends Problem {

    public Datos datos;
    public int cantContenedores;
    public int cantCamiones;
    public int capCamiones;
    public int capCamionesAprox; //capCamiones + capCamiones / 2
    public float tiempoFin;       //tiempo de final de la planificacion en segundos.
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

        this.cantContenedores = datos.puntos.length;    //incluye el origen

        numberOfObjectives_ = 2;        //0: Min trayectoria, 1: Maximizar QoS

        solutionType_ = new IntSolutionType(this) ;

        numberOfVariables_  = this.capCamionesAprox  * this.cantCamiones;

        lowerLimit_ = new double[numberOfVariables_];
        upperLimit_ = new double[numberOfVariables_];

        for(int i = 0; i < this.numberOfVariables_; i++){
            lowerLimit_[i] = 0;
            upperLimit_[i] = this.cantContenedores-1;       //0 a cantContenedores - 1
        }

        problemName_        = "Recoleccion de basura";

    }

    //Construye lista de tiempos ordenada.
    //donde int[] siempre sera un array de 2 elementos, donde el primero es el tiempo en que fue recogido, y el segundo el camion que lo recogio
    //El array resultado, es entonces, una lista donde cada indice es cada contenedor (sin ser el 0 que es el origen y deberia ser ignorado)
    //y cada valor es una lista de todos los tiempos ordenados en que fue recogido el contenedor
    //Ademas setea en this.tiempoFin el tiempo maximo encontrado.
    public ArrayList<ArrayList<int[]>> construirListaTiempos(Variable[] variables){

        ArrayList<ArrayList<int[]>> res = new ArrayList<>(this.cantContenedores);

        float[][] tiempos = this.datos.tiempos;
        int tiempoRecol = this.datos.datosBasicos.tiempoRecoleccionContenedor;

        //Inicializo
        for(int i = 0; i < this.cantContenedores;i++){
            ArrayList<int[]> r = new ArrayList<>();
            //r.add(new int[]{this.tiempoFin, -1});       //Tiempo fin, -1 para indicar que no fue recogido
            res.add(r);
        }

        int indice = 0;
        int indiceFinal = 0;
        int ix = 0;
        int iy = 0;
        int tiempo = 0;
        for(int i = 0; i < this.cantCamiones; i++){
            tiempo = 0;
            indice = i * this.capCamionesAprox;
            indiceFinal = indice + this.capCamionesAprox;

            try {
                iy = (int) variables[indice].getValue();
                tiempo += tiempos[0][iy];   //tiempo del origen al primer contenedor

                if(iy != 0) {
                    res.get(iy).add(new int[]{tiempo, i});
                    //ademas sumo el tiempo de la recoleccion
                    tiempo+=tiempoRecol;
                }

                for (int j = indice + 1; j < indiceFinal; j++) {
                    ix = (int) variables[j - 1].getValue();
                    iy = (int) variables[j].getValue();
                    tiempo += tiempos[ix][iy];

                    if(iy != 0) {    //Solo inserto si el contenedor es distinto al origen
                        res.get(iy).add(new int[]{tiempo, i});
                        tiempo+=tiempoRecol;
                    }
                }
            }
            catch (Throwable t){
                throw new RuntimeException("ERROR: "+t.getMessage(), t);
            }

        }

        //Por ultimo ordeno las listas y obtengo el tiempo maximo del camion
        int maxTiempo = 0;
        int indiceMax = -1;
        indice = 0;
        for(ArrayList<int[]> v : res){
            v.sort(tiempoComparator);
            if(v.size() > 0 && v.get(v.size()-1)[0] > maxTiempo){
                maxTiempo = v.get(v.size()-1)[0];
                indiceMax = indice;
            }

            indice++;
        }

        //Tiempo de fin es = al ultimo tiempo en que fue recolectado, + tiempo de recoleccion + tiempo en volver a la base desde ese punto
        if(indiceMax != -1) {
            this.tiempoFin = maxTiempo + tiempoRecol + tiempos[indiceMax][0];
        }
        else {
            this.tiempoFin = 0;
        }

        return res;

    }


    //Devuelve le puntaje asociado segun el porcentaje de llenado, al momento de ser recogido
    public static float getPuntajeRecogido(float porcentaje){

        if (porcentaje < 100 ){
            return porcentaje;
        }
        //Si es >= 100, negativo
        return porcentaje * -1;

    }

    //Devuelve el puntaje asociado segun el porcentaje de llenado, si no es recogido.
    public static float getPuntajeNoRecogido(float porcentaje){

        return 100-porcentaje;  //Cuanto mas vacio queda mejor, y si se pasa de 100 empieza a ser negativo

    }

    @Override
    public void evaluate(Solution solution) {

        //******************************************************************************************************************
        //******** Variables que voy a necesitar ************
        //******************************************************************************************************************

        Variable[] variables = solution.getDecisionVariables();
        int tiempoRecol = this.datos.datosBasicos.tiempoRecoleccionContenedor;

        float[][] distancias = this.datos.distancias;
        int indice = 0;
        int indiceFinal = 0;

        ArrayList<int[]> contenedor_i;
        int largoContenedor_i;

        LlenadoInicial[] b = this.datos.llenados;   //basura inicial
        Velocidad[] velocidades = this.datos.velocidades;   //velocidades de llenado




        //******************************************************************************************************************
        //******** Restricciones y correccion de soluciones ************
        //******************************************************************************************************************

        ArrayList<ArrayList<int[]>> tc;
        boolean violoCond;

        //Itero hasta que la solucion sea factible
        do {

            tc = construirListaTiempos(variables);
            violoCond = false;

            //Un contenedor no puede ser recogido al mismo tiempo por mas de un cami�n, ni en el intervalo en que est� siendo recogido (tiempoRecoleccionContenedor)
            //esta restriccion es violada demasiadas veces, comparada con las demas, habria que ver como mejorarla.

            int camionViolador;
            try {
                for (int i = 1; i < this.cantContenedores; i++) {
                    contenedor_i = tc.get(i);
                    largoContenedor_i = contenedor_i.size();

                    for (int j = 1; j < largoContenedor_i; j++) {
                        if (contenedor_i.get(j)[0] - contenedor_i.get(j - 1)[0] <= tiempoRecol) {
                            violoCond = true;
                            //solution.setNumberOfViolatedConstraint(solution.getNumberOfViolatedConstraint()+1);

                            camionViolador = contenedor_i.get(j)[1];
                            indice = camionViolador * this.capCamionesAprox;
                            indiceFinal = indice + this.capCamionesAprox;

                            //saco del camion la recoleccion de ese contenedor
                            for (int k = indice; k < indiceFinal; k++) {

                                if (variables[k].getValue() == i) {
                                    variables[k].setValue(0);

                                    break;
                                }

                                //variables[k].setValue(0);

                            }

                        }

                    }
                }
            } catch (JMException e) {
                e.printStackTrace();
                throw new RuntimeException("ERROR: " + e.getMessage(), e);
            }




            //Para cada camión, luego que se tiene un 0 en su solución, todos los restantes valores a la derecha también deberán ser 0
            for (int i = 0; i < this.cantCamiones; i++) {

                indice = i * this.capCamionesAprox;
                indiceFinal = indice + this.capCamionesAprox;

                try {

                    //La cantidad de basura recogida por cada camión no puede exceder su capacidad real, para esto se utiliza el % de basura recogido de cada contenedor y se valida que el total no supere su capacidad.

                    double contenedoresRecolectados = 0;        //double para manejar fracciones.
                    double sumaBasura;

                    for (int k = 1; k < this.cantContenedores; k++) {
                        contenedor_i = tc.get(k);
                        largoContenedor_i = contenedor_i.size();

                        if (largoContenedor_i > 0) {

                            //primero agrego la primera recoleccion, que incluye el valor inicial.

                            if (contenedor_i.get(0)[1] == i) { //solo sumo si el camion que junto el contenedor es el camion actual

                                sumaBasura = (b[k].v + velocidades[k].v * contenedor_i.get(0)[0]) / 100.0;   //puede ser mayor a 1 !! O sea, que junta la basura desbordada en caso de > 100%;

                                if (contenedoresRecolectados + sumaBasura > (double) this.capCamiones) {
                                    violoCond = true;

                                    //elimino el primer contenedor que encuentre que sea igual al que hizo que me pase, ya que viene ordenado por tiempo deberia ser correcto.
                                    for (int j = indice; j < indiceFinal; j++) {
                                        if (variables[j].getValue() == k) {
                                            variables[j].setValue(0);
                                            break;
                                        }

                                    }
                                } else {
                                    contenedoresRecolectados += sumaBasura;
                                }
                            }

                            //Luego agrego las restantes recolecciones
                            for (int z = 1; z < largoContenedor_i; z++) {
                                if (contenedor_i.get(z)[1] == i) { //solo sumo si el camion que junto el contenedor es el camion actual

                                    sumaBasura = (velocidades[k].v * (contenedor_i.get(z)[0] - (contenedor_i.get(z - 1)[0] + tiempoRecol))) / 100.0;

                                    if (contenedoresRecolectados + sumaBasura > (double) this.capCamiones) {
                                        violoCond = true;


                                        //elimino el primer contenedor que encuentre que sea igual al que hizo que me pase, ya que viene ordenado por tiempo deberia ser correcto.
                                        for (int j = indice; j < indiceFinal; j++) {
                                            if (variables[j].getValue() == k) {
                                                variables[j].setValue(0);
                                                break;
                                            }

                                        }
                                    } else {
                                        contenedoresRecolectados += sumaBasura;
                                    }
                                }
                            }
                        }
                    }


                    //pongo todos los ceros lo mas a la derecha posible, y no dejo ceros entre medio.
                    for (int j = indice; j < indiceFinal; j++) {

                        if (variables[j].getValue() == 0) {
                            //si tengo un cero, muevo de derecha a izquierda todo lo que no sea cero
                            int indice3 = j + 1;

                            //Hago una especie de selection sort, dejando todos los 0's a la derecha.
                            while (indice3 < indiceFinal) {
                                if (variables[indice3].getValue() != 0) {
                                    variables[j].setValue(variables[indice3].getValue());
                                    variables[indice3].setValue(0);
                                    break;
                                }
                                indice3++;
                            }
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

            try {
                f1 += distancias[0][(int) variables[indice].getValue()];   //Distancia del 0 al primer contenedor

                for (int j = indice+1; j < indiceFinal; j++) {
                    f1 += distancias[(int) variables[j - 1].getValue()][(int) variables[j].getValue()];       //Distancias de contenedores intermedios
                }

                f1 += distancias[(int) variables[indiceFinal - 1].getValue()][0];  //Distancias del ultimo contenedor al origen
            }
            catch (Throwable t){
                throw new RuntimeException("ERROR: "+t.getMessage(), t);
            }
        }


        // -- Segunda funcion objetivo ---
        double f2 = 0;

        //Ignoro i=0 que es el origen
        for(int i = 1; i < this.cantContenedores; i++){
            contenedor_i = tc.get(i);
            largoContenedor_i = contenedor_i.size();

            if (largoContenedor_i > 0) {
                f2 += getPuntajeRecogido(b[i].v + velocidades[i].v * contenedor_i.get(0)[0]);  //Si la cantidad de contenedores de la lista es >0, quiere decir que fue recogido al menos una vez

                for (int j = 1; j < largoContenedor_i; j++){
                    f2+=getPuntajeRecogido(velocidades[i].v * (contenedor_i.get(j)[0] - (contenedor_i.get(j-1)[0] + tiempoRecol)));
                }

                //Agrego tiempo entre la ultima vez que fue recogido y el tiempo de fin
                f2+=getPuntajeNoRecogido(velocidades[i].v * (this.tiempoFin - (contenedor_i.get(largoContenedor_i - 1)[0] + tiempoRecol)));
            }
            else {
                f2 += getPuntajeNoRecogido(b[i].v + velocidades[i].v * this.tiempoFin);                      //si no, no fue recogido nunca

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
        float[][] tiempos = this.datos.tiempos;

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
            //if (true) {
                for (int j = 0; j < s.numberOfVariables(); j++) {
                    if (j % this.capCamionesAprox == 0 && j != 0) {
                        bw.write("  |  ");
                    } else if (j != 0) {
                        bw.write(" ");
                    }

                    //String id = this.datos.puntos[(int)s.getDecisionVariables()[j].getValue()].id;
                    //bw.write(id);
                    bw.write(s.getDecisionVariables()[j].toString());

                }
                bw.newLine();
                //bw.write("Trayectoria total en metros: " + String.valueOf(s.getObjective(0)));
                bw.write(String.valueOf(s.getObjective(0)));
                bw.newLine();
                //bw.write("QoS total: " + String.valueOf(-1 * s.getObjective(1)));
                bw.write(String.valueOf(-1 * s.getObjective(1)));
                bw.newLine();


                //Imprimo cuan lleno termina el contenedor
                ArrayList<ArrayList<int[]>> tc = construirListaTiempos(s.getDecisionVariables());
                ArrayList<int[]> contenedor_j;
                int largoContenedor_j;
                float maxTiempo = 0;
                for (int j = 1; j < this.cantContenedores; j++) {
                    contenedor_j = tc.get(j);
                    largoContenedor_j = contenedor_j.size();

                    //Si se recogio al menos una vez, calculo desde la ultima vez recogida
                    if (largoContenedor_j > 0 ) {
                        bw.write(String.format("Llenado al terminar (recogido) contenedor %s: %s %%", j, this.datos.velocidades[j].v * (this.tiempoFin - (contenedor_j.get(largoContenedor_j - 1)[0] + tiempoRecol))));
                        bw.newLine();

                        if(contenedor_j.get(largoContenedor_j - 1)[0] > maxTiempo){
                            maxTiempo = contenedor_j.get(largoContenedor_j - 1)[0];
                        }

                    }
                    //Caso contrario, usa llenado inicial
                    else {
                        bw.write(String.format("Llenado al terminar (no recogido) contenedor %s: %s %%", j, this.datos.llenados[j].v + this.datos.velocidades[j].v * this.tiempoFin));
                        bw.newLine();
                    }
                }
                bw.write("Tiempo en que es recolectado ultimo contenedor en h: " + String.valueOf(maxTiempo/60/60));
                bw.newLine();
                bw.write("Tiempo en que retorna el ultimo camion en h: " + String.valueOf(this.tiempoFin / 60 / 60));
                bw.newLine();
                //bw.write("violaciones: " + String.valueOf(s.getNumberOfViolatedConstraint()));
                //bw.newLine();
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
