#--variantCount=4
#--exclusive=true

setCategory 'quick'

PREPS=(
	'myExplain'
	'copyAndMorphSpl'
	'splCompile'
	'compileWsClient'
)
STEPS=(
	'submitJob -P jettyPort=8080 -P tuplesExpected=30'
	'checkJobNo'
	'waitForJobHealth'
	'runWsClient'
	'TT_waitForFileName=data/WindowMarker'
	'waitForFinAndCheckHealth'
	'cancelJobAndLog'
	'myEval1'
	'checkLogsNoError2'
)

FINS='cancelJobAndLog'

myExplain() {
	case "$TTRO_variantCase" in
	0) echo "variant $TTRO_variantCase - Output stream with message (rstring) and senderIdAttributeName";;
	1) echo "variant $TTRO_variantCase - Output stream with message (rstring) only and without control messages";;
	2) echo "variant $TTRO_variantCase - Output stream with message (rstring) only and without control messages and without acknowledgements";;
	3) echo "variant $TTRO_variantCase - Output stream with message (blob) and senderIdAttributeName";;
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
		if [[ "$TTRO_variantCase" == 3 ]]; then
			ant WsClient1 -DWebSocketClient.uri=ws://localhost:8080/ReceivedWsTuples/ports/output/0/wsinject -DWebSocketClient.bin=bin
		else
			ant WsClient1 -DWebSocketClient.uri=ws://localhost:8080/ReceivedWsTuples/ports/output/0/wsinject
		fi
	)
}

myEval1() {
	local i
	for ((i=0; i<10; i++)); do
		linewisePatternMatchInterceptAndSuccess \
			data/Tuples true \
			'*data="'"${i}Hello1_123456789abcdefghijkl${i}Hello2_123456789abcdefghijkl"'"*' \
			'*data="'"${i}Hello3_123456789abcdefghijkl${i}Hello4_123456789abcdefghijkl"'"*' \
			'*data="'"${i}Hello1_second_123456789abcdefghijkl"'"*'
	done
}
