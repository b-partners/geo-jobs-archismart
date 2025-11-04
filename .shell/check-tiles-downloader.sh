#!/bin/bash

URL=$1

ZOOM_SIZE=$2
SERVER=$3
GEO_JSON=$4

curl --fail -X POST $URL?zoom_size=$ZOOM_SIZE -F server=@$SERVER -F geojson=@$GEO_JSON > /dev/null 2>&1 -w "%{http_code}"