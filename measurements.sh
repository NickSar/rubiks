#!/bin/bash

SERVER_ADDRESS="fs0.das4.cs.vu.nl"
IPL_ARGUMENTS="-Dibis.server.address=$SERVER_ADDRESS"
module load prun

for proc in 2
  do
    for cores in 1
    do
      prun -v -$proc -np $cores bin/java-run $IPL_ARGUMENTS -Dibis.pool.name="tsok1${proc}${cores}" -Dibis.pool.size=$((proc * cores)) rubiks.ipl.Rubiks >>t1_${proc}_${cores}.out 2>>t1_${proc}_${cores}.err
      prun -v -$proc -np $cores bin/java-run $IPL_ARGUMENTS -Dibis.pool.name="tsok2${proc}${cores}" -Dibis.pool.size=$((proc * cores)) rubiks.ipl.Rubiks >>t2_${proc}_${cores}.out 2>>t2_${proc}_${cores}.err
      # prun -v -$proc -np $cores bin/java-run $IPL_ARGUMENTS -Dibis.pool.name="tsok3${proc}${cores}" -Dibis.pool.size=$((proc * cores)) sokoban.ipl.Sokoban "tests/t3.txt" >>t3_${proc}_${cores}.out 2>>t3_${proc}_${cores}.err
      # prun -v -$proc -np $cores bin/java-run $IPL_ARGUMENTS -Dibis.pool.name="tsok4${proc}${cores}" -Dibis.pool.size=$((proc * cores)) sokoban.ipl.Sokoban "tests/t4.txt" >>t4_${proc}_${cores}.out 2>>t4_${proc}_${cores}.err
  done;
done
