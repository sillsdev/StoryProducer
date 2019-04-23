## Intro
The Dockerfile in this folder can be used to create a docker image containing the software necessary to act as a build agent for TeamCity and to run Android unit tests.

## Usage
1. First, download the Android Sdk for Linux (make sure you get the "Command-line tools only" version).
2. Take the `sdk-tools-xxxxx.zip` file that you just downloaded and move it into the directory containing the Dockerfile (Leave the zip file as-is; do not unzip it.).
3. Run the following command (you may need sudo):

    `docker build -t teamcity-android-agent --build-arg SERVER_URL=http://<ip address>:<port> .`

Now that the image has been built, you can use the image to create a docker container (you may need sudo):

`docker run -it -v <path to agent config folder>:/data/teamcity_agent/conf teamcity-android-agent`

(The agent config folder is just a folder on the host computer. It doesn't need any special files in it to begin with, but teamcity will store files there so that you can preserve them if you ever delete/recreate the build agent container.)

If the IP address of the teamcity server has changed from what it was when you built the image, use the -e option with the previous command to specify the new URL (you may need sudo):

`docker run -it -e SERVER_URL="http://<ip address>:<port>" -v <path to agent config folder>:/data/teamcity_agent/conf teamcity-android-agent`
