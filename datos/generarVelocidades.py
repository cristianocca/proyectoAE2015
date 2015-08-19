import json
import random

#Genera instancias de valores de llenado

RUTA_PUNTOS = "./puntos.json"
RUTA_SALIDA = "./velocidades.json"


with open(RUTA_PUNTOS,'r') as f:
	puntos = json.loads(f.read())
	

velocidades = []
for v in puntos:
	vel = random.randint(1, 2)
	
	velocidades.append({'id':v['id'],'v':vel})
	
with open(RUTA_SALIDA,'w') as f:
	f.write(json.dumps(velocidades))