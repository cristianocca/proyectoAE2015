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
import math
import numpy

''' 
	Script encargado de ejecutar instancias y generar datos para configuracion parametrica.
	
	OJO: Se utiliza numpy, y xlwt.
'''


def ejecutarProceso(command):
	p = subprocess.Popen(command,
					 stdout=subprocess.PIPE,
					 stderr=subprocess.STDOUT)
	return iter(p.stdout.readline, b'')

#Pasos a ejecutar, si se saltean pasos, se asumen datos ya generados.		
class PASOS:
	EJECUTAR = False			#Ejecuta instancias y guarda resultados para post procesamiento
	OBTENER_FRENTE = False		#Por cada instancia, obtiene el mejor frente de todas las ejecuciones de esa instancia
	OBTENER_RHV = True			#Por cada instancia, el mejor frente, y todas las ejecuciones, obtiene RHV, promedios y varianzas para cada combinacion parametrica
	GENERAR_XL = True			#Guarda todos los datos obtenidos en un archivo excel


	
#Paths a jars
PATH_MAIN_AE = r"./out/artifacts/mainAE_jar/mainAE.jar"
PATH_OBTENER_FRENTE = r"./out/artifacts/obtenerFrente_jar/obtenerFrente.jar"
PATH_OBTENER_RHV = r"./out/artifacts/obtenerRHV_jar/obtenerRHV.jar"

#Path para guardar json con datos de ejecucion
PATH_JSON_EJECUCION = r"./salidaParam/ejecucion.json"

#Path/format a carpeta de salidas, donde {0} sera el nombre de instancia
CARPETA_SALIDAS = r"./salidaParam/{0}"

#Path salida excel
PATH_SALIDA_XL = r"./salidaParam/resultados.xls"

ITERACIONES = 3
POOL_SIZE = 4


if PASOS.EJECUTAR:

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
		
	#algoritmos = ["NSGA2","SPEA2"]
	algoritmos = ["NSGA2"]


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
	

	mutex = Lock()
	resultadosEjecucion = {}

	def procesarInstancia(indice, instancia):

		try:
			nombreInstancia = path.splitext(path.split(instancia)[1])[0]
			carpetaSalida = CARPETA_SALIDAS.format(nombreInstancia)

			mutex.acquire()
			print "Ejecutando instancia: ", nombreInstancia	
				
			ejecuciones = []
			resultadoInstancia = {'instancia':instancia, 'carpetaSalida':carpetaSalida, 'ejecuciones':ejecuciones}	
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
	
else:	
	with open(PATH_JSON_EJECUCION,'r') as f:
		print "Cargando json"
		resultadosEjecucion = json.loads(f.read())
	
if PASOS.OBTENER_FRENTE:
	print "Obteniendo frentes de cada instancia"
		
	for k,v in resultadosEjecucion.iteritems():
		v['mejorFrente'] = v['carpetaSalida']+"/MEJOR_FRENTE.txt"
		comando = " ".join(["java","-jar",PATH_OBTENER_FRENTE,v['carpetaSalida'], v['mejorFrente']])		
		#print comando
		for salida in ejecutarProceso(comando):
			print salida
		
	#Guardo json nuevamente con mejores frentes
	with open(PATH_JSON_EJECUCION,'w') as f:
		print "Guardando json..."
		f.write(json.dumps(resultadosEjecucion, indent=4))
		
		
