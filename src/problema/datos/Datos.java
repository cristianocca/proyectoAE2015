package problema.datos;

import com.google.gson.Gson;
import com.sun.javaws.exceptions.InvalidArgumentException;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;

/**
 * Created by Cristiano on 20/07/2015.
 */
public class Datos {
    public DatoBasico datosBasicos;
    public Punto[] puntos;
    public Velocidad[] velocidades;
    public LlenadoInicial[] llenados = null;

    public float[][] distancias;
    public float[][] tiempos;

    /**
     * Carga datos estaticos, los datos de llenado se cargan a parte ya que deben ser cargados por cada instancia.
     */
    public static Datos cargarDatos(String pathDatosBasicos, String pathPuntos, String pathVelocidades, String pathDistancias, String pathTiempos ) throws FileNotFoundException {

        Datos res = new Datos();
        try {
            res.datosBasicos = DatoBasico.cargar(pathDatosBasicos);
        } catch (FileNotFoundException e) {
            throw new FileNotFoundException("Archivo de datos basicos no encontrado.");
        }

        try{
            res.puntos = Punto.cargar(pathPuntos);
        }
        catch (FileNotFoundException e) {
            throw new FileNotFoundException("Archivo de puntos no encontrado.");
        }

        try{
            res.velocidades = Velocidad.cargar(pathVelocidades);
        }
        catch (FileNotFoundException e) {
            throw new FileNotFoundException("Archivo de velocidades no encontrado.");
        }

        try{
            Gson gson = new Gson();
            BufferedReader br = new BufferedReader(new FileReader(pathDistancias));
            res.distancias = gson.fromJson(br, float[][].class);
        }
        catch (FileNotFoundException e) {
            throw new FileNotFoundException("Archivo de distancias no encontrado.");
        }

        try{
            Gson gson = new Gson();
            BufferedReader br = new BufferedReader(new FileReader(pathTiempos));
            res.tiempos = gson.fromJson(br, float[][].class);
        }
        catch (FileNotFoundException e) {
            throw new FileNotFoundException("Archivo de tiempos no encontrado.");
        }


        return res;
    }

    public void cargarLlenados(String pathLlenados) throws FileNotFoundException {
        try{
            this.llenados = LlenadoInicial.cargar(pathLlenados);
        }
        catch (FileNotFoundException e) {
            throw new FileNotFoundException("Archivo de llenados no encontrado.");
        }
    }

    /**
     * Helper para cargar datos a partir de argumentos del main
     * @return
     */
    public static Datos cargarDatosDeArgs(String[] args) throws FileNotFoundException {
        if(args.length < 6){
            System.out.println("Se necesitan al menos 6 argumentos para datos.");

            System.out.println("Path datosBasicos.json");
            System.out.println("Path puntos.json");
            System.out.println("Path velocidades.json");
            System.out.println("Path distancias.json");
            System.out.println("Path tiempos.json");
            System.out.println("Path instancia.json");

            throw new IllegalArgumentException("ERROR: Faltan argumentos");
        }

        System.out.println(args[0]);
        System.out.println(args[1]);
        System.out.println(args[2]);
        System.out.println(args[3]);
        System.out.println(args[4]);
        System.out.println(args[5]);

        Datos datos = Datos.cargarDatos(args[0],args[1],args[2],args[3],args[4]);
        //Para probar cargo solo el primer argumento
        datos.cargarLlenados(args[5]);
        System.out.println("Total puntos: " + datos.puntos.length);




        return datos;
    }

}
