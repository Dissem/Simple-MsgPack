Simple MessagePack
==================
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/ch.dissem.msgpack/msgpack/badge.svg)](https://maven-badges.herokuapp.com/maven-central/ch.dissem.msgpack/msgpack)
[![Javadoc](https://javadoc-emblem.rhcloud.com/doc/ch.dissem.msgpack/msgpack/badge.svg)](http://www.javadoc.io/doc/ch.dissem.msgpack/msgpack)
[![Apache 2](https://img.shields.io/badge/license-Apache_2.0-blue.svg)](https://raw.githubusercontent.com/Dissem/Jabit/master/LICENSE)

This is a simple Kotlin/Java library for handling MessagePack data. It doesn't do any object mapping, but maps to
special objects representing MessagePack types. To build, use command `./gradlew build`.

For most cases you might be better off using `org.msgpack:msgpack`, but I found that I needed something that generically
represents the internal structure of the data.

_Simple MessagePack_ uses Semantic Versioning, meaning as long as the major version doesn't change, nothing should break
if you update. Be aware though that this doesn't necessarily applies for SNAPSHOT builds and the development branch.


#### Master
[![Build Status](https://travis-ci.org/Dissem/Simple-MsgPack.svg?branch=master)](https://travis-ci.org/Dissem/Simple-MsgPack) 
[![Code Quality](https://img.shields.io/codacy/4cd1f577c18d45dc9042aac5bc20b424/master.svg)](https://www.codacy.com/app/chrigu-meyer/Simple-MsgPack/dashboard?bid=4122049)
[![Test Coverage](https://codecov.io/github/Dissem/Simple-MsgPack/coverage.svg?branch=master)](https://codecov.io/github/Dissem/Simple-MsgPack?branch=master)

#### Develop
[![Build Status](https://travis-ci.org/Dissem/Simple-MsgPack.svg?branch=develop)](https://travis-ci.org/Dissem/Simple-MsgPack?branch=develop) 
[![Code Quality](https://img.shields.io/codacy/4cd1f577c18d45dc9042aac5bc20b424/develop.svg)](https://www.codacy.com/app/chrigu-meyer/Simple-MsgPack/dashboard?bid=4118049)
[![Test Coverage](https://codecov.io/github/Dissem/Simple-MsgPack/coverage.svg?branch=develop)](https://codecov.io/github/Dissem/Simple-MsgPack?branch=develop)

Limitations
--------------

* There is no fallback to BigInteger for large integer type numbers, so there might be an integer overflow when reading
  too large numbers
* `MPFloat` uses the data type you're using to decide on precision (float 32 or 64) - not the actual value. E.g. 0.5
  could be saved perfectly as a float 42, but if you provide a double value, it will be stored as float 64, wasting
  4 bytes.
* If you want to use the 'ext format family', you'll need to implement and register your own `MPType` and
  `MPType.Unpacker`.
* Be aware that custom `MPType.Unpacker` take precedence over the default unpackers, i.e. if you accidentally define
  your unpacker to handle strings, for example, you won't be able to unpack any regular strings anymore.

Setup
-----

Add msgpack as Gradle dependency:
```Gradle
compile "ch.dissem.msgpack:msgpack:1.0.0"
```

Usage
-----

### Serialize Data

First, you'll need to create some msgpack objects to serialize:
```Java
MPMap<MPString, MPType<?>> object = new MPMap<>();
object.put(new MPString("compact"), new MPBoolean(true));
object.put(new MPString("schema"), new MPInteger(0));
```
or the shorthand version for simple types:
```Java
import static ch.dissem.msgpack.types.Utils.mp;

MPMap<MPString, MPType<?>> object = new MPMap<>();
object.put(mp("compact"), mp(true));
object.put(mp("schema"), mp(0));
```
then just use `pack(OutputStream)`:
```Java
OutputStream out = ...;
object.pack(out);
```


### Deserialize Data

For deserializing data there is the reader object:
```Java
Reader reader = Reader.getInstance()
```
just use `reader.read(InputStream)`. Unfortunately you'll need to make sure you got what you expected, the following
example might result in `ClassCastException` at weird places:
```Java
InputStream in = ...;
MPType read = reader.read(in);
MPMap<MPString, MPString> map = (MPMap<MPString, MPString>) read;
String value = map.get(mp("key")).getValue();
```
