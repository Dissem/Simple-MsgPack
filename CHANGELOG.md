Changelog
=========

2.0.0
-----
Migrated to Kotlin. If you didn't implement your own `Unpacker`, nothing should change except for the added null safety.
Otherwise, because `is` is a reserved word in Kotlin, the method was renamed to `doesUnpack` for ease of use.

1.0.0
-----
Initial version