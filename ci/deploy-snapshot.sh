if [ "$TRAVIS_REPO_SLUG" == "intendia-oss/qualifier" ] && \
   [ "$TRAVIS_JDK_VERSION" == "oraclejdk8" ] && \
   [ "$TRAVIS_PULL_REQUEST" == "false" ] && \
   [ "$TRAVIS_BRANCH" == "master" ]; then

  mvn -s ci/settings.xml clean deploy -Dmaven.test.skip=true -Dinvoker.skip=true
fi
