#!/bin/sh
mvn release:prepare release:perform --batch-mode && git push
