import ansa
import os

from ansa import *

@session.defbutton("QFSAE_TOOLS","QFSAEAutoMesh")
def QFSAEAutoMesh():
	#Determine the file name from the database name as well as its path
	current_model = ansa.base.DataBaseName()

	cad_path = ''
	
	if os.path.exists('D:\Onedrive'):
		cad_path = 'D:\Onedrive\Documents\Queens\Formula SAE\Q22\Aero\CFD\MACROS\AUTOMESHER\QFSAE_AutoSurfaceMesh.ansa'
	else:
		cad_path = 'E:\Onedrive\Documents\Queens\Formula SAE\Q22\Aero\CFD\MACROS\AUTOMESHER\QFSAE_AutoSurfaceMesh.ansa'
	
	#Merge in the batch meshing scenario
	utils.Merge(filename= cad_path, property_offset= 'keep-old', merge_parts= True)
	
	#Show all entities in the model
	base.All()

	#First ensure that all faces are oriented outwards
	base.AutoCalculateOrientation("Visible", True)
	print("All faces oriented outwards\n")

	#Hide unmeshed macros and freeze the meshed macros
	mesh.UnmeshedMacros()
	base.Invert()
	base.FreezeVisibleFaces()
	base.Invert()

	#Run all the batchmeshing scenarios to generate the auto-mesh, for up to a maximum of 100 minutes on all unmeshed macros
	print("Running batch mesh scenarios, please wait...\n")
	batchmesh.DistributeAllItemsToScenarios()
	batchmesh.RunAllMeshingScenarios(100)

	#Show all faces and freeze the meshed faces
	base.All()
	base.FreezeVisibleFaces()

	ents = base.GetPartFromName("DeleteMe", "ANSAPART")

	base.DeleteEntity(ents,True,True)

	os.remove(current_model + '.log')

if __name__ == '__main__':
	QFSAEAutoMesh()