if PASOS.OBTENER_RHV:
	
	PARSE_RHV = re.compile(ur"RHV: (?P<valor>.+)")
	PARSE_SPREAD = re.compile(ur"Spread: (?P<valor>.+)")
	PARSE_GD = re.compile(ur"^GD: (?P<valor>.+)")
	PARSE_IGD = re.compile(ur"^IGD: (?P<valor>.+)")
	
	print "Obteniendo RHV y otros"
		
	for k,v in resultadosEjecucion.iteritems():
	
		for ejecucion in v['ejecuciones']:
			
			compromisosF1 = []
			compromisosF2 = []
			tiempos = []
			RHVs = []
			spreads = []
			gds = []
			igds = []
						
			for iteracion in ejecucion['iteraciones']:
				compromisosF1.append(iteracion['f1compromiso'])
				compromisosF2.append(iteracion['f2compromiso'])
				tiempos.append(iteracion['tiempo'])
				
				comando = " ".join(["java","-jar",PATH_OBTENER_RHV,iteracion['archivoSalidaFun'], v['mejorFrente']])		
				#print comando
				for salida in ejecutarProceso(comando):
					
					match = PARSE_RHV.search(salida)
					if match and match.group("valor"):
						RHVs.append(float(match.group("valor")))
						continue
						
					match = PARSE_SPREAD.search(salida)
					if match and match.group("valor"):
						spreads.append(float(match.group("valor")))
						continue
						
					match = PARSE_GD.search(salida)
					if match and match.group("valor"):
						gds.append(float(match.group("valor")))
						continue
						
					match = PARSE_IGD.search(salida)
					if match and match.group("valor"):
						igds.append(float(match.group("valor")))
						continue
						
						
			ejecucion['medCompromisoF1'] = numpy.mean(compromisosF1)
			ejecucion['medCompromisoF2'] = numpy.mean(compromisosF2)
			ejecucion['medTiempo'] = numpy.mean(tiempos)
			
			ejecucion['medRHV'] = numpy.mean(RHVs)
			ejecucion['varRHV'] = numpy.std(RHVs)
			
			ejecucion['medSpread'] = numpy.mean(spreads)
			ejecucion['varSpread'] = numpy.std(spreads)
			
			ejecucion['medGD'] = numpy.mean(gds)
			ejecucion['varGD'] = numpy.std(gds)
			
			ejecucion['medIGD'] = numpy.mean(igds)
			ejecucion['varIGD'] = numpy.std(igds)
			
				
		
	with open(PATH_JSON_EJECUCION,'w') as f:
		print "Guardando json..."
		f.write(json.dumps(resultadosEjecucion, indent=4))
		
	
if PASOS.GENERAR_XL:

	headers = ["algoritmo","poblacion","evals","cross","mut","medCompromisoF1","medCompromisoF2","medTiempo","medRHV","varRHV", "medSpread","varSpread","medGD","varGD","medIGD","varIGD"]
	
	book = xlwt.Workbook()
	
	print "Generando excel"
	
	for k,v in resultadosEjecucion.iteritems():
	
		sheet = book.add_sheet(k)
		
		for i, h in enumerate(headers):
			sheet.write(0,i,h)
	
		for i, ejecucion in enumerate(v['ejecuciones']):
			sheet.write(i+1, 0, ejecucion['algoritmo'])
			sheet.write(i+1, 1, ejecucion['poblacion'])
			sheet.write(i+1, 2, ejecucion['evals'])
			sheet.write(i+1, 3, ejecucion['cross'])
			sheet.write(i+1, 4, ejecucion['mut'])
			sheet.write(i+1, 5, ejecucion['medCompromisoF1'])
			sheet.write(i+1, 6, ejecucion['medCompromisoF2'])
			sheet.write(i+1, 7, ejecucion['medTiempo'])
			
			sheet.write(i+1, 8, ejecucion['medRHV'])
			sheet.write(i+1, 9, ejecucion['varRHV'])
			
			sheet.write(i+1, 10, ejecucion['medSpread'])
			sheet.write(i+1, 11, ejecucion['varSpread'])
			
			sheet.write(i+1, 12, ejecucion['medGD'])
			sheet.write(i+1, 13, ejecucion['varGD'])
			
			sheet.write(i+1, 14, ejecucion['medIGD'])
			sheet.write(i+1, 15, ejecucion['varIGD'])
	
	
	book.save(PATH_SALIDA_XL)
		
		
print "Fin"
	
	