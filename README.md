# flamalgam [![Build Status](https://travis-ci.org/dkoval/flamalgam.svg?branch=master)](https://travis-ci.org/dkoval/flamalgam)

Apache Flink-powered Implementation of relational JOIN operator on infinite data streams.

## Introduction

Join is a fundamental operation in SQL. However, it's far from trivial to model and execute this type of operation in 
a streaming environment. The problem becomes even more difficult to tackle if you need to join infinite data streams. 
That is, where a concept of a timed window is no longer applicable.

The project goal is to mimic [relational](https://en.wikipedia.org/wiki/Relational_algebra) `JOIN` operator 
on infinite data streams. The underlying implementation makes use of [Apache Flink](https://flink.apache.org/) 
as a stream processing technology and [Kotlin](https://kotlinlang.org/) as a programming language of choice.

## What is it for?

To make the description less abstract, let's consider a practical example. 

Imagine you already [CDC](https://en.wikipedia.org/wiki/Change_data_capture) -ed two logically related tables `A` and `B` 
from your RDBS of choice and now want to create a higher-order view `C` out of these streams of data. You may then want 
to store this new view of original data in another specialized database, say [Elasticsearch](https://www.elastic.co/), 
to optimize the read layer of the system you are building. But first things first - you somehow need to `join` infinite 
streams `A` and `B` in order to produce `C` and and these are types of problems this project deals with.

... TODO: put a diagram here ...

Without any further ado, let's get started.

## Quickstart

... to be continued ...

## Licence

The project is distributed under the terms of the [Apache 2.0 License](LICENSE.txt).
