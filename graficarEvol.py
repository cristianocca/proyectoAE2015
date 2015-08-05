import matplotlib.pyplot as plt
import matplotlib.animation as animation
import numpy as np
import glob
import os
import time
from natsort import natsorted, natsort_keygen, ns		#Librerias extra!

CARPETA = "./evolucion/"


files = glob.glob( "./evolucion/*fun.txt") 
files.sort(key = natsort_keygen(key= lambda e : e, alg=ns.U|ns.N|ns.IC))	#Ordeno orden natural segun archivos,

it = iter(files)

figure = plt.figure()
grafica = None

def init():
	global grafica
	try:
		f = it.next()		
		x = []
		y = []
		with open(f,'r') as arch:
			for l in arch.readlines():
				if len(l) > 0:				
					f1, f2, nl = l.split(" ")
					x.append(f1)
					y.append(f2)
				
		#grafica.set_data(x,y)
		grafica = plt.plot(x,y, 'ro')[0]
		return grafica,
	except StopIteration:
		return grafica,

def animate(num):
	global grafica
	try:
		f = it.next()		
		x = []
		y = []
		with open(f,'r') as arch:
			for l in arch.readlines():
				if len(l) > 0:				
					f1, f2, nl = l.split(" ")
					x.append(f1)
					y.append(f2)
					
								
		grafica.set_data(x,y)		
		
		#Recalcular limites
		ax = plt.gca()		
		ax.relim()		
		ax.autoscale_view()
		plt.draw()
		return grafica,
	except StopIteration:
		return grafica,


line_ani = animation.FuncAnimation(figure, animate, len(files), interval=500, init_func=init, blit=True)
#init()

plt.show()

