# flamalgam

## Problem

Join is a fundamental operation in SQL. However, it's far from trivial to model and execute this type of operation in 
a streaming environment. The problem becomes even more difficult to tackle if you need to join infinite data streams. 
That is, where a concept of a timed window is no longer applicable.

`Flamalgam` project aims to implement SQL join semantics on infinite data streams leveraging [Apache Flink](https://flink.apache.org/) 
as a stream processing technology and [Kotlin](https://kotlinlang.org/) as a programming language of choice.

## What is it for?

To make the description less abstract, let's consider a practical example. 

Imagine you already [CDC](https://en.wikipedia.org/wiki/Change_data_capture) -ed two logically related tables `A` and `B` 
from your RDBS of choice and now want to create a higher-order view `C` out of these streams of data. You may then want 
to store this new view of original data in another specialized database, say [Elasticsearch](https://www.elastic.co/), 
to optimize the read layer of the system you are building. But first things first - you somehow need to `join` infinite 
streams `A` and `B` in order to produce `C` and and this is exactly the type of problem this project deals with.

... Put a diagram here ...

Without further ado let's get right to it.

## Quickstart

... to be continued ...

## Licence

The project is distributed under the terms of the [Apache 2.0 License](LICENSE).
