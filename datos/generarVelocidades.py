import json
import random

#Genera instancias de valores de llenado

RUTA_PUNTOS = "./puntos.json"
RUTA_SALIDA = "./velocidades.json"


with open(RUTA_PUNTOS,'r') as f:
	puntos = json.loads(f.read())
	

#Si el id del punto esta entre 300 y 1200, aumento un poco mas su velocidad de llenado
#Ya que sabemos que el centro, cordon, y cercanias, es una parte muy poblada de montevideo
#Y ademas sabemos que los puntos en esa region son los con id 300-1200, mas o menos.

velocidades = []
for v in puntos:
	if v['id'] >= 300 and v['id'] <= 1200:
		vel = random.randint(20, 35)
	else:
		vel = random.randint(5, 25)
	
	velocidades.append({'id':v['id'],'v':vel})
	
with open(RUTA_SALIDA,'w') as f:
	f.write(json.dumps(velocidades))