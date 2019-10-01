#!/usr/bin/env bash
#
# Copyright (c) 2016-present, Facebook, Inc.
# All rights reserved.
#
# This source code is licensed under the BSD-style license found in the
# LICENSE file in the root directory of this source tree. An additional grant
# of patent rights can be found in the PATENTS file in the same directory.
#

RESULTDIR=result
DATADIR=data

mkdir -p "${RESULTDIR}"
mkdir -p "${DATADIR}"

if [ ! -f "${DATADIR}/enwik9.zip" ]
then
  wget -c http://mattmahoney.net/dc/enwik9.zip -P "${DATADIR}"
fi


if [ ! -f "${DATADIR}/text9" ]
then
  unzip "${DATADIR}/enwik9.zip" -d "${DATADIR}"
  perl wikifil.pl "${DATADIR}/enwik9" > "${DATADIR}"/text9
fi



if [ ! -f "${DATADIR}/rw/rw.txt" ]
then
  wget -c http://www-nlp.stanford.edu/~lmthang/morphoNLM/rw.zip -P "${DATADIR}"
  unzip "${DATADIR}/rw.zip" -d "${DATADIR}"
fi

mvn package

JAR=./target/fasttext-0.0.1-SNAPSHOT-jar-with-dependencies.jar

java -Xmx8G -jar ${JAR} skipgram -input "${DATADIR}"/text9 -output "${RESULTDIR}"/text9 -lr 0.025 -dim 100 \
  -ws 5 -epoch 1 -minCount 5 -neg 5 -loss ns -bucket 200000 \
  -minn 3 -maxn 6 -thread 4 -t 1e-4 -lrUpdateRate 100

cut -f 1,2 "${DATADIR}"/rw/rw.txt | awk '{print tolower($0)}' | tr '\t' '\n' > "${DATADIR}"/queries.txt

cat "${DATADIR}"/queries.txt | ./fasttext print-vectors "${RESULTDIR}"/text9.bin > "${RESULTDIR}"/vectors.txt

python eval.py -m "${RESULTDIR}"/vectors.txt -d "${DATADIR}"/rw/rw.txt
