from urllib import urlopen
from json import loads, dumps, dump, load
import sys
import signal
import time
from datetime import datetime, timedelta
import codecs
from operator import itemgetter, attrgetter
from functools import partial
import threading
from threading import Lock
import requests
from requests import Session

'''
    INFO:

    - El archivo de puntos debe estar en formato JSON, donde debe ser una lista de la forma
        [{"id":1234, "lat":-4123.123", "lon":-123.21"}, ...]
        Donde id puede ser numerico o string

    - El archivo de matriz de distancia debe ser un array en formato JSON de la forma:
        - Cada indice de la matriz debe corresponderse con el indice del archivo de puntos, sino los resultados seran invalidos
        - Es exactamente una matriz, de la forma [ [1.123,233.43,4....], [123.22,123123.123,324234.32], ... ] donde cada fila contiene
        la distancia a cada uno de los puntos, se espera que la diagonal contenga 0....
        - La distancia se encuentra en metros

    - El archivo estado contiene estado de la ejecucion actual y ultima, se debe especificar pero no necesariamente estar presente

    - El archivo salida distancias contiene la salida. El formato de la salida es un archivo JSON, el cual contiene una matriz de la forma.
        [ [ distancia, distancia distancia ] ] Donde cada fila contiene la distancia del elemento de la fila al de la columna 
        y los indices corresponden con los de la entrada de puntos.

        - distancia en metros

    - El archivo salida tiempos es analogo al distancias pero para los tiempos necesarios para ir de un punto a otro
        - tiempo en segundos.



'''

ARCHIVO_PUNTOS = "./puntos.json"
ARCHIVO_MATRIZ_DISTANCIA = "./matriz_distancia.json"
ARCHIVO_ESTADO = "./progreso.json"
ARCHIVO_SALIDA_DISTANCIAS = "./salida_distancias.json"
ARCHIVO_SALIDA_TIEMPOS = "./salida_tiempos.json"

MAX_RETRIES = 300
ROUTER_RESET_ERRORS = 3 #Errors necesarios para iniciar reinicio de router
TIEMPO_ENTRE_RESTARTS = timedelta(minutes=7)    #Tiempo minimo para aceptar un restart de router

POOL_SIZE = 3

urlFormat = "http://maps.googleapis.com/maps/api/distancematrix/json?origins={0:.15f},{1:.15f}&destinations={2:.15f},{3:.15f}&mode=driving"

MUTEX = Lock()
EXIT = False

puntos = None
matriz = None
estado = {'pendientes':None}
requestsNecesarios = 0
nuevaSalida = True

resultadoDistancias = None
resultadoTiempos = None

matrizFilas = 0
matrizCols = 0


def guardarEstado():
    MUTEX.acquire()
    try:
        try:
            with open(ARCHIVO_SALIDA_DISTANCIAS,'w') as f:
                dump(resultadoDistancias,f)
        except Exception as e:
            raise Exception("Error al guardar archivo de distancias: " + str(e))

        try:
            with open(ARCHIVO_SALIDA_TIEMPOS,'w') as f:
                dump(resultadoTiempos,f)
        except Exception as e:
            raise Exception("Error al guardar archivo de tiempos: " + str(e))

        try:
            with open(ARCHIVO_ESTADO,'w') as f:
                dump(estado,f)
        except Exception as e:
            raise Exception("Error al guardar archivo de estado: " + str(e))

    finally:
        MUTEX.release()


def getNextPendiente():
    MUTEX.acquire()
    try:
        for k in estado['pendientes'].iterkeys():
            pendiente = estado['pendientes'][k]
            if pendiente[2] == 0:
                pendiente[2] = 1
                return k, pendiente

        return None, None
    finally:
        MUTEX.release()

def eliminarPendiente(id):
    MUTEX.acquire()
    try:
        estado['pendientes'].pop(id, None)        
        print "Restantes: {0}".format(len(estado['pendientes']))
    finally:
        MUTEX.release()


def setResultados(i, j, d, t):
    MUTEX.acquire()
    try:
        resultadoDistancias[i][j] = d
        resultadoTiempos[i][j] = t
    finally:
        MUTEX.release()


