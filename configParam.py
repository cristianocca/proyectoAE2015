import subprocess
import time
import codecs
import json
from os import path
import os
import sys
import re
import xlwt
import threading
from multiprocessing.pool import ThreadPool
import multiprocessing
from threading import Lock

''' 
	Script encargado de ejecutar instancias y generar datos para configuracion parametrica.
'''


def ejecutarProceso(command):
	p = subprocess.Popen(command,
					 stdout=subprocess.PIPE,
					 stderr=subprocess.STDOUT)
	return iter(p.stdout.readline, b'')

	
#Path al jar que ejecuta el algoritmo
PATH_MAIN_AE = r"./out/artifacts/mainAE_jar/mainAE.jar"

#Path para guardar json con datos de ejecucion
PATH_JSON_EJECUCION = r"./salidaParam/ejecucion.json"

#Path/format a carpeta de salidas, donde {0} sera el nombre de instancia
CARPETA_SALIDAS = r"./salidaParam/{0}"


ITERACIONES = 3
POOL_SIZE = 4

#Para parsear resultados de ejecuciones
PARSE_F1_REGEX = re.compile(ur"Compromiso F1: (?P<valor>.+)")
PARSE_F2_REGEX = re.compile(ur"Compromiso F2: (?P<valor>.+)")

#Uso el tiempo reportado por java en vez de calcularo aca, para evitar sumar costo de parseo de archivos
PARSE_TIEMPO_REGEX = re.compile(ur"Tiempo Algoritmo: (?P<valor>.+)s")
	
argumentosBasicos = [
	"java",
	"-jar",
	PATH_MAIN_AE,
	"./datos/datosBasicos.json",
	"./datos/puntos.json", 
	"./datos/velocidades.json",
	"./datos/distancias.json",
	"./datos/tiempos.json",
]

instancias = [
		"./datos/instancias/llenado_1.json",
		"./datos/instancias/llenado_2.json",
		"./datos/instancias/llenado_3.json",
	]
	
algoritmos = ["NSGA2","SPEA2"]


posiblesPob = ["100","200"]
posiblesEval = ["25000"]
posiblesCross = ["0.75","0.8"]
posiblesMut = ["0.01","0.05"]

combinacionesParametros = []
for alg in algoritmos:
	for pob in posiblesPob:
		for eval in posiblesEval:
			for cross in posiblesCross:
				for mut in posiblesMut:
					combinacionesParametros.append([alg, pob,eval,cross,mut])
				
print len(combinacionesParametros), "combinaciones parametricas"

	
#headers = ["algoritmo","poblacion","eval","cross","mut","pcF1","pcF2","tiempoPromedio","carpetaSalida","mediaHV","varHV"]
#book = xlwt.Workbook()
#sheet = book.add_sheet(nombreInstancia)
#for i, h in enumerate(headers):
#	sheet.write(0,i,h)

mutex = Lock()
resultadosEjecucion = {}

def procesarInstancia(indice, instancia):

	try:
		nombreInstancia = path.splitext(path.split(instancia)[1])[0]
		carpetaSalida = CARPETA_SALIDAS.format(nombreInstancia)

		mutex.acquire()
		print "Ejecutando instancia: ", nombreInstancia	
			
		ejecuciones = []
		resultadoInstancia = {'instancia':instancia, 'ejecuciones':ejecuciones}	
		resultadosEjecucion[nombreInstancia] = resultadoInstancia
			
		mutex.release()
		
			
		for ip, p in enumerate(combinacionesParametros):

			mutex.acquire()
			print "Probando parametros: {0} {1} {2} {3} {4}".format(*p)
			mutex.release()
							
			if not path.exists(carpetaSalida):
				try:
					os.makedirs(carpetaSalida)
				except OSError:
					pass
			
			iteraciones = []
			ejecucion = {
				'algoritmo':p[0],
				'poblacion':p[1],
				'evals':p[2],
				'cross':p[3],
				'mut':p[4],
				'iteraciones':iteraciones,		
			}
			archivosFunc = []
			
			for i in xrange(ITERACIONES):
				mutex.acquire()
				print "Iteracion:",i
				mutex.release()
				
				archivoSalidaFun = "{0}/param_{1}_iter_{2}_fun.txt".format(carpetaSalida, ip, i)
				archivoSalidaVar = "{0}/param_{1}_iter_{2}_var.txt".format(carpetaSalida, ip, i)
				
				archivosFunc.append(archivoSalidaFun)
				
				comando = " ".join(argumentosBasicos + [instancia] + p + [archivoSalidaFun, archivoSalidaVar])
				#print comando
				
				f1compromiso = 0
				f2compromiso = 0
				tiempo = 0
			
				for salida in ejecutarProceso(comando):
					#print "Salida java: ", salida
					match = PARSE_F1_REGEX.search(salida)
					if match and match.group("valor"):
						f1compromiso=float(match.group("valor"))
						continue
						
					match = PARSE_F2_REGEX.search(salida)
					if match and match.group("valor"):
						f2compromiso=float(match.group("valor"))
						continue
						
					match = PARSE_TIEMPO_REGEX.search(salida)
					if match and match.group("valor"):
						tiempo=float(match.group("valor"))
						continue
						
				iteraciones.append({
					'iteracion':i,
					'archivoSalidaFun':archivoSalidaFun,
					'archivoSalidaVar':archivoSalidaVar,
					'f1compromiso':f1compromiso,
					'f2compromiso':f2compromiso,
					'tiempo':tiempo,
				})
				
			
			mutex.acquire()
			ejecuciones.append(ejecucion)
			mutex.release()
			
	except Exception as e:
		try:
			mutex.release()
		except:
			pass
			
		print "ERROR EN THREAD: " + str(e)

pool = ThreadPool(processes=POOL_SIZE)


results = []
for i, instancia in enumerate(instancias):
	results.append(pool.apply_async(procesarInstancia, [i, instancia]))
	
for r in results:
	while True:
		try:
			#Para permitir interrupt de keyboard.
			r.get(timeout=1)
			break
		except multiprocessing.TimeoutError:
			pass
		except KeyboardInterrupt:
			try:
				mutex.acquire()
				
				with open(PATH_JSON_EJECUCION,'w') as f:
					print "Guardando json..."
					f.write(json.dumps(resultadosEjecucion, indent=4))
				
				
				res = raw_input("Se ha guardado el progreso, Seguro que quiere terminar? si / no: ")
				if res == "si":
					pool.terminate()
					sys.exit()
			finally:
				mutex.release()
	

try:
	mutex.acquire()
		
	with open(PATH_JSON_EJECUCION,'w') as f:
		print "Guardando json..."
		f.write(json.dumps(resultadosEjecucion, indent=4))
		
finally:
	mutex.release()
	
print "Fin"
	
	