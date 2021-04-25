#!/bin/bash 
cd amplisync-ws
rm -r target
mvn package war:exploded