#!/bin/sh

export VITAL_HOME=/home/centos/vital-install

appHome=/home/centos/vital-agent-rest

cd $appHome

java -jar target/vital-agent-rest-0.1.0-fat.jar config/vital-agent-rest.config > nohup.out 2>&1

