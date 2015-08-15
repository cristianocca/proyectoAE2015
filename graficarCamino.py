import matplotlib.pyplot as plt
import matplotlib.animation as animation
import numpy as np
import glob
import os
import time
from natsort import natsorted, natsort_keygen, ns		#Librerias extra!
import json

'''
	Para graficar un camino
'''

ARCHIVO_PUNTOS = "./datos/puntos.json"
ARCHIVO_SALIDA = "./SALIDA.txt" #Con formato de imprimir solucion

def getNextColor():
	while True:
		yield 'r-'
		yield 'g-'
		yield 'k-'
		yield 'c-'
		yield 'm-'
		yield 'y-'
		yield 'w-'

		yield 'r--'
		yield 'g--'
		yield 'k--'
		yield 'c--'
		yield 'm--'
		yield 'y--'
		yield 'w--'

figure = plt.figure()
axes = figure.add_subplot(111)

with open(ARCHIVO_PUNTOS,'r') as f:
	puntos = json.loads(f.read())
	
with open(ARCHIVO_SALIDA, 'r') as f:
	salida = f.readlines()
	
colores = getNextColor()
for i, camion in enumerate(salida[0].split("|")[0:10]):
	puntoActual = puntos[0]
	recorridoX = [float(puntoActual["lat"])]
	recorridoY = [float(puntoActual["lon"])]	
	color = colores.next()
	axes.plot(recorridoX,recorridoY, color[0]+".", ms=10)	#Punto inicial
	for j, punto in enumerate(camion.split(" ")):
		punto = punto.strip()			
		if punto:
			puntoInt = int(punto)
			if puntoInt != 0 and puntoInt < len(puntos):
				puntoActual = puntos[int(punto)]
				
				recorridoX.append(float(puntoActual["lat"]))
				recorridoY.append(float(puntoActual["lon"]))
			
			
	axes.plot([recorridoX[-1]],[recorridoY[-1]], color[0]+"x", ms=10)		#Punto final
	axes.plot(recorridoX,recorridoY, color, ms=10)

		

	
axes.relim()		
axes.autoscale_view()
axes.legend()
plt.show()