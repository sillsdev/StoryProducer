docker build -t teamcity-android-agent --build-arg SERVER_URL=http://127.0.0.1:8888 .
docker pull jetbrains/teamcity-server

docker run --name teamcity-server-instance  \
    -v /data/teamcity_server/datadir \
    -v /opt/teamcity/logs  \
    -p 8111:8111 \
    jetbrains/teamcity-server
    
chromeOSipAddr=`/sbin/ifconfig eth0 | grep -E -o 'inet\s([0-9\.]+)' | grep -Eo '[0-9\.]+'`
docker run --name teamcity-app -e SERVER_URL="http://$chromeOSipAddr:8111" -v /home/johnml1135/TeamCityConfig:/data/teamcity_agent/conf teamcity-android-agent