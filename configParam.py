import subprocess
import time
import codecs
import json
from os.path import splitext, split
import re
import xlwt

def ejecutarProceso(command):
	p = subprocess.Popen(command,
					 stdout=subprocess.PIPE,
					 stderr=subprocess.STDOUT)
	return iter(p.stdout.readline, b'')

	
PATH_JAR = r"./out/artifacts/proyectoAE_jar/proyectoAE.jar"
PATH_RESULTADOS = r"./salidaParam/resultados.xls"
ITERACIONES = 10

PARSE_F1_REGEX = re.compile(ur"Funcion Objetivo 1: (?P<valor>.+)")
PARSE_F2_REGEX = re.compile(ur"Funcion Objetivo 2: (?P<valor>.+)")

#Uso el tiempo reportado por java en vez de calcularo aca, para evitar sumar costo de parseo de archivos
PARSE_TIEMPO_REGEX = re.compile(ur"Tiempo Algoritmo: (?P<valor>.+)s")
	
argumentosBasicos = [
	"java",
	"-jar",
	PATH_JAR,
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



#comando = " ".join(argumentosBasicos + instancia + argumentosParams)
#for salida in ejecutarProceso(comando):
#	print salida
	
headers = ["algoritmo","poblacion","eval","cross","mut","promedioF1","promedioF2","tiempoPromedio"]

book = xlwt.Workbook()

for instancia in instancias:
	print "Ejecutando instancia: ", instancia
	
	sheet = book.add_sheet(splitext(split(instancia)[1])[0])
	
	for i, h in enumerate(headers):
		sheet.write(0,i,h)
	
		
	for ip, p in enumerate(combinacionesParametros):

		print "Probando parametros: {0} {1} {2} {3} {4}".format(*p)
		
		sumaF1 = 0.0
		sumaF2 = 0.0
		tiempo = 0.0
		
		for i in xrange(ITERACIONES):
			print "Iteracion:",i
			comando = " ".join(argumentosBasicos + [instancia] + p)
			for salida in ejecutarProceso(comando):
				match = PARSE_F1_REGEX.search(salida)
				if match and match.group("valor"):
					sumaF1+=float(match.group("valor"))
					continue
					
				match = PARSE_F2_REGEX.search(salida)
				if match and match.group("valor"):
					sumaF2+=float(match.group("valor"))
					continue
					
				match = PARSE_TIEMPO_REGEX.search(salida)
				if match and match.group("valor"):
					tiempo+=float(match.group("valor"))
					continue
		
		line = ip+1
		sheet.write(line,0,p[0])
		sheet.write(line,1,p[1])
		sheet.write(line,2,p[2])
		sheet.write(line,3,p[3])
		sheet.write(line,4,p[4])
		sheet.write(line,5,sumaF1 / ITERACIONES)
		sheet.write(line,6,sumaF2 / ITERACIONES)
		sheet.write(line,7,tiempo / ITERACIONES)
		
		#Guardo por las dudas
		book.save(PATH_RESULTADOS)
	
	