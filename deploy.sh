#!/usr/bin/env bash
# mvn package # this is not needed since heroku:deploy runs this

# More info on using aliases in scripts (noninteractive shells):
# https://www.thegeekdiary.com/how-to-make-alias-command-work-in-bash-script-or-bashrc-file/
# https://unix.stackexchange.com/questions/288512/how-can-i-test-if-a-particular-alias-is-defined
# https://askubuntu.com/questions/350208/what-does-2-dev-null-mean
# https://stackoverflow.com/questions/638975/how-do-i-tell-if-a-regular-file-does-not-exist-in-bash
shopt -s expand_aliases

# Load ~/.profile if it exists (to get the aliases),
if [ -f ~/.profile ];
  then
    echo "Loading ~/.profile to get aliases"
    source ~/.profile
fi

if alias jdk8 2>/dev/null; then
  echo "Using JDK8";
  jdk8;
else
  echo "Make sure to use JDK8 to run this script";
fi

mvn heroku:deploy

# DEBUG
# mvn heroku:deploy -X -e
