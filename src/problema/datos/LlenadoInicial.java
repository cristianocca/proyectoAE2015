package problema.datos;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;

/**
 * Created by Cristiano on 20/07/2015.
 */
public class LlenadoInicial {

    public String id;
    public int v;


    public static LlenadoInicial[] cargar(String path) throws FileNotFoundException {
        Gson gson = new Gson();

        BufferedReader br = new BufferedReader(new FileReader(path));

        return gson.fromJson(br, LlenadoInicial[].class);
    }
}
