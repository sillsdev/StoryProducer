docker start teamcity-server-instance

docker start teamcity-app

#print out the address to go to for team city
echo "Go to Team City:"
echo http://`/sbin/ifconfig eth0 | grep -E -o 'inet\s([0-9\.]+)' | grep -Eo '[0-9\.]+'`:8111