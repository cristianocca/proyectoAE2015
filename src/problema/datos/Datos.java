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
        if(args.length != 7){
            System.out.println("Se necesitan 6 argumentos.");

            System.out.println("Path datosBasicos.json");
            System.out.println("Path puntos.json");
            System.out.println("Path velocidades.json");
            System.out.println("Path llenado_x.json, donde x se reemplazara por arg de instancias.");
            System.out.println("Instancias (para x) separadas por coma: ej: 1,2,3,4,5");
            System.out.println("Path distancias.json");
            System.out.println("Path tiempos.json");

            throw new IllegalArgumentException("ERROR: Faltan argumentos");
        }

        System.out.println(args[0]);
        System.out.println(args[1]);
        System.out.println(args[2]);
        System.out.println(args[3]);
        System.out.print("Instancias: ");
        String[] instanciasArg = args[4].split(",");
        if(instanciasArg.length <= 0){
            System.out.println("Argumento de instancias debe ser al menos 1");
            throw new IllegalArgumentException("ERROR: Faltan argumentos");
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


        //*************************
        //PARA PROBAR-----------
        //ROMPO LOS DATOS, REDUZCO LA CANTIDAD DE DATOS A ALGO CHICO
        Datos datos2 = new Datos();
        datos2.datosBasicos = new DatoBasico();
        int puntosPrueba =10;
        datos2.datosBasicos.cantidadCamiones = 2;
        datos2.datosBasicos.capacidadCamiones = 2;
        datos2.datosBasicos.tiempoRecoleccionContenedor = datos.datosBasicos.tiempoRecoleccionContenedor;


        datos2.puntos = new Punto[puntosPrueba];
        datos2.velocidades = new Velocidad[puntosPrueba];
        datos2.llenados = new LlenadoInicial[puntosPrueba];

        datos2.distancias = new float[puntosPrueba][puntosPrueba];
        datos2.tiempos = new float[puntosPrueba][puntosPrueba];
        for(int i=0; i < puntosPrueba; i++){
            datos2.puntos[i] = datos.puntos[i];
            datos2.velocidades[i] = datos.velocidades[i];
            datos2.llenados[i] = datos.llenados[i];

            for(int j = 0; j < puntosPrueba; j++){
                datos2.distancias[i][j] = datos.distancias[i][j];
                datos2.tiempos[i][j] = datos.tiempos[i][j];
            }
        }

        datos = datos2;

        System.out.println("Total puntos prueba: " + datos.puntos.length);

        //*************************

        return datos;
    }

}
