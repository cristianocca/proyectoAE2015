package problema.datos;

import com.google.gson.Gson;

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

}
