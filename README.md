# ClusteringStreamData-Kafka

Reimplementation of the paper "Clustering Stream Data by Exploring the Evolution of Density Mountain" in the project seminar Mining Streaming Data at HPI

## Prerequisites 

- [gradle](https://gradle.org)
- [docker and docker-compose](https://www.docker.com/get-started)
- [node and npm](https://www.npmjs.com/get-npm)

## Getting Started

1) Clone the repository via `git clone ...`
2) Build the application docker images by running `gradle dockerBuildImage` inside the folder
3) Start the docker deamon and run `docker-compose up` to start the kafka cluster locally
4) Connect to `localhost:5656` with the browser of your choice
