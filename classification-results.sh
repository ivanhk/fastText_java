#!/usr/bin/env bash
#
# Copyright (c) 2016-present, Facebook, Inc.
# All rights reserved.
#
# This source code is licensed under the BSD-style license found in the
# LICENSE file in the root directory of this source tree. An additional grant
# of patent rights can be found in the PATENTS file in the same directory.
#

# This script produces the results from Table 1 in the following paper: 
# Bag of Tricks for Efficient Text Classification, arXiv 1607.01759, 2016

myshuf() {
  perl -MList::Util=shuffle -e 'print shuffle(<>);' "$@";
}

normalize_text() {
  tr '[:upper:]' '[:lower:]' | sed -e 's/^/__label__/g' | \
    sed -e "s/'/ ' /g" -e 's/"//g' -e 's/\./ \. /g' -e 's/<br \/>/ /g' \
        -e 's/,/ , /g' -e 's/(/ ( /g' -e 's/)/ ) /g' -e 's/\!/ \! /g' \
        -e 's/\?/ \? /g' -e 's/\;/ /g' -e 's/\:/ /g' | tr -s " " | myshuf
}

DATASET=(
  ag_news
  sogou_news
  dbpedia
  yelp_review_polarity
  yelp_review_full
  yahoo_answers
  amazon_review_full
  amazon_review_polarity
)

ID=(
  0Bz8a_Dbh9QhbUDNpeUdjb0wxRms # ag_news
  0Bz8a_Dbh9QhbUkVqNEszd0pHaFE # sogou_news
  0Bz8a_Dbh9QhbQ2Vic1kxMmZZQ1k # dbpedia
  0Bz8a_Dbh9QhbNUpYQ2N3SGlFaDg # yelp_review_polarity
  0Bz8a_Dbh9QhbZlU4dXhHTFhZQU0 # yelp_review_full
  0Bz8a_Dbh9Qhbd2JNdDBsQUdocVU # yahoo_answers
  0Bz8a_Dbh9QhbZVhsUnRWRDhETzA # amazon_review_full
  0Bz8a_Dbh9QhbaW12WVVZS2drcnM # amazon_review_polarity
)

# These learning rates were chosen by validation on a subset of the training set.
LR=( 0.25 0.5 0.5 0.1 0.1 0.1 0.05 0.05 )

RESULTDIR=result
DATADIR=data

mkdir -p "${RESULTDIR}"
mkdir -p "${DATADIR}"

for i in {0..7}
do
  echo "Downloading dataset ${DATASET[i]}"
  if [ ! -f "${DATADIR}/${DATASET[i]}.train" ]
  then
    wget -c "https://googledrive.com/host/${ID[i]}" -O "${DATADIR}/${DATASET[i]}_csv.tar.gz"
    tar -xzvf "${DATADIR}/${DATASET[i]}_csv.tar.gz" -C "${DATADIR}"
    cat "${DATADIR}/${DATASET[i]}_csv/train.csv" | normalize_text > "${DATADIR}/${DATASET[i]}.train"
    cat "${DATADIR}/${DATASET[i]}_csv/test.csv" | normalize_text > "${DATADIR}/${DATASET[i]}.test"
  fi
done

mvn package

JAR=./target/fasttext-0.0.1-SNAPSHOT-jar-with-dependencies.jar

for i in {0..7}
do
  echo "Working on dataset ${DATASET[i]}"
  java -jar ${JAR} supervised -input "${DATADIR}/${DATASET[i]}.train" \
    -output "${RESULTDIR}/${DATASET[i]}" -dim 10 -lr "${LR[i]}" -wordNgrams 2 \
    -minCount 1 -bucket 10000000 -epoch 5 -thread 4 > /dev/null
  java -jar ${JAR} test "${RESULTDIR}/${DATASET[i]}.bin" \
    "${DATADIR}/${DATASET[i]}.test"
done
