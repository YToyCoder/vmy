#!/bin/bash

if [ $VMY_HOME ]; then
  java -jar $VMY_HOME/lib/vmy.jar -sh 
else 
  echo VMY_HOME not exists, you should add it to your envirment!
fi