This folder is copied to `{install}/drivers/xugu` by `org.jkiss.dbeaver.db.feature` (`root.folder.drivers`).

Do not add it to the xugu plugin `bin.includes`. On Windows, a JDBC JAR that exists only as a bundle entry is resolved via `FileLocator.toFileURL` + `Path.of(url.getFile())`, which throws `Illegal char <:>` on `/C:/...`.
