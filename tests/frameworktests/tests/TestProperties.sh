#samples path
setVar 'TTRO_streamsxInetServerSamplesPath' "$TTRO_inputDir/../../../samples"

#toolkit path
setVar 'TTPR_streamsxInetServerToolkit' "$TTRO_inputDir/../../../com.ibm.streamsx.inetserver"

#Some sample need json toolkit to compile
setVar 'TTPR_streamsxJsonToolkit' "$STREAMS_INSTALL/toolkits/com.ibm.streamsx.json"

setVar 'TT_toolkitPath' "${TTPR_streamsxInetServerToolkit}:${TTPR_streamsxJsonToolkit}" #consider more than one tk...
