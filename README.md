# HEXOIDS

A simple distributed arcade game

# Requirements

Please have Docker with docker-compose setup on your machine.

# Development

## Start Infrastructure

To start infrastructure services run:

    docker-compose -f docker-compose-dev-infrastructure.yml up -d

## Start

To start the service run:

    mvn compile quarkus:dev
    
and open in a WebGL capable browser:

    http://localhost:8080
    
# Production

## Setup ENV

Copy `.env.sample` to `.env` and replace the value
`HEXOIDS_HOST` with your accessible host

## Run

    docker-compose up -d
    
## Increase nodes

    docker-compose scale hexoids=#