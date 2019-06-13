# Builds a TeamCity build agent capable of compiling Android apps and running their corresponding unit tests.
FROM jetbrains/teamcity-agent

ARG ANDROID_HOME=/opt/android-sdk
ARG ANDROID_VERSION=28
ARG ANDROID_BUILD_TOOLS_VERSION=28.0.3
ARG SERVER_URL

ENV ANDROID_HOME=$ANDROID_HOME
ENV SERVER_URL=$SERVER_URL

RUN  apt-get update -y && \
     apt-get -y autoremove && \
     apt-get clean
     
RUN apt-get install -y zip \
    unzip

RUN mkdir /opt/android-sdk
WORKDIR /opt/android-sdk
COPY sdk-tools-linux-*.zip ./sdk.zip
RUN unzip -q sdk.zip
RUN rm sdk.zip
RUN yes | $ANDROID_HOME/tools/bin/sdkmanager --licenses
RUN $ANDROID_HOME/tools/bin/sdkmanager --update
RUN $ANDROID_HOME/tools/bin/sdkmanager "build-tools;${ANDROID_BUILD_TOOLS_VERSION}" "platforms;android-${ANDROID_VERSION}" "platform-tools"
