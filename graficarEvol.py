import matplotlib.pyplot as plt
import matplotlib.animation as animation
import numpy as np
import glob
import os
import time
from natsort import natsorted, natsort_keygen, ns		#Librerias extra!

'''
	Para graficar la evolucion de una ejecucion
'''

ANIMACION = True

files = glob.glob( "./evolucion/*fun.txt") 
files.sort(key = natsort_keygen(key= lambda e : e, alg=ns.U|ns.N|ns.IC))	#Ordeno orden natural segun archivos,

figure = plt.figure()
axes = figure.add_subplot(111)
grafica = None
texto = None

if ANIMACION:
		
	pause = False
	def onClick(event):
		global pause
		pause ^= True
	
	figure.canvas.mpl_connect('button_press_event', onClick)
	
	x = []
	y = []
	if len(files) > 0:
		with open(files[0],'r') as arch:
			for l in arch.readlines():
				if len(l) > 0:				
					f1, f2, nl = l.split(" ")
					x.append(float(f1))
					y.append(float(f2))
	
	inicial = axes.plot(x,y, 'bo',ms=6)[0]	
	
	#La utilizada por la animacion
	grafica = axes.plot([],[], 'ro',ms=6)[0]
	
	texto = axes.text(0.1, 0.9, "{0} / {1}".format(1, len(files)), transform=axes.transAxes)
	
	def getNext():		
		indice = 0
		while indice < len(files):
			if not pause:				
				x = []
				y = []
				with open(files[indice],'r') as arch:
					for l in arch.readlines():
						if len(l) > 0:				
							f1, f2, nl = l.split(" ")
							x.append(float(f1))
							y.append(float(f2))
				indice+=1
				
			yield x, y, indice
		

	def animate(data):
		
		x,y,ind = data
							
		grafica.set_data(x,y)		
		texto.set_text("{0} / {1}".format(ind, len(files)))
		
		#Recalcular limites
		axes.relim()		
		axes.autoscale_view()
		plt.draw()
		return grafica, texto
	
	#frames can be a generator, an iterable, or a number of frames.
	#init_func is a function used to draw a clear frame.
	#If not given, the results of drawing from the first item in the frames sequence will be used.
	#This function will be called once before the first frame.
	line_ani = animation.FuncAnimation(figure, animate, getNext, interval=200, blit=True, repeat=False)
	#line_ani.save('./evolucion/animacion.mp4')
		
	plt.show()
	
	
else:

	x = []
	y = []
	for f in files:
		with open(f,'r') as arch:
			for l in arch.readlines():
				if len(l) > 0:				
					f1, f2, nl = l.split(" ")
					x.append(float(f1))
					y.append(float(f2))
	
	grafica = axes.plot(x,y, 'ro')[0]
		

	plt.show()

