package problema;

import jmetal.core.Problem;
import jmetal.core.Solution;
import jmetal.core.SolutionSet;
import jmetal.encodings.solutionType.IntSolutionType;
import jmetal.encodings.variable.Int;
import org.omg.CORBA.PUBLIC_MEMBER;
import problema.datos.Datos;
import problema.datos.Punto;

import java.util.Random;

/**
 * Created by Gimena on 03/08/2015.
 */
public class MainGreedy {


    public static Int[] ejecutarGreedy(Datos datos){

        boolean[] visitados = new boolean[datos.puntos.length];
        for(int i = 0; i<visitados.length; i++)
            visitados[i]=false;

        int cantPorCamion = datos.datosBasicos.capacidadCamiones + datos.datosBasicos.capacidadCamiones/2;
        int lowerBound = 0;
        int upperBound = datos.datosBasicos.cantidadCamiones - 1;

        Int[]  resultado = new Int[datos.datosBasicos.cantidadCamiones*cantPorCamion];
            /*
            para cada camion:
                busco los 10 contenedores mas cercanos que aun no fueron visitados
                obtengo los 3 mas llenos
                elijo aleatoriamente 1
                visito el contenedor elegido
                    necesito marcarlo como visitado y guardar en algun lugar el tiempo que demore en llegar ahi mas 3 minutos de recoleccion
             */


        int libres = visitados.length - 1;
        int index = 0;
        for(int i=0; i< datos.datosBasicos.cantidadCamiones; i++){
            int actual = 0; //indice del punto actual
            int recogido = 0;
            int cant = 0; //cantidad de contenedores que ha visitado un camion
            while (recogido < (datos.datosBasicos.capacidadCamiones*100 )&& cant < cantPorCamion && libres > 0){
                int[] masCercanos = getDiezMasCercanos(actual, libres, visitados, datos);
                if(masCercanos != null && masCercanos.length > 0) {
                    int[] masllenos = getMasLlenos(masCercanos, datos);
                    if(masllenos != null && masllenos.length > 0) {
                        int elegido = elegirRandom(masllenos);
                        if (elegido >= 0) {
                            visitados[elegido] = true;
                            recogido += datos.llenados[elegido].v;
                            libres--;
                            Int aux = new Int(elegido, lowerBound, upperBound);
                            resultado[index] = aux;
                            actual = elegido;
                        }
                    }
                }
                cant++;
                index++;
            }
            if(cant < cantPorCamion){
                while (cant < cantPorCamion){
                    Int aux = new Int(0,lowerBound, upperBound);
                    resultado[index] = aux;
                    index++;
                    cant++;
                }
            }
        }

        return resultado;
    }

    public static void main(String[] args){
        try{
            Datos datos = Datos.cargarDatosDeArgs(args, true);

            String salidaFun = "SALIDA_FUN_GREEDY.txt";
            String salidaVar = "SALIDA_VAR_GREEDY.txt";

            if(args.length >= 7){
                salidaFun = args[8];
            }

            if(args.length >= 8){
                salidaVar = args[9];
            }

            System.out.println("---- Parametros a utilizar ----");
            System.out.println("Salidas: " + salidaFun + ", " + salidaVar);

            long initTime = System.currentTimeMillis();

            Int[] resultado = ejecutarGreedy(datos);

            //Con la solucion, instancio el problema para poder usar mismas funciones que el AE
            //y simplificar el codigo

            Problem problem = new Problema(datos);
            Solution solucionGreedy = new Solution(problem, resultado);
            problem.evaluate(solucionGreedy);
            long elapsedTime = System.currentTimeMillis() - initTime;


            SolutionSet solSetGreedy = new SolutionSet(1);
            solSetGreedy.add(solucionGreedy);
            ((Problema) problem).imprimirSolucion("SALIDA_GREEDY.txt", solSetGreedy);


            //Imprimo y genero datos de la misma forma que el AE

            System.out.println("Compromiso F1: " + solucionGreedy.getObjective(0));
            System.out.println("Compromiso F2: " + solucionGreedy.getObjective(1));
            System.out.println("Tiempo Algoritmo: " + elapsedTime/1000 + "s");

            solSetGreedy.printFeasibleFUN(salidaFun);
            solSetGreedy.printFeasibleVAR(salidaVar);


        }
        catch (Throwable t){
            System.out.println(t.getMessage());
            return;
        }
    }

    private static int[] getDiezMasCercanos(int actual, int cantLibres, boolean[] visitados, Datos datos){
        try {
            int total = 10;
            if (cantLibres < 10) {
                total = cantLibres;
            }
            int[] res = new int[total];
            int index = 0;
            int i = 1;
            while (index < total) {
                if (datos.puntosOrdenados[actual][i] != 0 && visitados[datos.puntosOrdenados[actual][i]] == false) {
                    res[index] = datos.puntosOrdenados[actual][i];
                    index++;
                }
                i++;
            }
            return res;
        }
        catch (Exception ex){
            return null;
        }
    }

    private static int[] getMasLlenos(int[] masCercanos, Datos datos){
        try {


            if (masCercanos.length <= 3) {
                return masCercanos;
            }
            int[] res = new int[3];
            int max = 0;
            int index = 0;
            for (int i = 0; i < masCercanos.length; i++) {
                if (datos.llenados[masCercanos[i]].v > max) {
                    max = datos.llenados[masCercanos[i]].v;
                    index = i;
                }
            }
            res[0] = masCercanos[index];

            int max2 = 0;
            index = 0;
            for (int i = 0; i < masCercanos.length; i++) {
                if (datos.llenados[masCercanos[i]].v > max2 && datos.llenados[masCercanos[i]].v < max) {
                    max2 = datos.llenados[masCercanos[i]].v;
                    index = i;
                }
            }
            res[1] = masCercanos[index];;

            int max3 = 0;
            index = 0;
            for (int i = 0; i < masCercanos.length; i++) {
                if (datos.llenados[masCercanos[i]].v > max3 && datos.llenados[masCercanos[i]].v < max2) {
                    max3 = datos.llenados[masCercanos[i]].v;
                    index = i;
                }
            }
            res[2] = masCercanos[index];

            return res;
        }
        catch (Exception ex){
            return  null;
        }
    }

    private static int elegirRandom(int[] masllenos){
        try {
            if(masllenos != null && masllenos.length > 1) {
                int cant = masllenos.length - 1;
                Random rand = new Random();
                int index = rand.nextInt(cant);
                return masllenos[index];
            }
            else{
                if(masllenos != null){
                    return masllenos[0];
                }
                else{
                    return  -1;
                }
            }
        }
        catch (Exception ex){
            return 0;
        }
    }
}
