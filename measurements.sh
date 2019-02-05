#!/bin/bash

SERVER_ADDRESS="fs0.das4.cs.vu.nl"
IPL_ARGUMENTS="-Dibis.server.address=$SERVER_ADDRESS"
module load prun

for cores in {1..16}
    do
      prun -v -1 -np $cores bin/java-run $IPL_ARGUMENTS -Dibis.pool.name="tsok1${cores}" -Dibis.pool.size=$cores rubiks.ipl.Rubiks >>t1_${cores}.out 2>>t1_${cores}.err
      prun -v -1 -np $cores bin/java-run $IPL_ARGUMENTS -Dibis.pool.name="tsok2${cores}" -Dibis.pool.size=$cores rubiks.ipl.Rubiks >>t2_${cores}.out 2>>t2_${cores}.err
      prun -v -1 -np $cores bin/java-run $IPL_ARGUMENTS -Dibis.pool.name="tsok3${cores}" -Dibis.pool.size=$cores rubiks.ipl.Rubiks >>t3_${cores}.out 2>>t3_${cores}.err
      prun -v -1 -np $cores bin/java-run $IPL_ARGUMENTS -Dibis.pool.name="tsok4${cores}" -Dibis.pool.size=$cores rubiks.ipl.Rubiks >>t4_${cores}.out 2>>t4_${cores}.err
done
