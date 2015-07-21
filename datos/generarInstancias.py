import json
import random

#Genera instancias de valores de llenado

RUTA_PUNTOS = "./puntos.json"
RUTA_SALIDA = "./instancias/llenado_{0}.json"
CANT_INSTANCIAS = 10

with open(RUTA_PUNTOS,'r') as f:
	puntos = json.loads(f.read())
	


for i in xrange(CANT_INSTANCIAS):
	instancia = []
	for v in puntos:
		instancia.append({'id':v['id'],'v':random.randint(0,100)})
	with open(RUTA_SALIDA.format(str(i+1)),'w') as f:
		f.write(json.dumps(instancia))