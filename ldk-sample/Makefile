SHELL := bash
.ONESHELL:
.SHELLFLAGS := -eu -o pipefail -c
.DELETE_ON_ERROR:
MAKEFLAGS += --warn-undefined-variables
MAKEFLAGS += --no-builtin-rules

ifeq ($(origin .RECIPEPREFIX), undefined)
  $(error This Make does not support .RECIPEPREFIX. Please use GNU Make 4.0 or later)
endif
.RECIPEPREFIX = >

noop=
space = $(noop) $(noop)

MAVEN_HOME = /opt/maven
JAVA_HOME = /opt/java-11

BITCOIN_BIN = /opt/bitcoin/src
#BITCOIN_BIN = /opt/bitcoin-22.0/bin

mvn += JAVA_HOME=$(JAVA_HOME)
mvn += $(MAVEN_HOME)/bin/mvn

java += LD_PRELOAD=/usr/lib64/clang/13/lib/libclang_rt.asan-x86_64.so
java += ASAN_OPTIONS=detect_leaks=0
java += /opt/java-11/bin/java
java += -Djava.library.path=/opt/ldk-garbagecollected

bitcoind = $(BITCOIN_BIN)/bitcoind
bitcoin_cli = $(BITCOIN_BIN)/bitcoin-cli

# Building bitcoin
# ./autogen.sh
# ./configure --with-incompatible-bdb
# ./make -j 16

ldk_jars += $(shell find $$(pwd)/target/lib -type f -name '*.jar')
ldk_jars += $$(pwd)/target/ldk-sample.jar
classpath += $(subst $(space),:,$(ldk_jars))

run:
> LDK_SEED=c1da666fbc06e6a6c39457e90385f65a1c53580c92cab555952e7c909731c8f8 \
> LDK_PEER_PUBKEY=02d9cd0d55d951832149dbeb237383f7869cfe6ffe908bc151b5b9e6747601ad25 \
> LDK_PEER_HOST=127.0.0.1 \
> LDK_PEER_PORT=9901 \
> BTC_CORE_URL=http://127.0.0.1:18443 \
> BTC_CORE_USER=user \
> BTC_CORE_PASS=pass \
> $(java) -classpath $(classpath) com.getlipa.ldk.sample.LDKSample
.PHONY: run

build:
> $(mvn) install dependency:copy-dependencies -DoutputDirectory=target/lib
.PHONY: build

clean:
> $(mvn) clean
.PHONY: clean

get-ldk-jars:
> scp -r $(REMOTE):.m2/repository/org/lightningdevkit $(HOME)/.m2/repository/org
.PHONY: get-ldk-jars

daemon:
> $(bitcoind) -regtest -daemon
.PHONY: daemon

stop:
> $(bitcoin_cli) -regtest stop
.PHONY: stop

create-wallet:
> $(bitcoin_cli) -regtest createwallet "testwallet"
.PHONY: create-wallet

load-wallet:
> $(bitcoin_cli) -regtest loadwallet "testwallet"
.PHONY: load-wallet

blocks:
> $(bitcoin_cli) -regtest generatetoaddress 101 $$($(bitcoin_cli) -regtest getnewaddress)
.PHONY: blocks
