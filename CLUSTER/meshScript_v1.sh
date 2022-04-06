#!/bin/bash

#SBATCH --job-name=QR-ms_STAR
#SBATCH --mem=0
#SBATCH -n 112
#SBATCH --partition=scb
#SBATCH -o %x.o
#SBATCH -e %x.e
#SBATCH --time=0-02:00:00
#SBATCH -x node[201-203,235]

#Start time for job to track overall progress
echo "`date`: Beginning job execution..."

#Macros used during the run process. Update only if the macros change
MESH=runMesh_v3.java

#Get the trial name from the current directory and determine sim file from it. Has a condition to determine if it's a multi-point run
runname=$(basename $PWD)
if [[ "$runname" != "trial"* ]]
then
runname="$(basename $(dirname $PWD))_$(basename $PWD)"
fi

sim_file="${runname}.sim"

#Parameters for defining wheel coordinates and radiators
cgx=0.50                #CG location along the x-axis, relative to front axle in percent
wheelbase=1.550         #Wheelbase in metres
tire_rad=16.2           #Tire diameter in inches
Front_Camber=0.0        #Degrees
Front_Toe=0.0           #Degrees, toe-out positive
FT=0.575                #Half Front Track [m]
RT=0.5625               #Half Rear Track [m]
Rear_Camber=0.0
Rear_Toe=0.0
FRH_Off=0.0             #Front Ride Height Offset [m]
RRH_Off=0.0             #Rear Ride Height Offset [m]
Roll=0.0                #Roll Angle, ignored for straight-line models
Yaw=0.0                 #Yaw Angle, ignored for straight-line models

COR=10                  #Corner radius [m], only for cornering
A_C=1.3                 #Lateral acceleration [G's], only for cornering
V=16.0                  #Straight Line speed for half-car [m/s], only for straight-line

rad_1_O="{1.01717, -0.44996, 0.248457}"
rad_1_X="{0.7071068, 0.0, 0.7071068}"
rad_1_Y="{0.0, 1.0, 0.0}"

rad_2_O="{1.01717, 0.44996, 0.248457}"
rad_2_X="{0.7071068, 0.0, 0.7071068}"
rad_2_Y="{0.0, 1.0, 0.0}"

#sed commands to update wheel parameters
sed -i "/cgx =/c\    public static double cgx = $cgx;" $MESH
sed -i "/wheelbase =/c\    public static double wheelbase = $wheelbase;" $MESH
sed -i "/tire_rad =/c\    public static double tire_rad = $tire_rad;" $MESH
sed -i "/Front_Camber =/c\    public static double Front_Camber = $Front_Camber;" $MESH
sed -i "/Front_Toe =/c\    public static double Front_Toe = $Front_Toe;" $MESH
sed -i "/FT =/c\    public static double FT = $FT;" $MESH
sed -i "/RT =/c\    public static double RT = $RT;" $MESH
sed -i "/Rear_Camber =/c\    public static double Rear_Camber = $Front_Camber;" $MESH
sed -i "/Rear_Toe =/c\    public static double Rear_Toe = $Rear_Toe;" $MESH
sed -i "/FRH_Off =/c\    public static double FRH_Off = $FRH_Off;" $MESH
sed -i "/RRH_Off =/c\    public static double RRH_Off = $RRH_Off;" $MESH
sed -i "/Roll =/c\    public static double Roll = $Roll;" $MESH
sed -i "/Yaw =/c\    public static double Yaw = $Yaw;" $MESH
sed -i "/COR =/c\    public static double COR = $COR;" $MESH
sed -i "/rad_1_O =/c\    public static double[] rad_1_O = $rad_1_O;" $MESH
sed -i "/rad_1_X =/c\    public static double[] rad_1_X = $rad_1_X;" $MESH
sed -i "/rad_1_Y =/c\    public static double[] rad_1_Y = $rad_1_Y;" $MESH
sed -i "/rad_2_O =/c\    public static double[] rad_2_O = $rad_2_O;" $MESH
sed -i "/rad_2_X =/c\    public static double[] rad_2_X = $rad_2_X;" $MESH
sed -i "/rad_2_Y =/c\    public static double[] rad_2_Y = $rad_2_Y;" $MESH

echo "----------------------------------------------------"
echo "This job is allocated to run on $SLURM_NTASKS cpu(s)"
echo "Job is running on node(s): "
echo "$SLURM_JOB_NODELIST"
echo "----------------------------------------------------"

#process for submitting mesh operations
echo "Beginning mesh operations..."
MESHSTART=`date +%s`
$starccm+ -rsh ssh -batchsystem slurm $sim_file -doepower -np $SLURM_NTASKS -batch $MESH
MESHEND=`date +%s`
echo "Mesh operations finished"
echo "Time elapsed: $(($MESHEND - $MESHSTART)) seconds to run"
echo ""

rm -f "${runname}.sim~"

echo "`date`: Job has finished running"

