import os
import argparse
import re

meshPrefix = 'QR-ms'
solvePrefix = 'QR-so'
postProPrefix = 'QR-pp'

def modifyScript(scriptPath, trialNumber, prefix, cores):
	f = open(scriptPath, 'r')
	tempfile = [line.rstrip() for line in f.readlines()]
	for i in range(len(tempfile)):
		if re.match(".*#SBATCH --job-name.*",tempfile[i]):
			tempfile[i] = "#SBATCH --job-name=" + prefix + "_" + trialNumber

		if re.match(".*#SBATCH -n.*",tempfile[i]):
			tempfile[i] = "#SBATCH -n " + str(int(cores))

	f.close()

	fOut = open(scriptPath, 'w')
	for line in tempfile:
		fOut.write(line + '\n')

	fOut.close
	
def updateScripts(trial, cores, meshCores):
	# Routine for updating batch scripts core numbers, as well as job names

	doSep = ''

	if trial != '':
		doSep = '/'

	cwd = os.getcwd() + "/" + trial + doSep
	trialNumber = ''

	if trial == '':
		trialNumber = os.path.basename(cwd[:-1])[-4:]
	else:
		trialNumber = trial[-4:]

	batchScripts = []

	for (dirpath, dirnames, filenames) in os.walk(cwd):
		for file in filenames:
			if '.sh' in file:
				batchScripts.append(file)

	for script in batchScripts:
		numCores = cores
		prefix = ''
		if 'mesh' in script:
			prefix = meshPrefix
			numCores = meshCores
		elif 'solve' in script:
			prefix = solvePrefix
		elif 'postPro' in script:
			prefix = postProPrefix
		
		scriptPath = cwd + script
		modifyScript(scriptPath, trialNumber, prefix, numCores)

def updatePostPro(trial, doGBMR, baseline):
	# Routine for updating post processing routines requests

	doSep = ''

	if trial != '':
		doSep = '/'

	cwd = os.getcwd() + "/" + trial + doSep
	
	for (dirpath, dirnames, filenames) in os.walk(cwd):
		for file in filenames:
			if re.match(".*postPro.*sh", file):
				cwd += file

	f = open(cwd, 'r')
	tempfile = [line.rstrip() for line in f.readlines()]
	for i in range(len(tempfile)):
		if re.match(".*exportGBMR=.*",tempfile[i]):
			if doGBMR == True:
				tempfile[i] = "exportGBMR=1"
			else:
				tempfile[i] = "exportGBMR=0"

		if re.match(".*baseline=\"",tempfile[i]):
			if baseline != '':
				tempfile[i] = "baseline=\"\\\"" + baseline + "\\\"\""
		
		if re.match(".*doDelta=",tempfile[i]):
			if baseline != '':
				tempfile[i] = "doDelta=1"
			else:
				tempfile[i] = "doDelta=0"

	f.close()

	fOut = open(cwd, 'w')
	for line in tempfile:
		fOut.write(line + '\n')

	fOut.close

def main(trials, doGBMR, baseline, cores, meshCores):
	for trial in trials:
		if trial == '':
			print("Running QRScriptFixer for current trial...\n========================================\n")
		else:
			print("Running QRScriptFixer for " + trial + "...\n============================================\n")

		# Update batch scripts
		print("Updating core counts and job names in batch scripts...")
		updateScripts(trial, cores, meshCores)
		print("Batch scripts updated.\n")

		# Update post pro requests
		print("Updating GBMR exporting and delta baseline...")
		updatePostPro(trial, doGBMR, baseline)
		print("Post processing script updated.\n")

		print("scriptFixer complete.\n")

if __name__ == '__main__':
	parser = argparse.ArgumentParser(description='Update run scripts with set values. Meant to be run in the trial directory, or in the RUN directory with trials specified')

	parser.add_argument('-t', action='store', dest='trials', type=str, nargs='+', default=[''])
	parser.add_argument('-exportGBMR', action='store_true', dest='doGBMR')
	parser.add_argument('-delta', action='store', type=str, dest='baseline', default='')
	parser.add_argument('-c', action='store', type=int, dest='cores', default=112)
	parser.add_argument('-cm', action='store', type=int, dest='meshCores', default=112)
    
	args = parser.parse_args()

	main(args.trials, args.doGBMR, args.baseline, args.cores, args.meshCores)