import re
routerParseRegex = re.compile(ur'getObj\("Frm_Logintoken"\)\.value\s=\s"(\d+)";')
lastRestart = datetime.now() - TIEMPO_ENTRE_RESTARTS
def restartRouter():

    MUTEX.acquire()

    try:
        global lastRestart

        if datetime.now() - lastRestart < TIEMPO_ENTRE_RESTARTS:
            print "Reinicio de router no aceptado, no paso suficiente tiempo"
            return

        s = Session()
        data = {
            'action': 'login',
            'frashnum':'',
            'Frm_Logintoken':'0',
            'Username': 'instalador',
            'Password': 'wwzz2233',

            }
        #Inicio sesion y obtengo cookie
        try:
            print "Reiniciando router"
            print "Iniciando sesion en router..."
            r = s.get('http://192.168.1.1/')

            try:    
                data['Frm_Logintoken'] = routerParseRegex.search(r.text).group(1)
            except:
                print "Error obteniendo login token... intentando de todas formas"

            r = s.post('http://192.168.1.1/', data=data)
            
            print "Cookie de session obtenida: " + s.cookies.get('SID','NINGUNA')
            
                        
            print "Reiniciando router..."
            data = {
                'IF_ACTION':'devrestart',
                'IF_ERRORSTR':'SUCC',
                'IF_ERRORPARAM':'SUCC',
                'IF_ERRORTYPE':'-1',
                'flag':'1'
            }
            r = s.post('http://192.168.1.1/getpage.gch?pid=1002&nextpage=manager_dev_conf_t.gch', data=data)

            print "Router reiniciado"

            lastRestart = datetime.now()

        except Exception as e:
            raise Exception("Error en login: "+str(e))


    except Exception as e:
        print "Error al reiniciar router: "+str(e)
    finally:
        MUTEX.release()

