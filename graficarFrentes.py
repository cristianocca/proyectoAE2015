import matplotlib.pyplot as plt
import matplotlib.animation as animation
import numpy as np
import glob
import os
import time
from natsort import natsorted, natsort_keygen, ns		#Librerias extra!


'''
	Para graficar y comparar frentes
'''

files = glob.glob( "./frentes/*fun*.txt") 
mejorFrente = "./frentes/MEJOR_FRENTE.txt"

figure = plt.figure()
axes = figure.add_subplot(111)


def getNextColor():
	while True:
		yield 'ro'
		yield 'go'
		yield 'ko'
		yield 'co'
		yield 'mo'
		yield 'yo'
		yield 'wo'

		yield 'rs'
		yield 'gs'
		yield 'ks'
		yield 'cs'
		yield 'ms'
		yield 'ys'
		yield 'ws'
		
colores = getNextColor()
for f in files:
	with open(f,'r') as arch:
		x = []
		y = []
		for l in arch.readlines():
			if len(l) > 0:								
				data = l.split(" ")
				if len(data) >= 2:
					f1, f2 = data[:2]
					x.append(float(f1))
					y.append(float(f2))
		
		axes.plot(x,y, colores.next(), ms=5, label=f)
		
try:
	with open(mejorFrente,'r') as arch:
		x = []
		y = []
		for l in arch.readlines():
			if len(l) > 0:				
				data = l.split(" ")
				if len(data) >= 2:
					f1, f2 = data[:2]
					x.append(float(f1))
					y.append(float(f2))

		axes.plot(x,y, 'bx', ms=10, label=mejorFrente)
except:
	pass
	
axes.relim()		
axes.autoscale_view()
axes.legend()
plt.show()

