#!/bin/bash
echo "Compilation du serveur HTTP..."
mkdir -p bin
javac -d bin src/java/*.java
echo "Compilation terminee!"