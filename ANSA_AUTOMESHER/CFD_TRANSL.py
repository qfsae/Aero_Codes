#This CFD_TRANSL.py will be read automatically if ANSA starts with the CFD
#layout. All the functions will be placed under the UDFs buttons.
#If one wants to add/remove functions from this file, he can place it in his
#home directory and more specifically: /home/user/.BETA/ANSA/version_15.0.0/'CFD_TRANSL.py

import ansa
import sys
import os 

#CHANGE THIS PATH TO BE WHEREVER YOU PUT YOUR SCRIPTS!!!
sys.path.append('pathToYourMacros')
##QFSAE_TOOLS
import QFSAEAutoMesh
import GenerateGBMR
import OutputSTL