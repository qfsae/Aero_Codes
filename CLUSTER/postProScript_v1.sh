#!/bin/bash

#SBATCH --job-name=QR-pp_STAR
#SBATCH --mem=0
#SBATCH -n 112
#SBATCH --partition=scb
#SBATCH -o %x.o
#SBATCH -e %x.e
#SBATCH --time=0-10:00:00
#SBATCH -x node[201-203,235]

#Start time for job to track overall progress
echo "`date`: Beginning job execution..."

#Macros used during the run process. Update only if the macros change
POST=PostPro_v3.java

#Get the trial name from the current directory and determine sim file from it. Has a condition to determine if it's a multi-point run
runname=$(basename $PWD)
if [[ "$runname" != "trial"* ]]
then
runname="$(basename $(dirname $PWD))_$(basename $PWD)"
fi

sim_file="${runname}.sim"

#Parameters for post pro (to do or not to do deltas)
doDelta=0
baseline="\"trial0001\""
exportGBMR=1

#sed commands to update post pro parameters
sed -i "/doDelta = /c\    int doDelta = $doDelta;" $POST
sed -i "/baseline = /c\    String baseline = $baseline;" $POST
sed -i "/exportGBMR = /c\    int exportGBMR = $exportGBMR;" $POST

echo "----------------------------------------------------"
echo "This job is allocated to run on $SLURM_NTASKS cpu(s)"
echo "Job is running on node(s): "
echo "$SLURM_JOB_NODELIST"
echo "----------------------------------------------------"

#process for submitting post processing operations
echo "Beginning post-processing..."
POSTSTART=`date +%s`
$starccm+ -rsh ssh -batchsystem slurm $sim_file -doepower -np $SLURM_NTASKS -rr -rrthreads 8 -batch $POST
POSTEND=`date +%s`
echo "Post processing finished"
echo "Time elapsed: $(($POSTEND - $POSTSTART)) seconds to run"
echo ""

rm -f "${runname}.sim~"

echo "`date`: Job has finished running"

