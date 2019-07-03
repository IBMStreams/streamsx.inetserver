#--variantList="$TTRO_streamsxInetServerSamples"

setCategory 'quick'

function testStep {
	local save="$PWD"
	cd "$TTRO_streamsxInetServerSamplesPath/$TTRO_variantCase"
	pwd
	export SPL_CMD_ARGS=''
	export STREAMS_INETSERVER_TOOLKIT="$TTPR_streamsxInetServerToolkit"
	echoExecuteAndIntercept2 'success' 'make'
	cd "$save"
	return 0
}
