PREPS=(
	'copyOnly'
	'splCompile'
	'compileWsClient'
)
STEPS=(
	'submitJob -P jettyPort=8080 -P tuplesExpected=4'
	'checkJobNo'
	'myWait'
	'runWsClient'
	'TT_waitForFileName=data/WindowMarker'
	'waitForFinAndHealth'
	'cancelJobAndLog'
	'myEval'
)

FINS='cancelJobAndLog'

compileWsClient() {
	(
		cd "$TTRO_inputDir/../WebSocketClient"
		ant build
	)
}

runWsClient() {
	(
		cd "$TTRO_inputDir/../WebSocketClient"
		ant WsClient1 -DWebSocketClient.uri=ws://localhost:8080/ReceivedWsTuples/ports/output/0/wsinject
	)
}

myWait() {
	while ! jobHealthy; do
		printInfo "Wait for jobno=$TTTT_jobno to become healthy State=$TTTT_state Healthy=$TTTT_healthy"
		sleep "$TT_waitForFileInterval"
		now=$(date -u +%s)
		difftime=$((now-start))
		if [[ $difftime -gt $TTPR_waitForJobHealth ]]; then
			setFailure "Takes to long ( $difftime ) for the job to become healty"
			return 0
		fi
	done
}

myEval() {
	linewisePatternMatchInterceptAndSuccess data/Tuples true '*data="Hello1_123456789abcdefghijklHello2_123456789abcdefghijkl"*' '*data="Hello3_123456789abcdefghijklHello4_123456789abcdefghijkl"*'
	linewisePatternMatchInterceptAndSuccess data/Tuples true '*data="Hello1_second_123456789abcdefghijkl"*' '*data="Hello2_second_123456789abcdefghijkl"*'
}
