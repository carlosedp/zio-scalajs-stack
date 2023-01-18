FROM ubuntu:18.04

RUN apt-get update -q -y && \
    apt-get install -q -y build-essential libz-dev locales --no-install-recommends

RUN locale-gen en_US.UTF-8

ENV LANG en_US.UTF-8
ENV LANGUAGE en_US:en
ENV LC_ALL en_US.UTF-8