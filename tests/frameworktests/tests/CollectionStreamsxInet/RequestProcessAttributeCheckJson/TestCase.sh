#--variantCount=4

function myExplain {
	case "$TTRO_variantCase" in
	0) echo "variant $TTRO_variantCase - Json Mode Input port has no default attribute jsonString";;
	1) echo "variant $TTRO_variantCase - Json Mode Output port has no default attribute jsonString";;
	2) echo "variant $TTRO_variantCase - Json Mode Input port has no attribute name from parameter jsonResponseAttributeName";;
	3) echo "variant $TTRO_variantCase - Json Mode Output port has no attribute name from parameter jsonAttributeName";;
	*) printErrorAndExit "invalid variant $TTRO_variantCase";;
	esac
}

PREPS=(
	'myExplain'
	'copyAndMorphSpl'
)

STEPS=(
	"splCompile port=8080"
	'executeLogAndError output/bin/standalone -t 2'
	'linewisePatternMatchInterceptAndSuccess "$TT_evaluationFile" "" "${errorCodes[$TTRO_variantCase]}"'
)

errorCodes=(
	'*Could not detect required attribute "key" on input port 0. Or specify a valid value for "keyAttributeName"*'
	"*Could not detect required attribute 'key'"' on output port 0. Or specify a valid value for "keyAttributeName"*'
	'*Could not detect required attribute "key" on input port 0. Or specify a valid value for "keyAttributeName"*'
	"*Could not detect required attribute 'key'"' on output port 0. Or specify a valid value for "keyAttributeName"*'
)