if __name__ == "__main__":
    
    try:           
    
        try:
            with codecs.open(ARCHIVO_PUNTOS,'r','UTF-8','ignore') as f:
                puntos = load(f)            

            if len(puntos) < 2:
                raise Exception("Deben haber al menos dos puntos")

            for punto in puntos:
                if not "id" in punto or not "lat" in punto or not "lon" in punto:
                    raise Exception("El archivo de puntos debe tener id, lat, y lon")
                try:
                    float(punto["lat"])
                    float(punto["lon"])
                except:
                    raise Exception("Latitid y longitud deben ser numeros reales")

        except Exception as e:        
            raise Exception("No se pudo leer archivo de puntos o formato invalido:" + str(e))

        try:
            with codecs.open(ARCHIVO_MATRIZ_DISTANCIA,'r','UTF-8','ignore') as f:
                matriz = load(f)


            matrizFilas = len(matriz)
            if matrizFilas < 1:
                raise Exception("La matriz debe tener al menos una fila")

            matrizColumnas = len(matriz[0])

            #Valido matriz y al mismo tiempo sumo cuantos requests necesitaria
            try:
            
                for fila in matriz:
                    if len(fila) !=  matrizColumnas:
                        raise Exception("La matriz es invalida, todas las filas deben tener la misma cantidad de columnas")

                    for col in fila:
                        float(col)
                        requestsNecesarios+=1

            except Exception as e:
                raise Exception("Datos de matriz invalidos: " + str(e))
            
        except Exception as e:
            raise Exception("No se pudo leer archivo de matriz o formato invalido: " + str(e))
        

        try:
            with codecs.open(ARCHIVO_ESTADO,'r','UTF-8','ignore') as f:
                estado = load(f)
        except:        
            print "WARNING: no se encontro archivo de estado, empezando de 0"

        try:                                
            nuevaSalida = estado['pendientes'] is None or not isinstance(estado['pendientes'], dict)
            
        except:
            raise Exception("Datos de estado son incorrectos.")

        if not nuevaSalida:
            #Recargo estado anterior.

            try:
                with codecs.open(ARCHIVO_SALIDA_DISTANCIAS,'r','UTF-8','ignore') as f:
                    resultadoDistancias = load(f)

                #Valido matriz
                if len(resultadoDistancias) != matrizFilas:
                    raise Exception("La matriz es distancias es invalida, no coincide con las filas necesarias.")

                for fila in resultadoDistancias:
                    if len(fila) !=  matrizColumnas:
                        raise Exception("La matriz es distancias es invalida, no coincide con las columnas necesarias.")

                    for col in fila:
                        float(col)

            except Exception as e:
                raise Exception("Archivo de salida de distancias es invalido, no se pudo recargar estado: " + str(e))


            try:
                with codecs.open(ARCHIVO_SALIDA_TIEMPOS,'r','UTF-8','ignore') as f:
                    resultadoTiempos = load(f)

                 #Valido matriz
                if len(resultadoTiempos) != matrizFilas:
                    raise Exception("La matriz es tiempos es invalida, no coincide con las filas necesarias.")

                for fila in resultadoTiempos:
                    if len(fila) !=  matrizColumnas:
                        raise Exception("La matriz es tiempos es invalida, no coincide con las columnas necesarias.")

                    for col in fila:
                        float(col)

            except Exception as e:
                raise Exception("Archivo de salida de tiempos es invalido, no se pudo recargar estado: " + str(e))


            #Marco cualquier pendiente que estaba siendo procesado como no procesado
            for p in estado['pendientes'].itervalues():
                p[2] = 0

        else:
            #Inicializo matrices
            resultadoDistancias = [ [ 0 for j in xrange(matrizColumnas)] for i in xrange(matrizFilas)] 
            resultadoTiempos = [ [ 0 for j in xrange(matrizColumnas)] for i in xrange(matrizFilas)] 

            pendientes = {}
            for i in xrange(matrizFilas):
                for j in xrange(matrizColumnas):
                    pendientes["{0}-{1}".format(i,j)] = [i,j,0]

            estado['pendientes'] = pendientes

        print "Restantes: {0}".format(len(estado['pendientes']))
        print "Matriz de tamanio: {0} filas x {1} columnas".format(matrizFilas, matrizColumnas)
    
        print "*" * 25
        print "Empezando proceso... Apretar control-c para parar en cualquier momento y guardar estado, de lo contrario se perderan cambios."
        print "*" * 25


        def ejecucion():
            global EXIT

            try:
                id, pendiente = getNextPendiente()

                while pendiente is not None:
                    intento = 0
                    i = pendiente[0]
                    j = pendiente[1]

                    while intento <= MAX_RETRIES:
                        if EXIT:
                            return

                        try:
                            url = urlFormat.format(puntos[i]['lat'], puntos[i]['lon'], puntos[j]['lat'], puntos[j]['lon'])
                            r = requests.get(url).json()
                                                
                            if r['status'] == "OK" and r['rows'][0]['elements'][0]['status'] == "OK":
                                d = float(r['rows'][0]['elements'][0]['distance']['value'])
                                t = float(r['rows'][0]['elements'][0]['duration']['value'])
                                                
                                setResultados(i, j, d, t)
                                break

                            else:                                
                                intento+=1    
                                errorMsg = r['status']
                                errorMsg = r['rows'][0]['elements'][0]['status'] if errorMsg == "OK" else errorMsg
                                print "Error en respuesta de google: " + errorMsg
                                time.sleep(10)

                                if intento > ROUTER_RESET_ERRORS:
                                    restartRouter()

                        except Exception as e:
                            intento+=1
                            print "Error obteniendo datos de servidor: " + str(e)
                            print "Reintento: " + str(intento)
                            time.sleep(10)

                            if intento > MAX_RETRIES:
                                raise KeyboardInterrupt("Superada capacidad de reintentos, finalizando")
                    
                    eliminarPendiente(id)                                           
                    id, pendiente = getNextPendiente()

            except KeyboardInterrupt:
                print "Keyboard interrupt en thread, terminando thread"
            except Exception as e:
                print "Error no capturado en thread, terminando thread: " + str(e)
                        
        threads = []
        try:
            
            for i in xrange(POOL_SIZE):
                t = threading.Thread(target=ejecucion)
                t.setDaemon(True)    
                t.start()
                threads.append(t)

            for t in threads:
                while True: #Se hace asi para no quedarse bloqueado siempre y agarrar los key interrupt
                    t.join(0.5)
                    if not t.is_alive():
                        break
                    
        
            print " ********************* PROCESO FINALIZADO ***************************"
            guardarEstado()
        
        except KeyboardInterrupt:
            print "Esperando threads... Control-C otra vez si no quiere esperar"
            EXIT = True
            try:
                for t in threads:
                    while True:
                        t.join(0.5)
                        if not t.is_alive():
                            break

            except KeyboardInterrupt:
                pass

            print "Guardando estado y datos actuales..."
            guardarEstado()
            print "Estado guardado"
            sys.exit(1)
        except Exception as e:
            print "Error no capturado: " + str(e)
            print "Intentando guardar estado..."
            guardarEstado()
            print "Estado guardado"
            sys.exit(1)

    except Exception as e:
        print "Error en ejecucion, finalizando: "+str(e)

