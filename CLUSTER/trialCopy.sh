#!/bin/bash

copyFiles() {
    simName1=$(echo $1 | tr "/" "_")
    simName2=$(echo $2 | tr "/" "_")

    echo "Copying macros from ${1} to ${2}"
    cp $1/*.java $2/.
    cp $1/*.sh $2/.
    echo "Copying meshFiles from ${1} to ${2}"
    cp $1/meshFiles.txt $2/.
    echo "Copying sim template from ${1} to ${2}"
    if [ "${3}" == "-mesh" ]
    then
        cp $1/LOG-01-MESH-$simName1.log $2/LOG-01-MESH-$simName2.log
        cp $1/$simName1.sim $2/$simName2.sim
        exit
    fi
    if [ -f $1/${simName1}_PreMesh.sim ]
        then
            cp $1/${simName1}_PreMesh.sim $2/${simName2}.sim
        else
            cp $1/$simName1.sim $2/$simName2.sim
        fi
}

multiPointCopying(){
    if [ $(find $PWD/$1 -maxdepth 1 -type d | wc -l) == 1 ]
    then
        copyFiles $1 $2 $3
    else
        filesToLoop=$(find $PWD/$1/* -maxdepth 1 -type d)
        for i in $filesToLoop
        do
            folderName=$(basename $i)
            mkdir $2/$folderName

            copyFiles $1/$folderName $2/$folderName
            echo ""
        done
    fi
}


if [ $(basename $PWD) != "RUN" ]
then
    echo "TrialCopy must be run from the RUN directory!"
    echo "TrialCopy exiting..."
    exit 0
fi

if [ ${#2} == 9 ]
then
    if [ -d $2 ]
    then
        echo "This trial already exists, trialCopy will now exit."
        exit 0
    else
        echo "Creating ${2} directory..."
        mkdir $2

        multiPointCopying $1 $2 $3
    fi
else
    trialPrefix=${2:0:7}
    trialStart=${2:8:2}
    trialEnd=${2:11:2}

    trialStart=$((10#$trialStart))
    trialEnd=$((10#$trialEnd))

    for i in $(seq $trialStart $trialEnd)
    do
        if [ ${#i} == 1 ]
        then
            trial="${trialPrefix}0${i}"
        else
            trial="${trialPrefix}${i}"
        fi
        
        if [ -d $trial ]
        then
            echo "This trial already exists, skipping trial..."
            continue
        else
            echo "Creating ${trial} directory..."
            mkdir $trial

            multiPointCopying $1 $trial $3
            echo ""
        fi

    done
fi
