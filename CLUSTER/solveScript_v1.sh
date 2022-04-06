#!/bin/bash

#SBATCH --job-name=QR-so_STAR
#SBATCH --mem=0
#SBATCH -n 112
#SBATCH --partition=scb
#SBATCH -o %x.o
#SBATCH -e %x.e
#SBATCH --time=1-00:00:00
#SBATCH -x node[201-203,235]

#Start time for job to track overall progress
echo "`date`: Beginning job execution..."

#Macros used during the run process. Update only if the macros change
RUN=runSim_v2.java

#Get the trial name from the current directory and determine sim file from it. Has a condition to determine if it's a multi-point run
runname=$(basename $PWD)
if [[ "$runname" != "trial"* ]]
then
runname="$(basename $(dirname $PWD))_$(basename $PWD)"
fi

sim_file="${runname}.sim"

#Parameters for defining freestream velocity
A_C=1.3                 #Lateral acceleration [G's], only for cornering
V=16.0                  #Straight Line speed for half-car [m/s], only for straight-line

#sed commands to update wheel parameters
sed -i "/A_C =/c\    public static double A_C = $A_C;" $RUN
sed -i "/V =/c\    public static double V = $V;" $RUN

echo "----------------------------------------------------"
echo "This job is allocated to run on $SLURM_NTASKS cpu(s)"
echo "Job is running on node(s): "
echo "$SLURM_JOB_NODELIST"
echo "----------------------------------------------------"

#process for submitting run operations
echo "Beginning steady run..."
RUNSTART=`date +%s`
$starccm+ -rsh ssh -batchsystem slurm $sim_file -doepower -np $SLURM_NTASKS -batch $RUN
RUNEND=`date +%s`
echo "Steady run finished"
echo "Time elapsed: $(($RUNEND - $RUNSTART)) seconds to run"
echo ""

rm -f "${runname}.sim~"

echo "`date`: Job has finished running"

