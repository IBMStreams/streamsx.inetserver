#--variantList='tupleColoc tupleExloc jsonColoc jsonExloc'
#--exclusive=true

setCategory 'quick'

PREPS=(
	'myExplain'
	'copyAndMorphSpl'
	'splCompile'
)
STEPS=(
	'submitJob -P jettyPort=8080 -P tuplesExpected=2'
	'checkJobNo'
	'waitForJobHealth'
	'mySend'
	'TT_waitForFileName=data/WindowMarker'
	'waitForFinAndCheckHealth'
	'cancelJobAndLog'
	'myEval'
)

FINS='cancelJobAndLog'

myExplain() {
	case "$TTRO_variantCase" in
	tupleColoc) echo "variant $TTRO_variantCase - Tuple Tests with minimum attributes and pe colocation";;
	tupleExloc) echo "variant $TTRO_variantCase - Tuple Tests with minimum attributes and pe exlocation";;
	jsonColoc)  echo "variant $TTRO_variantCase - Json Tests with minimum attributes and pe colocation";;
	jsonExloc)  echo "variant $TTRO_variantCase - Json Tests with minimum attributes and pe exlocation";;
	*) printErrorAndExit "invalid variant $TTRO_variantCase";;
	esac
}

mySend() {
	executeLogAndSuccess curl -f http://localhost:8080/RequestStream/ports/analyze/0/login
	executeLogAndSuccess curl -f http://localhost:8080/RequestStream/ports/analyze/0/login
}

myEval() {
	if [[ "$TTRO_variantCase" == tuple* ]]; then
		linewisePatternMatchInterceptAndSuccess 'data/Tuples' 'true' '*key=1*' '*key=2*'
	else
		linewisePatternMatchInterceptAndSuccess 'data/Tuples' 'true' '*\\\"key\\\":1*' '*\\\"key\\\":2*'
	fi
}
