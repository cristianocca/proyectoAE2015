package problema;

import problema.datos.Datos;
import problema.datos.Punto;

import java.util.Random;

/**
 * Created by Gimena on 03/08/2015.
 */
public class MainGreedy {

    private static Datos datos;

    public static void main(String[] args){
        try{
            datos = Datos.cargarDatosDeArgs(args);
            boolean[] visitados = new boolean[datos.puntos.length];
            for(int i = 0; i<visitados.length; i++)
                visitados[i]=false;
            /*
            para cada camion:
                busco los 10 contenedores mas cercanos que aun no fueron visitados
                obtengo los 3 mas llenos
                elijo aleatoriamente 1
                visito el contenedor elegido
                    necesito marcarlo como visitado y guardar en algun lugar el tiempo que demore en llegar ahi mas 3 minutos de recoleccion
             */

            int recogido = 0;
            int libres = visitados.length;
            for(int i=0; i< datos.datosBasicos.cantidadCamiones; i++){
                int actual = 0; //indice del punto actual

                while (recogido < datos.datosBasicos.capacidadCamiones){
                    int[] masCercanos = getDiezMasCercanos(actual,libres, visitados);
                    int[] masllenos = getMasLlenos(masCercanos);
                    int elegido = elegirRandom(masllenos);
                    libres--;
                }

            }
        }
        catch (Throwable t){
            System.out.println(t.getMessage());
            return;
        }
    }

    private static int[] getDiezMasCercanos(int actual, int cantLibres, boolean[] visitados){
        int cant = 0;
        int total = 10;
        if(cantLibres < 10){
            total = cantLibres;
        }
        int[] res = new int[total];
        int i=1;
        int index = 0;
        while (cant < total){
            if(visitados[datos.puntosOrdenados[actual][i]]==false){
                res[index] = datos.puntosOrdenados[actual][i];
                index++;
            }
        }
        return  res;
    }

    private static int[] getMasLlenos(int[] masCercanos){

        if(masCercanos.length <= 3){
            return  masCercanos;
        }
        int[] res = new int[3];
        int max = 0;
        int index = 0;
        for (int i = 0; i<masCercanos.length;i++){
            if(datos.llenados[masCercanos[i]].v > max){
                max = datos.llenados[masCercanos[i]].v;
                index = i;
            }
        }
        res[0] = index;

        int max2 = 0;
        index = 0;
        for (int i = 0; i< masCercanos.length;i++){
            if(datos.llenados[masCercanos[i]].v > max2 &&datos.llenados[masCercanos[i]].v < max ){
                max2 = datos.llenados[masCercanos[i]].v;
                index = i;
            }
        }
        res[1]=index;

        int max3 = 0;
        index = 0;
        for (int i = 0; i<masCercanos.length;i++){
            if(datos.llenados[masCercanos[i]].v > max3 && datos.llenados[masCercanos[i]].v < max2 ){
                max3 = datos.llenados[masCercanos[i]].v;
                index = i;
            }
        }
        return  null;
    }

    private static int elegirRandom(int[] masllenos){
        int cant = masllenos.length - 1;
        Random rand = new Random();
        int index = rand.nextInt(cant);
        return  masllenos[index];
    }
}
