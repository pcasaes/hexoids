# HEXOIDS

A simple distributed arcade game.

![Screenshot](./docs/hexoids-screenshot1.png "Screenshot")


- [Requirements](#requirements)
- [Development](#development)
    - [Infrastructure](#infrastructure)
    - [Start Quarkus](#start-quarkus)
- [Production](#production)
    - [Setup Env](#setup-env)
    - [Cluster Commands](#cluster-commands)
- [Architecture](#architecture)
- [Future Improvements](#future-improvements)
    - [Techinical](#technical)
    - [Game Play](#game-play)

# Requirements

* [Java 8](https://openjdk.java.net/install/)
    * [Maven](https://maven.apache.org/)
* [Docker](https://www.docker.com/)
    * [docker-compose](https://docs.docker.com/compose/)
* WebGL capable Browser

# Development

## Infrastructure

Hexoids uses [Apache Kafka](https://kafka.apache.org/) as its backend

To start infrastructure services run:

    docker-compose -f docker-compose-dev-infrastructure.yml up -d
    
If you'd like to reset the backend run:

    docker-compose -f docker-compose-dev-infrastructure.yml rm -svf


## Start Quarkus

Hexoids is built using [Quarkus](https://quarkus.io/)

To start the service run:

    mvn compile quarkus:dev
    
and open in a WebGL capable browser:

    http://localhost:8080
    
If you wish to use a different port run:

    mvn compile quarkus:dev -Dquarkus.http.port=8180
    
# Production

The file `docker-compose.yml` is a guide to how we could setup Hexoids for production.
This particular setup runs everything in a single Host which is not something you'd
do for real. Regardless this allows us to test the service in a distributed fashion.

> **IMPORTANT**: If running on multiple host their clocks must be synchronized.

## Setup ENV

Copy `.env.sample` to `.env` and replace the value
`HEXOIDS_HOST` with your accessible host hostname or ip. The server will be
accessible on the hostname/ip on port 80: `http://HEXOIDS_HOST`.

## Cluster commands

| Action | Command |
| --- | --- |
| Start | `docker-compose up -d` |
| Stop | `docker-compose stop` |
| Log | `docker-compose logs -f hexoids` |
| Scale | `docker-compose scale hexoids=2` |
| Delete | `docker-compose rm -svf` |

# Architecture

Hexoids is server authoritative and is built around an event driven model.

# Future Improvements

## Technical

* Lag compensation on the client side.

    Right now there is no lag compensation on the client.
    Users behind high latency connections would have a poor experience.
    
* Move to more robust client side framework like [Godot](https://godotengine.org/).

    [Phaser](https://phaser.io/) is a great way to get started and prototyping
    quickly. But from a scalability stand point there's only so far ou can go
    running the client on a single threaded browser engine.

* Replace Websockets with a combination of TCP an UDP sockets.

   TCP connections are expensive, Websockets even more so. High frequency events 
   like player and bolt movements would be better served over a UDP socket. 
   
* Replace the custom inter thread event queue with the [LMAX Disruptor](https://github.com/LMAX-Exchange/disruptor)

    Rolling your own inter thread event queue is an interesting exercise in
    writing concurrent code, but if we truly wish to reduce latency and
    increase throughput then integrating with the disruptor is the way to go.
    
* Add persistence

    Right now nothing gets persisted long term. It would be interesting to
    certain player information like K/D and Hit miss ratio's. As well as a permanent
    leader board. For this to work probably though we would need authentication.

## Game Play

* Add obstacles

    Without any kind of obstacles the game quickly becomes a frenetic death match
    with bolts flying all over the place. Having obstacles would allow players to
    employ diverse tactics. This could be done procedurally. It would event be 
    interesting to allow playing to deploy obstacles in a limited fashion.
   
* Powerups

    Having players compete to acquire powerups would add variety to the game:
    teleport, cloak, bombs, shields, faster bolts, more bolts, etc.
    
* Different ships
    
    Give players the option to choose different ships with different properties:
    More bolts, faster top speed, faster acceleration, etc.
    
* Teams and different game modes

    With teams there could be different game modes like capture the flag and
    eliminations.