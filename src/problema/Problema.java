package problema;

import jmetal.core.Problem;
import jmetal.core.Solution;
import jmetal.core.Variable;
import jmetal.encodings.solutionType.ArrayIntSolutionType;
import jmetal.encodings.solutionType.IntSolutionType;
import jmetal.encodings.variable.ArrayInt;
import jmetal.encodings.variable.Int;
import jmetal.util.JMException;
import jmetal.util.PseudoRandom;
import problema.datos.Datos;
import problema.datos.LlenadoInicial;
import problema.datos.Velocidad;

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
    public int tiempoFin;       //tiempo de final de la planificacion en segundos.
    public static TiempoComparator tiempoComparator = new TiempoComparator();

    public Problema(Datos datos){
        this.datos = datos;
        this.cantCamiones = datos.datosBasicos.cantidadCamiones;
        this.capCamiones = datos.datosBasicos.capacidadCamiones;
        this.capCamionesAprox = (this.capCamiones + this.capCamiones / 2);
        this.tiempoFin = datos.datosBasicos.tiempoTrabajo * 60 * 60;

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
    //y cada valor es una lista de todos los tiempos ordenados en que fue recogido el contenedor, ademas del tiempo de fin como ultimo elemento.
    public ArrayList<ArrayList<int[]>> construirListaTiempos(Solution solution){
        Variable[] variables = solution.getDecisionVariables();

        ArrayList<ArrayList<int[]>> res = new ArrayList<>(this.cantContenedores);

        float[][] tiempos = this.datos.tiempos;
        int tiempoRecol = this.datos.datosBasicos.tiempoRecoleccionContenedor;

        //Inicializo
        for(int i = 0; i < this.cantContenedores;i++){
            ArrayList<int[]> r = new ArrayList<>();
            r.add(new int[]{this.tiempoFin, -1});       //Tiempo fin, -1 para indicar que no fue recogido
            res.add(r);
        }

        int indice = 0;
        int ix = 0;
        int iy = 0;
        int tiempo = 0;
        for(int i = 0; i < this.cantCamiones; i++){
            tiempo = 0;
            indice = i * this.capCamionesAprox;

            try {
                iy = (int) variables[indice].getValue();
                tiempo += tiempos[0][iy];   //tiempo del origen al primer contenedor

                if(iy != 0) {
                    res.get(iy).add(new int[]{tiempo, i});
                    //ademas sumo el tiempo de la recoleccion
                    tiempo+=tiempoRecol;
                }

                for (int j = 1; j < this.capCamionesAprox; j++) {
                    ix = (int) variables[indice + j - 1].getValue();
                    iy = (int) variables[indice + j].getValue();
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

        //Por ultimo ordeno las listas
        for(ArrayList<int[]> v : res){
            v.sort(tiempoComparator);
        }

        return res;

    }


    //Devuelve le puntaje asociado segun el porcentaje de llenado, al momento de ser recogido
    public float getPuntajeRecogido(float porcentaje){
        if(porcentaje <= 25){
            return 0;
        }
        if (porcentaje > 25 && porcentaje < 100 ){
            return porcentaje;
        }
        //Si es >= 100, negativo
        return porcentaje * -1;
    }

    //Devuelve el puntaje asociado segun el porcentaje de llenado, si no es recogido.
    public float getPuntajeNoRecogido(float porcentaje){
        if(porcentaje <= 25){
            return 0;
        }
        return porcentaje * -1;
    }

    @Override
    public void evaluate(Solution solution) {
        Variable[] variables = solution.getDecisionVariables();


        // --- Calculo de primer funcion objetivo ----

        float[][] distancias = this.datos.distancias;
        float f1 = 0;
        int indice = 0;

        for(int i = 0; i < this.cantCamiones; i++){

            indice = i * this.capCamionesAprox;

            try {
                f1 += distancias[0][(int) variables[indice].getValue()];   //Distancia del 0 al primer contenedor

                for (int j = 1; j < this.capCamionesAprox; j++) {
                    f1 += distancias[(int) variables[indice + j - 1].getValue()][(int) variables[indice + j].getValue()];       //Distancias de contenedores intermedios
                }

                f1 += distancias[(int) variables[indice + this.capCamionesAprox - 1].getValue()][0];  //Distancias del ultimo contenedor al origen
            }
            catch (Throwable t){
                throw new RuntimeException("ERROR: "+t.getMessage(), t);
            }
        }


        // --- Calculo de segunda funcion objetivo ----

        ArrayList<ArrayList<int[]>> tc = construirListaTiempos(solution);


        float f2 = 0;
        ArrayList<int[]> contenedor_i;
        int largoContenedor_i;

        LlenadoInicial[] b = this.datos.llenados;   //basura inicial
        Velocidad[] velocidades = this.datos.velocidades;   //velocidades de llenado

        //Ignoro i=0 que es el origen
        for(int i = 1; i < this.cantContenedores; i++){
            contenedor_i = tc.get(i);
            largoContenedor_i = contenedor_i.size();

            if (largoContenedor_i != 1) {
                f2 += getPuntajeRecogido(b[i].v + velocidades[i].v * contenedor_i.get(0)[0]);  //Si la cantidad de contenedores de la lista es != 1, quiere decir que fue recogido
            }
            else {
                f2 += getPuntajeNoRecogido(b[i].v + velocidades[i].v * contenedor_i.get(0)[0]);                      //si no, no fue recogido
                //ademas finalizo el loop para este contenedor
                continue;
            }

            for (int j = 2; j < largoContenedor_i - 1; j++){
                f2+=getPuntajeRecogido(velocidades[i].v * (contenedor_i.get(j)[0] - contenedor_i.get(j-1)[0]));
            }

            //El ultimo siempre va a ser el tiempo de fin, en que no fue recogido.
            f2+=getPuntajeRecogido(velocidades[i].v * (contenedor_i.get(largoContenedor_i-1)[0] - contenedor_i.get(largoContenedor_i-2)[0]));
        }

        solution.setObjective(0, f1);
        solution.setObjective(1, -1*f2);

    } // evaluate

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
