# Bots

This simple [NodeJS](https://nodejs.org/en/) application allows us to simulate players with a not so smart ai (random).

# Setup

You'll need to install [nvm](https://github.com/nvm-sh/nvm) and run:

    nvm i
    
It should set up node. Then run:

    npm i
    
Copy the file `settings.local.sample.js` to `settings.local.js`.
Edit the file to make sure the server's endpoint is correct as well as
the number of desired bots.

# Run

To run:

    node index.js
    
To stop press `Ctrl-C`