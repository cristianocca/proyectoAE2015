package problema;

import jmetal.core.Problem;
import jmetal.core.Solution;
import jmetal.core.SolutionSet;
import jmetal.core.Variable;
import jmetal.encodings.solutionType.IntSolutionType;
import jmetal.encodings.variable.Int;
import jmetal.encodings.variable.Permutation;
import jmetal.encodings.variable.ZeroPermutation;
import org.omg.CORBA.PUBLIC_MEMBER;
import problema.datos.Datos;
import problema.datos.Punto;

import java.util.Random;

/**
 * Created by Gimena on 03/08/2015.
 */
public class MainGreedy {



    //Esta version si el contenedor esta < 20% al momento de visitarlo, no lo recoge (y nunca se recoge)
    public static ZeroPermutation ejecutarGreedy(Datos datos){

        if(datos.puntosOrdenados == null) {
            datos.puntosOrdenados = datos.cargarPuntosOrdenados(datos.distancias);
        }

        boolean[] visitados = new boolean[datos.puntos.length];
        for(int i = 0; i<visitados.length; i++)
            visitados[i] = datos.llenados[i].v < 20;

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
                for (int j = 1; j < datos.puntosOrdenados.length; j++) {
                    elegido = datos.puntosOrdenados[actual][j];

                    if(!visitados[elegido]) {
                        sumaTiempo = tiempo + datos.tiempos[actual][elegido];
                        sumaBasura = (datos.llenados[elegido].v + datos.velocidades[elegido].v * sumaTiempo) / 100;

                        //Solo lo agrego si tiene mas de 20% de basura, y entra en el camion
                        if (sumaBasura > 0.2 && recogido + sumaBasura <= datos.datosBasicos.capacidadCamiones) {
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


        ZeroPermutation res = new ZeroPermutation();
        res.vector_ = resultado;
        res.size_ = resultado.length;
        return res;
    }


    //Esta version si el contenedor esta < 20% al momento de visitarlo, no lo recoge (se recoge despues)
    public static ZeroPermutation ejecutarGreedyv2(Datos datos){

        if(datos.puntosOrdenados == null) {
            datos.puntosOrdenados = datos.cargarPuntosOrdenados(datos.distancias);
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
                for (int j = 1; j < datos.puntosOrdenados.length; j++) {
                    elegido = datos.puntosOrdenados[actual][j];

                    if(!visitados[elegido]) {
                        sumaTiempo = tiempo + datos.tiempos[actual][elegido];
                        sumaBasura = (datos.llenados[elegido].v + datos.velocidades[elegido].v * sumaTiempo) / 100;

                        //Solo lo agrego si tiene mas de 20% de basura, y entra en el camion
                        if (sumaBasura > 0.2 && recogido + sumaBasura <= datos.datosBasicos.capacidadCamiones) {
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


        ZeroPermutation res = new ZeroPermutation();
        res.vector_ = resultado;
        res.size_ = resultado.length;
        return res;
    }





    public static void main(String[] args){
        try{
            Datos datos = Datos.cargarDatosDeArgs(args);

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

            Permutation resultado = ejecutarGreedy(datos);

            //Con la solucion, instancio el problema para poder usar mismas funciones que el AE
            //y simplificar el codigo

            Problem problem = new Problema(datos);
            Solution solucionGreedy = new Solution(problem, new Variable[]{resultado});
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
