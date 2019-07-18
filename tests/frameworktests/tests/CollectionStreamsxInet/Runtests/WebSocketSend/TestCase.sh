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
	'submitJob -P jettyPort=8080'
	'checkJobNo'
	'waitForJobHealth'
	'runWsClient'
	'TT_waitForFileName=WsData_1.txt.end'
	'waitForFinAndCheckHealth'
	'cancelJobAndLog'
	'checkAllFilesExist . WsData_2.txt.end'
	'checkLineCount WsData_1.txt 102'
	'checkLineCount WsData_2.txt 102'
)

FINS='cancelJobAndLog'

myExplain() {
	case "$TTRO_variantCase" in
	0) echo "variant $TTRO_variantCase - Input stream with message (rstring) and int64";;
	1) echo "variant $TTRO_variantCase - Input stream with message (rstring) and int64 and param textMessage";;
	2) echo "variant $TTRO_variantCase - Input stream with message (rstring) and int64 and param textMessage and param enableConnectionControlMessages=false";;
	3) echo "variant $TTRO_variantCase - Input stream with data (blob) and int64 and param binaryMessageAttributeName";;
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
		ant WsClient2 '-DWebSocketClient.uri=ws://localhost:8080/WsSink/ports/input/0/wssend' "-DWebSocketClient.dir=$TTRO_workDirCase"
	)
}
