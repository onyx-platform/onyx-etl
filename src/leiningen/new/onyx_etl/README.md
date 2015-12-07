# {{app-name}}

Onyx convenience application for moving data between storage mediums. You can use this application to run prewritten Onyx jobs, or generate custom Onyx jobs from scratch.

## Usage

First, check the help menu to see all of the options:

```text
lein run {{app-name}}.launcher.local-runner --help
```

Then proceed to read the rest of the documentation below. When you find that the prewritten Onyx job isn't as expressive as you need, generate your Onyx job with `--job-file my_job.clj`. You can read more about code generation below, but it's worth calling out early in the README.

## Examples

### SQL to Datomic

In this example, we're going to lift data from a MySQL table into an existing Datomic database.

Let's assume our SQL table looks like this:

```text
mysql> select * from people;
+----+---------+------+
| id | name    | age  |
+----+---------+------+
|  1 | Mike    |   23 |
|  2 | Dorrene |   24 |
|  3 | Bridget |   32 |
|  4 | Joe     |   70 |
|  5 | Amanda  |   25 |
|  6 | Steven  |   30 |
+----+---------+------+
6 rows in set (0.00 sec)
```

And our Datomic schema looks like this:

```clojure
[{:db/id #db/id [:db.part/db]
  :db/ident :com.excellent/person
  :db.install/_partition :db.part/db}

 {:db/id #db/id [:db.part/db]
  :db/ident :user/name
  :db/valueType :db.type/string
  :db/cardinality :db.cardinality/one
  :db.install/_attribute :db.part/db}

 {:db/id #db/id [:db.part/db]
  :db/ident :user/age
  :db/valueType :db.type/long
  :db/cardinality :db.cardinality/one
  :db.install/_attribute :db.part/db}]
```

Then our command to move and transform the data might look like:

```text
$ lein run -m {{app-name}}.launcher.local-runner --from sql --to datomic --datomic-uri datomic:free://localhost:4334/my-db-name --datomic-partition com.excellent/person --datomic-key-file datomic-keys.edn --sql-classname com.mysql.jdbc.Driver --sql-subprotocol mysql --sql-subname //127.0.0.1:3306/onyx_example --sql-user sql-user --sql-password sql-password --sql-table people --sql-id-column id
```

Where the content of `datomic-keys.edn` is:

```clojure
{:user/name :name
 :user/age :age}
```

### Datomic Specifics

#### Datomic Key File

A Datomic key file must be provided with `--datomic-key-file` when Datomic is used as an output target. This file must contain an EDN map. The keys of the map are keywords that represent Datomic `:db/id` attributes that exist in the target database schema. The values are keywords that repesent keys in the segments that were extracted from the input data source.

For example, if you had the following SQL table:

```text
mysql> describe events
    -> ;
+------------+-------------+------+-----+---------+----------------+
| Field      | Type        | Null | Key | Default | Extra          |
+------------+-------------+------+-----+---------+----------------+
| id         | int(11)     | NO   | PRI | NULL    | auto_increment |
| occurence  | varchar(32) | YES  |     | NULL    |                |
| duration   | int(4)      | YES  |     | NULL    |                |
+------------+-------------+------+-----+---------+----------------+
3 rows in set (0.01 sec)
```

And the following Datomic schema:

```clojure
[{:db/id #db/id [:db.part/db]
  :db/ident :com.distributed-masonry/web-events
  :db.install/_partition :db.part/db}

 {:db/id #db/id [:db.part/db]
  :db/ident :web-event/event-time
  :db/valueType :db.type/inst
  :db/cardinality :db.cardinality/one
  :db.install/_attribute :db.part/db}

 {:db/id #db/id [:db.part/db]
  :db/ident :web-event/duration
  :db/valueType :db.type/long
  :db/cardinality :db.cardinality/one
  :db.install/_attribute :db.part/db}]
```

An example Datomic key file would contain:

```clojure
{:web-event/event-time :occurence
 :web-event/duration :duration}
```

### Code Generation

Eventually, using the canned Onyx jobs that we've written aren't going to be enough for your use case. You might want to do some extra type casting, data transformations, and so on. We've made it easy to transition from prewritten job to generated Onyx code. You can drop into a fully written Onyx program by adding `--job-file <my_job.clj>` to your command line switches. This will take all of the Onyx code that would have executed and constructs a fully valid namespace for you to use additional Onyx features. Using this switch would look like:

```text
$ lein run -m {{app-name}}.launcher.local-runner --from sql --to datomic <more options here> --job-file my_onyx_job.clj
```

## License

Copyright © 2015 Distributed Masonry LLC

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
