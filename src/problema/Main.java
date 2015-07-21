/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package problema;
import java.io.*;
import jmetal.util.JMException;
import problema.datos.Datos;

/**
 *
 * @author Cristiano
 */
public class Main {
    
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
    }
    
}
