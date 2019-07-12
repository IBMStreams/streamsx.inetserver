#--variantCount=2
#--exclusive=true

PREPS=(
	'myExplain'
	'copyAndMorphSpl'
	'splCompile'
	'compileWsClient'
)
STEPS=(
	'submitJob -P jettyPort=8080 -P tuplesExpected=4'
	'checkJobNo'
	'waitForJobHealth'
	'runWsClient'
	'TT_waitForFileName=data/WindowMarker'
	'waitForFinAndCheckHealth'
	'cancelJobAndLog'
	'myEval'
	'checkLogsNoError2'
)

FINS='cancelJobAndLog'

myExplain() {
	case "$TTRO_variantCase" in
	0) echo "variant $TTRO_variantCase - Output stream with message (rstring) and enderIdAttributeName";;
	1) echo "variant $TTRO_variantCase - Output stream with message (rstring) only and without control messages";;
	*) printErrorAndExit "invalid variant $TTRO_variantCase";;
	esac
}

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

myEval() {
	linewisePatternMatchInterceptAndSuccess data/Tuples true '*data="Hello1_123456789abcdefghijklHello2_123456789abcdefghijkl"*' '*data="Hello3_123456789abcdefghijklHello4_123456789abcdefghijkl"*'
	linewisePatternMatchInterceptAndSuccess data/Tuples true '*data="Hello1_second_123456789abcdefghijkl"*' '*data="Hello2_second_123456789abcdefghijkl"*'
}
