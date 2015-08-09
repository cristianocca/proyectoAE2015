package problema.datos;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;

/**
 * Created by Cristiano on 20/07/2015.
 */
public class Velocidad {

    public String id;
    public double v;

    /**
     * Carga y devuelve una lista de velocidades.
     * @param path
     * @return
     */
    public static Velocidad[] cargar(String path) throws FileNotFoundException {
        Gson gson = new Gson();

        BufferedReader br = new BufferedReader(new FileReader(path));

        Velocidad[] res = gson.fromJson(br, Velocidad[].class);
        //convierto todas las velocidades a segundos. Ya que viene en % / hora
        for(Velocidad vel : res){
            vel.v = (vel.v / 60 ) / 60;
        }

        return res;
    }
}
