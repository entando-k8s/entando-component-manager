#!/bin/bash
java -XX:MaxRAMPercentage=${MAX_RAM_PERCENTAGE:-20}.0 -XshowSettings:vm -jar app.jar