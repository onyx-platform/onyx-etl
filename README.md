# onyx-etl

Onyx convenience application for moving data between storage mediums. **Work in progress.**

## Usage

<< Fill me in >>

### Datomic Specifics

#### Datomic Key File

A Datomic key file must be provided with `--datomic-key-file` when Datomic is used as an output target. This file must contain an EDN map. The keys of the map are keywords that represent Datomic `:db/id` attributes that exist in the target database schema. The values are keywords that repesent keys in the segments that were extracted from the input data source.

For example, if you had the following SQL table:

```text
mysql> describe people
    -> ;
+-------+-------------+------+-----+---------+----------------+
| Field | Type        | Null | Key | Default | Extra          |
+-------+-------------+------+-----+---------+----------------+
| id    | int(11)     | NO   | PRI | NULL    | auto_increment |
| name  | varchar(32) | YES  |     | NULL    |                |
| age   | int(4)      | YES  |     | NULL    |                |
+-------+-------------+------+-----+---------+----------------+
3 rows in set (0.01 sec)
```

And the following Datomic schema:

```clojure
[{:db/id #db/id [:db.part/db]
  :db/ident :com.mdrogalis/people
  :db.install/_partition :db.part/db}
 
 {:db/id #db/id [:db.part/db]
  :db/ident :user/name
  :db/valueType :db.type/string
  :db/cardinality :db.cardinality/one
  :db.install/_attribute :db.part/db}
  db/id #db/id [:db.part/db]
  :db/ident :user/age
  :db/valueType :db.type/long
  :db/cardinality :db.cardinality/one
  :db.install/_attribute :db.part/db}]
```

An example Datomic key file would contain:

```clojure
{:user/name :name
 :user/age :age}
```

## License

Copyright © 2015 Distributed Masonry LLC

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
