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
from scipy import stats

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
	EJECUTAR = True			#Ejecuta instancias y guarda resultados para post procesamiento
	OBTENER_FRENTE = True		#Por cada instancia, obtiene el mejor frente de todas las ejecuciones de esa instancia
	OBTENER_RHV = True			#Por cada instancia, el mejor frente, y todas las ejecuciones, obtiene RHV y otros valores, promedios y varianzas para cada combinacion parametrica
	GENERAR_XL = True			#Guarda todos los datos obtenidos en un archivo excel

MIN_PVAL = 0.05
	
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

ITERACIONES = 30
POOL_SIZE = 4


if PASOS.EJECUTAR:

	#Para parsear resultados de ejecuciones
		
	PARSE_F1_COMPR = re.compile(ur"Compromiso F1: (?P<valor>.+)")
	PARSE_F2_COMPR = re.compile(ur"Compromiso F2: (?P<valor>.+)")
	
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
	#algoritmos = ["NSGA2"]


	posiblesPob = ["100","200"]
	posiblesEval = ["45000"]
	posiblesCross = ["0.75","0.9"]
	posiblesMut = ["0.01","0.05","0.1"]

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
					
					f1peor = 0
					f1mejor = 0
					f2peor = 0
					f2mejor = 0
					f1compromiso = 0
					f2compromiso = 0
					tiempo = 0
								
					for salida in ejecutarProceso(comando):
						#print "Salida java: ", salida
																		
						match = PARSE_F1_COMPR.search(salida)
						if match and match.group("valor"):
							f1compromiso=float(match.group("valor"))
							continue
							
						match = PARSE_F2_COMPR.search(salida)
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
	PARSE_GD = re.compile(ur"GD: (?P<valor>.+)")
	
	print "Obteniendo RHV y otros"
		
	for k,v in resultadosEjecucion.iteritems():
	
		for ejecucion in v['ejecuciones']:
									
			f1s = []
			f2s = []
			
			compromisosF1 = []
			compromisosF2 = []
			tiempos = []
			RHVs = []
			gds = []
						
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
						
					match = PARSE_GD.search(salida)
					if match and match.group("valor"):
						gds.append(float(match.group("valor")))
						continue
												
				
				#Leo el archivo de salida y le agrego valores funcionales
				with open(iteracion['archivoSalidaFun'],'r') as arch:
					for l in arch.readlines():
						if len(l) > 0:				
							f1, f2, nl = l.split(" ")
							f1s.append(float(f1))
							f2s.append(float(f2))
						
									
			ejecucion['peorF1'] = max(f1s)
			ejecucion['mejorF1'] = min(f1s)			
			ejecucion['medF1'] = numpy.mean(f1s)
			ejecucion['peorF2'] = max(f2s)
			ejecucion['mejorF2'] = min(f2s)
			ejecucion['medF2'] = numpy.mean(f2s)
						
			ejecucion['medCompromisoF1'] = numpy.mean(compromisosF1)			
			ejecucion['medCompromisoF2'] = numpy.mean(compromisosF2)
			
			ejecucion['medTiempo'] = numpy.mean(tiempos)
			
			ejecucion['medRHV'] = numpy.mean(RHVs)
			ejecucion['varRHV'] = numpy.std(RHVs)			
			ejecucion['ksRHVpval'] = stats.kstest(RHVs,'norm', args=(ejecucion['medRHV'],ejecucion['varRHV'])).pvalue if ejecucion['varRHV'] else 0
			ejecucion['shapiroRHVpval'] = stats.shapiro(RHVs)[1] if ejecucion['varRHV'] else 0
			
			ejecucion['medGD'] = numpy.mean(gds)
			ejecucion['varGD'] = numpy.std(gds)
			ejecucion['ksGDpval'] = stats.kstest(gds,'norm', args=(ejecucion['medGD'],ejecucion['varGD'])).pvalue if ejecucion['varGD'] else 0
			ejecucion['shapiroGDpval'] = stats.shapiro(gds)[1] if ejecucion['varGD'] else 0
							
		
	with open(PATH_JSON_EJECUCION,'w') as f:
		print "Guardando json..."
		f.write(json.dumps(resultadosEjecucion, indent=4))
		
	
if PASOS.GENERAR_XL:

	headers = ["algoritmo","poblacion","evals","cross","mut",
				'peorF1','mejorF1','medF1',
				'peorF2','mejorF2','medF2',
				"medCompromisoF1",
				"medCompromisoF2",				
				"medTiempo",
				"medRHV","varRHV",'ksRHVpval', 'shapiroRHVpval',
				"medGD","varGD", 'ksGDpval', 'shapiroGDpval']
	
	book = xlwt.Workbook()
	
	colorVerde = xlwt.easyxf('pattern: pattern solid, fore_colour light_green')
	colorRojo = xlwt.easyxf('pattern: pattern solid, fore_colour red')
	
	print "Generando excel"
	
	for k,v in resultadosEjecucion.iteritems():
	
		sheet = book.add_sheet(k)
		
		for i, h in enumerate(headers):
			sheet.write(0,i,h)
	
		for i, ejecucion in enumerate(v['ejecuciones']):
			for colnum, col in enumerate(headers):
				if "pval" in col:
					sheet.write(i+1, colnum, ejecucion[col], colorVerde if ejecucion[col] >= MIN_PVAL else colorRojo)
				else:
					sheet.write(i+1, colnum, ejecucion[col])
	
	
	book.save(PATH_SALIDA_XL)
		
		
print "Fin"
	
	
"""

import numpy
from scipy import stats

n1 = numpy.random.normal(0,1,100)
n2 = stats.norm.rvs(loc=0, scale=1, size=100)

stats.kstest(n1,'norm', args=(0,1))	--kolmogorov smirnov
stats.shapiro(numpy.random.normal(0,5,30))	--Shapiro-Wilk
stats.normaltest(numpy.random.normal(0,5,30))	--Test DAgostino-Pearson
"""
	