#!/bin/bash

SERVER_ADDRESS="fs0.das4.cs.vu.nl"
IPL_ARGUMENTS="-Dibis.server.address=$SERVER_ADDRESS"
module load prun

for cores in {2..16}
    do
      prun -v -1 -np $cores bin/java-run $IPL_ARGUMENTS -Dibis.pool.name="tsok1${cores}" -Dibis.pool.size=${cores} rubiks.ipl.Rubiks >>ex1.out 2>>ex1.err
done
