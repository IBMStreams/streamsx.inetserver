setVar 'TTPR_timeout' 240

# The common test suite for inet toolkit tests
import "$TTRO_scriptDir/streamsutils.sh"

#collect all samples as variant string for case Samples
all=''
short=''
cd "$TTRO_streamsxInetServerSamplesPath"
for x in $TTRO_streamsxInetServerSamplesPath/*; do
	if [[ -f $x/Makefile ]]; then
		short="${x#$TTRO_streamsxInetServerSamplesPath/}"
		all="$all $short"
	fi
done
printInfo "All samples are: $all"
setVar 'TTRO_streamsxInetServerSamples' "$all"
