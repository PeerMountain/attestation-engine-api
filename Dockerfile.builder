FROM openjdk:11

RUN apt-get update
RUN apt-get install npm -y
RUN apt-get install apt-transport-https
RUN sh -c 'wget -qO- https://dl-ssl.google.com/linux/linux_signing_key.pub | apt-key add -'
RUN sh -c 'wget -qO- https://storage.googleapis.com/download.dartlang.org/linux/debian/dart_stable.list > /etc/apt/sources.list.d/dart_stable.list'
RUN apt-get update
RUN apt-get install dart

ENV PATH="/usr/lib/dart/bin:${PATH}"

RUN dart pub global activate protoc_plugin
ENV PATH="/root/.pub-cache/bin/protoc-gen-dart:${PATH}"
