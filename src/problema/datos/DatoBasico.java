package problema.datos;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;

/**
 * Created by Cristiano on 20/07/2015.
 */
public class DatoBasico {

    public int cantidadCamiones;
    public int capacidadCamiones;               //contenedores
    public int tiempoRecoleccionContenedor;     //segundos
    public int tiempoTrabajo;                   //horas


    public static DatoBasico cargar(String path) throws FileNotFoundException {
        Gson gson = new Gson();
        BufferedReader br = new BufferedReader(new FileReader(path));

        return gson.fromJson(br, DatoBasico.class);
    }
}
