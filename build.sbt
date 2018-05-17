import com.typesafe.sbt.packager.docker._

enablePlugins(JavaServerAppPackaging)

lazy val akkaHttpVersion = "10.1.1"
lazy val akkaVersion    = "2.5.12"
lazy val elastic4sVersion = "6.2.8"

lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization    := "fr.hurrycane",
      scalaVersion    := "2.12.5"
    )),
    name := "workshop2",
    dockerEntrypoint ++= Seq(
      """-Dakka.actor.provider=cluster""",
      """-Dakka.remote.netty.tcp.hostname="$(eval "echo $AKKA_REMOTING_BIND_HOST")"""",
      """-Dakka.remote.netty.tcp.port="$AKKA_REMOTING_BIND_PORT"""",
      """$(IFS=','; I=0; for NODE in $AKKA_SEED_NODES; do echo "-Dakka.cluster.seed-nodes.$I=akka.tcp://$AKKA_ACTOR_SYSTEM_NAME@$NODE"; I=$(expr $I + 1); done)""",
      "-Dakka.io.dns.resolver=async-dns",
      "-Dakka.io.dns.async-dns.resolve-srv=true",
      "-Dakka.io.dns.async-dns.resolv-conf=on"
    ),
    dockerCommands :=
      dockerCommands.value.flatMap {
        case ExecCmd("ENTRYPOINT", args@_*) => Seq(Cmd("ENTRYPOINT", args.mkString(" ")))
        case v => Seq(v)
      },
    dockerRepository := Some("knox.hurrycane.fr"),
    dockerUpdateLatest := true,
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-http"            % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-http-xml"        % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-cluster" % akkaVersion,
      "com.typesafe.akka" %% "akka-stream"          % akkaVersion,
      "com.typesafe.play" %% "play-ahc-ws-standalone" % "2.0.0-M1",
      "com.newmotion" %% "akka-rabbitmq" % "5.0.0",
      "com.sksamuel.elastic4s" %% "elastic4s-spray-json" % "6.2.8",
      "com.sksamuel.elastic4s" %% "elastic4s-http-streams" % elastic4sVersion,
      "com.sksamuel.elastic4s" %% "elastic4s-http" % elastic4sVersion,
      "com.sksamuel.elastic4s" %% "elastic4s-core" % elastic4sVersion,
      "com.typesafe.akka" %% "akka-http-testkit"    % akkaHttpVersion % Test,
      "com.typesafe.akka" %% "akka-testkit"         % akkaVersion     % Test,
      "com.typesafe.akka" %% "akka-stream-testkit"  % akkaVersion     % Test,
      "org.scalatest"     %% "scalatest"            % "3.0.1"         % Test
    )
  )
