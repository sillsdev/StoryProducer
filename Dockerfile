# Get our base, which has Android Studio tools installed
FROM androidsdk/android-31

# Get our system-images
RUN /opt/android-sdk-linux/cmdline-tools/tools/bin/sdkmanager --install "system-images;android-26;google_apis;x86"
RUN /opt/android-sdk-linux/cmdline-tools/tools/bin/sdkmanager --install "system-images;android-26;google_apis_playstore;x86"
RUN /opt/android-sdk-linux/cmdline-tools/tools/bin/sdkmanager --install "system-images;android-27;google_apis;x86"
RUN /opt/android-sdk-linux/cmdline-tools/tools/bin/sdkmanager --install "system-images;android-27;google_apis_playstore;x86"
RUN /opt/android-sdk-linux/cmdline-tools/tools/bin/sdkmanager --install "system-images;android-28;google_apis;x86"
RUN /opt/android-sdk-linux/cmdline-tools/tools/bin/sdkmanager --install "system-images;android-28;google_apis_playstore;x86"
RUN /opt/android-sdk-linux/cmdline-tools/tools/bin/sdkmanager --install "system-images;android-29;google_apis;x86"
RUN /opt/android-sdk-linux/cmdline-tools/tools/bin/sdkmanager --install "system-images;android-29;google_apis_playstore;x86"
RUN /opt/android-sdk-linux/cmdline-tools/tools/bin/sdkmanager --install "system-images;android-30;google_apis;x86"
RUN /opt/android-sdk-linux/cmdline-tools/tools/bin/sdkmanager --install "system-images;android-30;google_apis_playstore;x86"

# Get some libraries that are not in Ubuntu 20.04, but are needed by some of the Android tools
RUN apt install -y pulseaudio && apt install -y libxcursor-dev

# Set our working directory:
WORKDIR /storyproducer

# Copy the Story Producer files to working directory in the Docker image
COPY . .

