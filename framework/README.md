# Intueri-Framework

##### Dependencies

For running and building the framework:
1. [docker](https://www.docker.com/)
2. [docker-compose](https://docs.docker.com/compose/)

For using/testing the framework a [Postman](https://www.getpostman.com/) collection with some default values is provided. The README assumes this collection is used, but all HTTP requests can also be send via any other software.

##### Building the software

The Intueri software is provided as a set of docker images. To build the images run the following commands:

1. Build Intueri Framework base image via docker `docker build -t intueri/framework:1.0.0 .`
2. Build the individual images via `docker-compose build`

Currently the version number is hardcoded into the individual docker files in the `docker` subdirectory. 

##### Demo

1. A `docker-compose.yml` is provided in the root directory that starts a minimal set of containers and default configurations to test the application. The containers can be started by running via `docker-compose up`.
2. A Postman collection with the common commands used to interact with the system is provided (`Intueri-Framework.postman_collection.json`). The typical workflow after initially starting the system is outlined below. For simplicity it is assumed that the default ids are used. If this is not the case the ids inside the individual requests need to be changed accordingly:
	1. Check available detector instances and their current state via `GET detectors`. If detector is in state `WAITING_FOR_CONFIG` proceed to ii. Else repeat (The metadata is only refreshed every 5minutes).
	2. Upload configuration via `POST config`
	3. Assign configuration to detector via `PUT config to detector`.
	2. Upload rule via `POST rule`
	3. Assign rule to detector via `PUT enabledRules to detector`.
	4. Bootstrap detector via `PUT command init to detector`
	4. Start detector via `PUT command start to detector`
3. Check the intueri-ouput containers log. The `docker-compose.yml` initiated a script that enters random data into the database every 10 seconds. As the default rule contains no filters this data should propagate to the output and be logged to the console. As the output only fetches new topics every 5 minutes there might be an initial delay of up to 5 minutes.
