import ansa
import os

from ansa import *

@session.defbutton("QFSAE_TOOLS","OutputSTL")
def OutputSTL():
	#Determine the file name from the database name as well as its path
	current_model = ansa.base.DataBaseName()
	model_path = current_model.split("/")
	file_base = model_path[-1].split('.ansa')[0]
	base_path = ''
	
	i = 0
	while (model_path[i] not in 'CAD') and (model_path[i] not in 'CFD'):
		base_path = base_path + model_path[i] + os.sep
		i += 1

	base_path = base_path + 'CFD' + os.sep + 'IMPORT' + os.sep

	base.OutputStereoLithography(base_path + file_base + '.stl', 'visible', 'ascii')

	print('STL output to: ' + base_path + file_base + '.stl')

if __name__ == '__main__':
	OutputSTL()
