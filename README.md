# Nudge

Disclaimer: Not much works yet, have a look in the tests for what does.
No on-disk storage at this point.

Nudge is an experimental document database built with the following goals in mind:

## Document storage in edn format

Allow you to store documents containing clojure datastructures, including your own record types. Basicly, anything that can be serialized.

## Non-redundant storage of datastructures

Each document is a map with an id, the keys and values of the map are hashed and stored as references, allowing documents to efficiently share substructures, and even contain other documents.

## Historic queries on the data

Never throw away old versions of documents. When you write a document with the same id as another, you simply push the new value of the id to a timestamped index of values. This allows you to do historic queries, and keep track of changes in your documents.

## Straightforward syncronization

Connect your database nodes to trusted peers, and automatically push new data matching a query to the specified nodes.

## A nice way to write software

The idea is that the database will be used by several processes at the same time, some of them providing views of and interaction with the data, and others connecting to other nodes or external services. One example i'm co-developing is a program that listens on irc-channels. It reads its configuration from the database, and pushes new messages as well. 
The view then just has to read from and write to the database.

## Simple but powerful query language

Nudge queries are themselves edn maps, an example query:

    {:nudge/type :irc/watcher
     :channel    {:network  {:nudge/id    (? network-id)}
                 	:*servers {:nudge/type  :irc/server
                  					 :irc/network (? network-id)}}}

This example from the irc interface returns documents of type :irc/watcher, it also joins the :channel property of the watcher, and binds the variable (? network-id). This allows you to select all servers on this network.

The asterisk in :*servers is syntax for subqueries, keys that are not in the document gets added with the result of a subquery. In this case :*servers will end up containing all known servers on that network.

## License

Copyright © 2013 Kristoffer Ström

Distributed under the Eclipse Public License, the same as Clojure.
