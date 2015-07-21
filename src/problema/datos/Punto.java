package problema.datos;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;

/**
 * Created by Cristiano on 20/07/2015.
 * Clase encargada de almacenar informacion sobre puntos.
 */
public class Punto {

    public String id;
    public float lat;
    public float lon;

    /**
     * Carga y devuelve una lista de puntos.
     * El punto en res[0] Siempre se asume es el origen, si no, el archivo esta mal.
     * @param path
     * @return
     */
    public static Punto[] cargar(String path) throws FileNotFoundException {
        Gson gson = new Gson();

        BufferedReader br = new BufferedReader(new FileReader(path));

        return gson.fromJson(br, Punto[].class);
    }
}
