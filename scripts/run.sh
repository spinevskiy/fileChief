#!/bin/bash
#
cd `dirname $0`

. ./param.sh

CLASSPATH=lib/filechief-1.8.0.jar:lib/slf4j-api-1.6.2.jar:lib/slf4j-log4j12-1.6.2.jar:lib/commons-net-3.6.jar:lib/commons-io-2.5.jar:lib/log4j-1.2.17.jar:lib/jsch-0.1.54.jar:lib/mail-1.4.7.jar:lib/activation-1.1.jar:lib/jackson-core-2.9.0.jar:lib/jackson-databind-2.9.0.jar:lib/jackson-annotations-2.9.0.jar:lib/jakarta-oro-2.0.8.jar
export CLASSPATH
rval=0

# Command invoker.
#
invokeCmd() {
    pid=`/usr/bin/nohup ${@} > /dev/null 2>startStatus & echo $!`
    echo "waiting ..."
    for i in {1..10}
    do
	sleep 1
	started=`cat startStatus`
	if [ -n "$started" ];  then
	    break
	fi
    done
    started=`cat startStatus | grep Started`
    if [ -n "$started" ];  then
      echo $pid > proc.pid
      echo "PID=`cat proc.pid`"
    fi
    cat startStatus
}

start() {
    echo -n $"Starting : "
    invokeCmd ${JAVA_:?} -Xmx400m -Dwork -Dlog4j.configuration=file:log4j.properties -Dlog4j.watch=5000 -Dmail.smtp.timeout=30000 -Dmail.smtp.connectiontimeout=30000 psn.filechief.FileChief
}

test() {
    echo -n $"Starting in test mode: "
    ${JAVA_:?} -Dlog4j.configuration=file:log4j.test -Dlog4j.watch=5000 psn.filechief.FileChief test
}

stop() {
   ${JAVA_:?} -client -Dlog4j.configuration=file:log4j.stop -Dlog4j.watch=5000 psn.filechief.FileChief stop
}

wait_log() {
   echo "sleep 5 sec"
   sleep 5
   echo "---------- tail of the log , Ctrl-C to break-------------"
   tail -f logs/log
}

halt() {
   ${JAVA_:?} -client -Dlog4j.configuration=file:log4j.stop -Dlog4j.watch=5000 psn.filechief.FileChief halt
}


# See how we were called.
case "$1" in
    start)
        start
        ;;
    stop)
        stop
        ;;
    end)
        stop
        wait_log
        ;;
    halt)
        halt
        ;;
    test)
        test
        ;;
    *)
        echo $"Usage: $0 {start|stop|halt|end|test}"
	rval=1
        ;;
esac

exit $rval