import ansa
import os

from ansa import *

@session.defbutton("QFSAE_TOOLS","GenerateGBMR")
def GenerateGBMR():
	#Determine the file name from the database name as well as its path
	current_model = ansa.base.DataBaseName()
	model_path = current_model.split("/")
	file_base = model_path[-1].split('.')[0]
	base_path = ''
	
	i = 0
	while (model_path[i] not in 'CAD') and (model_path[i] not in 'CFD'):
		base_path = base_path + model_path[i] + os.sep
		i += 1

	base_path = base_path + 'CFD' + os.sep + 'IMPORT' + os.sep

	if os.path.exists('D:\Onedrive'):
		mesh.ReadMeshParams('D:\Onedrive\Documents\Queens\Formula SAE\Q22\Aero\CFD\MACROS\AUTOMESHER\setToTria.ansa_mpar')
	else:
		mesh.ReadMeshParams('E:\Onedrive\Documents\Queens\Formula SAE\Q22\Aero\CFD\MACROS\AUTOMESHER\setToTria.ansa_mpar')

	ents = base.CollectEntities(base.CurrentDeck(), None, search_types ="__PROPERTIES__" , filter_visible = True)
	
	mesh.Wrap(ents, 25.0 , 'out', 'smooth', 10, 'no', 10.0, -1, 'wrap1', 10)

	base.DeleteEntity(ents,True,True)

	base.OutputStereoLithography(base_path + file_base + '.stl', 'all', 'ascii')

	os.remove(current_model + '.log')

if __name__ == '__main__':
	GenerateGBMR	()
