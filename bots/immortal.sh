#!/bin/bash

while true; do
	node index.js &
		
	last_pid=$!

	sleep 900

	kill $last_pid 2> /dev/null
done
