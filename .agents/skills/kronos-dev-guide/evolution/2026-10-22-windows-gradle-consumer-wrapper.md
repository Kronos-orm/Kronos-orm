# Use the platform Gradle wrapper in external consumers

In run [39648400804](https://github.com/Kronos-orm/Kronos-orm/actions/runs/39648400804), the Windows SQLite consumer failed before Gradle started. Its streamed check annotation reported `Windows Subsystem for Linux has no installed distributions`.

The workflow shell was Git Bash, but the native Java `ProcessBuilder` launched a bare `bash` command. Windows resolved its WSL launcher instead of the shell running the workflow. A Bash workflow step does not establish the executable selected by a Windows JVM child.

The external-consumer helper selects `COMSPEC /d /c gradlew.bat` for Windows requests to run `bash ./gradlew`, retaining all tasks, arguments, streaming diagnostics and exit status. Other hosts keep their requested command. Platform wrapper selection belongs at this process boundary; the repository modules continue to build with Kotlin Toolchain.

Hosted confirmation: commit `33272f356027f12d7bb9c78ff0766000b6d33d0f`, [Windows SQLite runtime](https://github.com/Kronos-orm/Kronos-orm/actions/runs/39652154760/job/119412321865), passed the actual SQLite test suite, the generated-ORM report guard and the executable before recording its consumer receipt. The local helper also preserved exit statuses zero and seven and their output. Native Toolchain PATH selection is covered separately in the Windows tar memory entry.
